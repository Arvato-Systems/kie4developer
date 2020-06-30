package com.arvato.workflow.kie4developer.common.impl.kjar;

import com.arvato.workflow.kie4developer.common.interfaces.IDeployableBPMNProcess;
import com.arvato.workflow.kie4developer.common.interfaces.IDeployableDependency;
import com.arvato.workflow.kie4developer.common.interfaces.IDeployableService;
import com.arvato.workflow.kie4developer.common.interfaces.IDeployableWorkItemHandler;
import com.arvato.workflow.kie4developer.common.interfaces.IRelease;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.io.Resource;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Helper to create Kjar alias KModule for a release
 *
 * @author TRIBE01
 * @see {https://github.com/kiegroup/jbpm/blob/7.15.0.Final/jbpm-runtime-manager/src/test/java/org/jbpm/runtime/manager/impl/deploy/AbstractDeploymentDescriptorTest.java}
 */
@Component
public class KJarBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(KJarBuilder.class);
  @Value("${maven.repository}")
  private String mavenRepoPath;
  private IRelease release;

  public KJarBuilder(IRelease release) {
    this.release = release;
  }

  /**
   * Build the Kjar (alias KModule) with all Processes and Workitemhandler
   *
   * @param deployableDependencies     related dependencies
   * @param deployableProcesses        related processes
   * @param deployableWorkitemhandlers related workitemhandlers
   * @param deployableServiceclasses   related service classes
   * @return the kjar file
   * @throws Exception if compilation fails or I/O error occurs
   */
  public File buildKjar(List<Class<? extends IDeployableDependency>> deployableDependencies,
      List<Class<? extends IDeployableBPMNProcess>> deployableProcesses,
      List<Class<? extends IDeployableWorkItemHandler>> deployableWorkitemhandlers,
      List<Class<? extends IDeployableService>> deployableServiceclasses) throws Exception {
    KieServices ks = KieServices.Factory.get();
    KieFileSystem kfs = ks.newKieFileSystem();

    Map<String, File> classFilesToDeploy = new HashMap<>(); // contains entries: [relative path in jar:file]

    // add classes-files
    addClassFileToDeployment(IDeployableWorkItemHandler.class, classFilesToDeploy);
    addClassFileToDeployment(IDeployableService.class, classFilesToDeploy);
    for (Class<? extends IDeployableWorkItemHandler> workitemhandlerClass : deployableWorkitemhandlers) {
      addClassFileToDeployment(workitemhandlerClass, classFilesToDeploy);
    }
    for (Class<? extends IDeployableService> serviceClass : deployableServiceclasses) {
      addClassFileToDeployment(serviceClass, classFilesToDeploy);
    }
    for (Class<? extends IDeployableDependency> dependencyClass : deployableDependencies) {
      addClassFilesToDeployment(dependencyClass, classFilesToDeploy);
    }

    // generate xml-files
    String deploymentDescriptor = buildDeploymentDescriptor(deployableWorkitemhandlers,
        release.isDeploymentTargetBICCW());
    String kmoduleInfo = buildKmoduleInfo();
    String kmoduleXml = buildKmoduleXml();
    String beansXml = buildBeansXml(deployableWorkitemhandlers);
    String persistenceXml = buildPersistence();
    String pomXml = buildPomXml();
    String pomProperties = buildPomProperties();

    // convert to Resources
    List<Resource> resources = new ArrayList<>();
    resources.add(ResourceFactory.newByteArrayResource(deploymentDescriptor.getBytes())
        .setSourcePath("META-INF/kie-deployment-descriptor.xml"));
    resources.add(ResourceFactory.newByteArrayResource(kmoduleInfo.getBytes()).setSourcePath("META-INF/kmodule.info"));
    resources.add(ResourceFactory.newByteArrayResource(kmoduleXml.getBytes()).setSourcePath("META-INF/kmodule.xml"));
    resources
        .add(ResourceFactory.newByteArrayResource(persistenceXml.getBytes()).setSourcePath("META-INF/persistence.xml"));
    resources.add(ResourceFactory.newByteArrayResource(beansXml.getBytes()).setSourcePath("META-INF/beans.xml"));
    resources.add(ResourceFactory.newByteArrayResource(pomXml.getBytes()).setSourcePath(
        "META-INF/maven/" + release.getGroupId() + File.separator + release.getArtifactId() + "/pom.xml"));
    resources.add(ResourceFactory.newByteArrayResource(pomProperties.getBytes()).setSourcePath(
        "META-INF/maven/" + release.getGroupId() + File.separator + release.getArtifactId() + "/pom.properties"));
    for (Class<? extends IDeployableBPMNProcess> deployableProcess : deployableProcesses) {
      resources.add(deployableProcess.newInstance().getBPMNModel()); // .bpmn
    }
    for (Entry<String, File> deployableWorkitemhandlerFileSet : classFilesToDeploy.entrySet()) {
      try {
        String filepathForKJar = deployableWorkitemhandlerFileSet.getKey();
        Path classFileForKJar = extractFileWhenIncludedInJar(deployableWorkitemhandlerFileSet.getValue().toPath());
        resources.add(
            ResourceFactory.newByteArrayResource(Files.readAllBytes(classFileForKJar)).setSourcePath(filepathForKJar));
      } catch (IOException e) {
        LOGGER.error(String.format("Error on reading workitemhandler class file %s",
            deployableWorkitemhandlerFileSet.getValue().getAbsolutePath()), e);
      }
    }

    // write to kmodule
    for (Resource resource : resources) {
      kfs.write(resource);
    }

    // build the kmodule
    KieBuilder builder = ks.newKieBuilder(kfs).buildAll();

    // validate the Kmodule
    if (builder.getResults().hasMessages(Message.Level.ERROR)) {
      throw new Exception(
          String.format("Process compilation error: %s", builder.getResults().getMessages().toString()));
    }

    // build the kjar file that represents the kmodule
    File jarFile;
    try {
      jarFile = File.createTempFile(release.getArtifactId() + "-" + release.getVersion() + "-", ".jar");
      OutputStream os = new FileOutputStream(jarFile);
      InternalKieModule kieModule = (InternalKieModule) builder.getKieModule();
      os.write(kieModule.getBytes());
      os.close();
    } catch (IOException e) {
      throw new IOException("Kjar write error", e);
    }

    // remove the generated default pom & src folder - is there any way to not generated it first?
    Map<String, String> zip_properties = new HashMap<>();
    zip_properties.put("create", "false");
    try (FileSystem zipfs = FileSystems.newFileSystem(URI.create("jar:" + jarFile.toURI()), zip_properties)) {
      delete(zipfs.getPath("META-INF/maven/org.default"));
      delete(zipfs.getPath("src"));
    } catch (IOException e) {
      throw new IOException("Kjar write error", e);
    }

    LOGGER.info("Kjar created: " + jarFile.getAbsolutePath());
    return jarFile;
  }

  /**
   * Unzip a zip/jar to a tmp directory
   *
   * @param zipFile the jar or zip file to extract
   * @return the temp directory which contains all extracted folders and files of the archive
   */
  private File unzip(File zipFile) throws IOException {
    Path outputPath = Files.createTempDirectory(UUID.randomUUID().toString());
    try (ZipFile zf = new ZipFile(zipFile)) {
      Enumeration<? extends ZipEntry> zipEntries = zf.entries();
      while (zipEntries.hasMoreElements()) {
        ZipEntry entry = zipEntries.nextElement();
        if (entry.isDirectory()) {
          Path dirToCreate = outputPath.resolve(entry.getName());
          Files.createDirectories(dirToCreate);
        } else {
          Path fileToCreate = outputPath.resolve(entry.getName());
          fileToCreate.toFile().getParentFile().mkdirs();
          Files.copy(zf.getInputStream(entry), fileToCreate);
        }
      }
    } catch (IOException e) {
      throw e;
    }
    return outputPath.toFile();
  }

  /**
   * Delete all files within and the directory itself
   *
   * @param path filesystem path to delete
   * @throws IOException on any I/O error
   */
  private void delete(Path path) throws IOException {
    if (path == null || !Files.exists(path)) {
      return;
    }
    if (Files.isDirectory(path)) {
      Stream<Path> children = Files.list(path);
      children.forEach(child -> {
        try {
          delete(child);
        } catch (IOException e) {
          LOGGER.error("Error while deleting file {}", child, e);
        }
      });
    }
    Files.delete(path);
  }

  /**
   * Add all class-files of the @link{IDeployableDependency} to the list of class files
   *
   * @param dependencyClass    the dependency definition
   * @param classFilesToDeploy the list of class files to extend
   */
  private void addClassFilesToDeployment(Class<? extends IDeployableDependency> dependencyClass,
      Map<String, File> classFilesToDeploy)
      throws IllegalAccessException, InstantiationException, IOException {
    IDeployableDependency instance = dependencyClass.newInstance();
    String groupId = instance.getMavenGroupId();
    String artifactId = instance.getMavenArtifactId();
    String versionId = instance.getMavenVersionId();

    File jarFile = new File(mavenRepoPath + File.separator +
        groupId.replace(".", File.separator) + File.separator
        + artifactId + File.separator + versionId + File.separator + artifactId + "-" + versionId + ".jar");
    File unzippedJar = unzip(jarFile);

    List<Path> allClassFilesFromJar = Files.walk(unzippedJar.toPath())
        .filter(path -> path.toFile().isFile())
        .filter(path -> path.toString().endsWith(".class"))
        .collect(Collectors.toList());

    for (Path classFile : allClassFilesFromJar) {
      addClassFileToDeployment(classFile, classFilesToDeploy);
    }
  }

  /**
   * Add the class-file to the list of class files
   *
   * @param classfilePath      the path to the class-file
   * @param classFilesToDeploy the list of class files to extend
   */
  private void addClassFileToDeployment(Path classfilePath, Map<String, File> classFilesToDeploy) {
    Path filepath = Paths.get(System.getProperty("java.io.tmpdir"))
        .relativize(classfilePath); // remove the tmp dir path
    filepath = filepath.subpath(1, filepath.getNameCount()); // remove the generated tmp-folder name
    classFilesToDeploy.put(filepath.toString(), classfilePath.toFile());
  }

  /**
   * Get the relativized filepath for a class-file
   *
   * @param clazz         the class
   * @param classfilePath the fullpath which should be relativized
   * @return the relativized filepath of the class-file
   */
  private Path extractRelativePathForClass(Class clazz, Path classfilePath) {
    int dotsInPackageName = (int) clazz.getPackage().getName().chars().filter(ch -> ch == '.').count();
    return classfilePath.subpath(classfilePath.getNameCount() - dotsInPackageName - 2, classfilePath.getNameCount());
  }

  /**
   * Get the class-file - extract when without jar
   *
   * @param classfilePath the path to the class-file (could be in a jar-file)
   * @return the (extracted) class-file
   * @throws IOException on any I/O Exception
   */
  private Path extractFileWhenIncludedInJar(Path classfilePath) throws IOException {
    while (classfilePath.toAbsolutePath().toString().contains(".jar")) {  // extract from jar if class is packed
      File jarFile = new File(classfilePath.toString().split(".jar")[0] + ".jar");
      File unzippedJarFile = unzip(jarFile);
      String seperator = classfilePath.toString().contains(".jar!") ? ".jar!" : ".jar";
      String afterJar = classfilePath.toString()
          .substring(classfilePath.toString().indexOf(seperator) + seperator.length());
      afterJar = afterJar.replaceAll("classes!", "classes");
      classfilePath = Paths.get(unzippedJarFile.toString() + File.separator + afterJar);
    }
    return classfilePath;
  }

  /**
   * Add the class-file to the list of class files
   *
   * @param clazz              the class
   * @param classFilesToDeploy the list of class files to extend
   */
  private void addClassFileToDeployment(Class clazz, Map<String, File> classFilesToDeploy) throws IOException {
    String compiledClassesDir = clazz.getProtectionDomain().getCodeSource().getLocation().getFile();
    compiledClassesDir = compiledClassesDir.startsWith("file:") ? compiledClassesDir.substring(5) : compiledClassesDir;
    compiledClassesDir = (compiledClassesDir.startsWith("/") && compiledClassesDir.contains(":")) ? compiledClassesDir.substring(1) : compiledClassesDir;
    Path directoryStreamPath = Paths
        .get(compiledClassesDir + clazz.getPackage().getName().replace(".", File.separator));
    directoryStreamPath = extractFileWhenIncludedInJar(directoryStreamPath);

    String pattern = "glob:**/" + clazz.getSimpleName() + "*.class";
    PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(pattern);
    try (DirectoryStream<Path> matches = Files.newDirectoryStream(directoryStreamPath, pathMatcher::matches)) {
      for (Path classFile : matches) {
        Path relativeFilepathOfClass = extractRelativePathForClass(clazz, classFile);
        classFilesToDeploy.put(relativeFilepathOfClass.toString(), classFile.toFile());
      }
    }
  }

  /**
   * Build the beans.xml for the provided release
   *
   * @param deployableWorkitemhandlers the workitemhandler
   * @return the beans.xml file content
   */
  private String buildBeansXml(List<Class<? extends IDeployableWorkItemHandler>> deployableWorkitemhandlers) {
    String beansXml =
        "<beans xmlns=\"http://java.sun.com/xml/ns/javaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://docs.jboss.org/cdi/beans_1_0.xsd\">\n"
            + "  <alternatives>\n";
    for (Class<? extends IDeployableWorkItemHandler> workitemhandler : deployableWorkitemhandlers) {
      beansXml +=
          "    <class>" + workitemhandler.getPackage().getName() + "." + workitemhandler.getName() + "</class>\n";
    }
    beansXml += "  </alternatives>\n"
        + "</beans>";
    return beansXml;
  }

  /**
   * Build the persistence.xml for the provided release
   *
   * @return the persistence.xml file content
   */
  private String buildPersistence() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        + "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" xmlns:orm=\"http://java.sun.com/xml/ns/persistence/orm\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"2.0\" xsi:schemaLocation=\"http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd http://java.sun.com/xml/ns/persistence/orm http://java.sun.com/xml/ns/persistence/orm_2_0.xsd\">\n"
        + "    <persistence-unit name=\"" + release.getDeploymentId() + "\" transaction-type=\"JTA\">\n"
        + "        <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>\n"
        + "        <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>\n"
        + "        <exclude-unlisted-classes>true</exclude-unlisted-classes>\n"
        + "        <properties>\n"
        + "            <property name=\"hibernate.dialect\" value=\"org.hibernate.dialect.H2Dialect\"/>\n"
        + "            <property name=\"hibernate.max_fetch_depth\" value=\"3\"/>\n"
        + "            <property name=\"hibernate.hbm2ddl.auto\" value=\"update\"/>\n"
        + "            <property name=\"hibernate.show_sql\" value=\"false\"/>\n"
        + "            <property name=\"hibernate.id.new_generator_mappings\" value=\"false\"/>\n"
        + "            <property name=\"hibernate.transaction.jta.platform\" value=\"org.hibernate.service.jta.platform.internal.JBossAppServerJtaPlatform\"/>\n"
        + "        </properties>\n"
        + "    </persistence-unit>\n"
        + "</persistence>\n";
  }

  /**
   * Build the kmodule.xml for the provided release
   *
   * @return the kmodule.xml file content
   */
  private String buildKmoduleXml() {
    return "<kmodule xmlns=\"http://www.drools.org/xsd/kmodule\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>";
  }

  /**
   * Build the kmodule.info for the provided release
   *
   * @return the kmodule.info file content
   */
  private String buildKmoduleInfo() {
    return "<org.drools.core.rule.KieModuleMetaInfo>\n"
        + "  <typeMetaInfos/>\n"
        + "  <rulesByPackage/>\n"
        + "</org.drools.core.rule.KieModuleMetaInfo>";
  }

  /**
   * Build the kie-deployment-descriptor.xml for the provided release
   *
   * @param deployableWorkitemhandlers the related workitemhandlers
   * @param includeBICCWListeners      flag to indicate if the BICCW listeners has to be included
   * @return the kie-deployment-descriptor.xml file content
   */
  private String buildDeploymentDescriptor(List<Class<? extends IDeployableWorkItemHandler>> deployableWorkitemhandlers,
      boolean includeBICCWListeners) throws IllegalAccessException, InstantiationException {
    String deplomentDescriptorXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        + "<deployment-descriptor xsi:schemaLocation=\"http://www.jboss.org/jbpm deployment-descriptor.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
        + "    <persistence-unit>org.jbpm.domain</persistence-unit>\n"
        + "    <audit-persistence-unit>org.jbpm.domain</audit-persistence-unit>\n"
        + "    <audit-mode>JPA</audit-mode>\n"
        + "    <persistence-mode>JPA</persistence-mode>\n"
        + "    <runtime-strategy>PER_PROCESS_INSTANCE</runtime-strategy>\n"
        + "    <marshalling-strategies/>\n";
    if (includeBICCWListeners) {
      deplomentDescriptorXml += "    <event-listeners>\n"
          + "     <event-listener>\n"
          + "            <resolver>reflection</resolver>\n"
          + "            <identifier>com.arvato.bicce.listener.ImprovedBicceProcessInstanceListener</identifier>\n"
          + "            <parameters/>\n"
          + "     </event-listener>\n"
          + "    </event-listeners>\n"
          + "    <task-event-listeners>\n"
          + "        <task-event-listener>\n"
          + "            <resolver>reflection</resolver>\n"
          + "            <identifier>com.arvato.bicce.email.listener.BicceTaskEmailEventListener</identifier>\n"
          + "            <parameters/>\n"
          + "        </task-event-listener>\n"
          + "        <task-event-listener>\n"
          + "            <resolver>reflection</resolver>\n"
          + "            <identifier>com.arvato.bicce.listener.ImprovedBicceTaskEventListener</identifier>\n"
          + "            <parameters/>\n"
          + "        </task-event-listener>"
          + "    </task-event-listeners>\n";
    } else {
      deplomentDescriptorXml += "    <event-listeners/>\n"
          + "    <task-event-listeners/>\n";
    }
    deplomentDescriptorXml += "    <globals/>\n"
        + "    <work-item-handlers>\n";
    for (Class<? extends IDeployableWorkItemHandler> workitemhandler : deployableWorkitemhandlers) {
      deplomentDescriptorXml += "        <work-item-handler>\n"
          + "            <resolver>reflection</resolver>\n"
          + "            <identifier>" + workitemhandler.getName() + "</identifier>\n"
          + "            <parameters></parameters>\n"
          + "            <name>" + workitemhandler.getSimpleName() + "</name>\n"
          + "        </work-item-handler>";
    }
    deplomentDescriptorXml += "    </work-item-handlers>\n"
        + "    <environment-entries/>\n"
        + "    <configurations/>\n"
        + "    <required-roles/>\n"
        + "    <remoteable-classes/>\n"
        + "    <limit-serialization-classes>true</limit-serialization-classes>\n"
        + "</deployment-descriptor>\n";
    return deplomentDescriptorXml;
  }

  /**
   * Build the pom.properties for the provided release
   *
   * @return the pom.properties file content
   */
  private String buildPomProperties() {
    return "groupId=" + release.getGroupId() + "\n"
        + "artifactId=" + release.getArtifactId() + "\n"
        + "version=" + release.getVersion() + "\n";
  }

  /**
   * Build the pom.xml for the provided release
   *
   * @return the pom.xml file content
   */
  private String buildPomXml() throws Exception {
    String pomXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<project xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
        + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
        + "  <modelVersion>4.0.0</modelVersion>\n"
        + "  <groupId>" + release.getGroupId() + "</groupId>\n"
        + "  <artifactId>" + release.getArtifactId() + "</artifactId>\n"
        + "  <version>" + release.getVersion() + "</version>\n"
        + "  <packaging>kjar</packaging>\n"
        + "  <name>" + release.getProjectName() + "</name>\n"
        + "  <description>" + release.getProjectDescription() + "</description>\n"
        + "  <dependencies>\n"
        + "    <dependency>\n"
        + "      <groupId>org.kie</groupId>\n"
        + "      <artifactId>kie-internal</artifactId>\n"
        + "      <version>7.15.0.Final</version>\n"
        + "      <scope>provided</scope>\n"
        + "    </dependency>\n"
        + "    <dependency>\n"
        + "      <groupId>org.kie</groupId>\n"
        + "      <artifactId>kie-api</artifactId>\n"
        + "      <version>7.15.0.Final</version>\n"
        + "      <scope>provided</scope>\n"
        + "    </dependency>\n";
    pomXml += "  </dependencies>\n"
        + "  <build>\n"
        + "    <plugins>\n"
        + "      <plugin>\n"
        + "        <groupId>org.kie</groupId>\n"
        + "        <artifactId>kie-maven-plugin</artifactId>\n"
        + "        <version>7.15.0.Final</version>\n"
        + "        <extensions>true</extensions>\n"
        + "      </plugin>\n"
        + "    </plugins>\n"
        + "  </build>\n"
        + "</project>";
    return pomXml;
  }
}
