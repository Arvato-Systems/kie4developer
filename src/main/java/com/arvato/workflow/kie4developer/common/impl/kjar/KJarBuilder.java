package com.arvato.workflow.kie4developer.common.impl.kjar;

import com.arvato.workflow.kie4developer.common.impl.FileSystemUtils;
import com.arvato.workflow.kie4developer.common.impl.ProcessBuilder;
import com.arvato.workflow.kie4developer.common.interfaces.IDeployableBPMNProcess;
import com.arvato.workflow.kie4developer.common.interfaces.IDeployableDependency;
import com.arvato.workflow.kie4developer.common.interfaces.IDeployableWorkItemHandler;
import com.arvato.workflow.kie4developer.common.interfaces.IRelease;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.jbpm.ruleflow.core.RuleFlowProcessFactory;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.io.Resource;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Helper to create Kjar alias KModule for a release
 *
 * @author TRIBE01
 */
@Component
public class KJarBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(KJarBuilder.class);
  private IRelease release;
  private FileSystemUtils fileSystemUtils;

  public KJarBuilder(IRelease release, FileSystemUtils fileSystemUtils) {
    this.release = release;
    this.fileSystemUtils = fileSystemUtils;
  }

  /**
   * Build the Kjar (alias KModule) with all Processes and Workitemhandler
   *
   * @param deployableDependencies     related dependencies
   * @param globals                    the environment variables (globals) for the release
   * @param deployableProcesses        related processes
   * @param processesToMock            related processes to mock
   * @param deployableWorkitemhandlers related workitemhandlers
   * @param deployableServiceClasses   related service classes
   * @return the kjar file
   * @throws Exception if compilation fails or I/O error occurs
   */
  public Map<String, File> buildKjar(List<IDeployableDependency> deployableDependencies,
      List<Class<? extends IDeployableBPMNProcess>> deployableProcesses,
      List<Class<? extends IDeployableBPMNProcess>> processesToMock,
      List<Class<? extends IDeployableWorkItemHandler>> deployableWorkitemhandlers,
      List<Class> deployableServiceClasses, Properties globals) throws Exception {
    KieServices ks = KieServices.Factory.get();
    KieFileSystem kfs = ks.newKieFileSystem();

    Map<String, File> classFilesToDeploy = new HashMap<>(); // contains entries: [relative path in jar:file]

    // add classes-files
    addClassFileToDeployment(IDeployableWorkItemHandler.class, classFilesToDeploy);
    for (Class<? extends IDeployableWorkItemHandler> workitemhandlerClass : deployableWorkitemhandlers) {
      addClassFileToDeployment(workitemhandlerClass, classFilesToDeploy);
    }
    for (Class serviceClass : deployableServiceClasses) {
      addClassFileToDeployment(serviceClass, classFilesToDeploy);
    }
    for (IDeployableDependency dependency : deployableDependencies) {
      addClassFilesToDeployment(dependency, classFilesToDeploy);
    }

    // generate xml-files
    String deploymentDescriptor = buildDeploymentDescriptor(deployableWorkitemhandlers, globals,
        release.isDeploymentTargetBICCW(), release.isIncludeProcessInstanceListener(),
        release.isIncludeTaskEmailEventListener(), release.isIncludeTaskEventListener());
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
      if (!deployableProcess.isInterface() && !Modifier.isAbstract(deployableProcess.getModifiers())) {
        IDeployableBPMNProcess deployableBPMNProcess = deployableProcess.newInstance();
        resources.addAll(ProcessBuilder.build(deployableBPMNProcess)); // .bpmn + .svg
      }
    }
    for (Class<? extends IDeployableBPMNProcess> deployableProcessToMock : processesToMock) {
      if (!deployableProcessToMock.isInterface() && !Modifier.isAbstract(deployableProcessToMock.getModifiers())) {
        IDeployableBPMNProcess deployableBPMNProcess = mockProcess(deployableProcessToMock);
        resources.addAll(ProcessBuilder.build(deployableBPMNProcess)); // .bpmn + .svg
      }
    }
    for (Entry<String, File> deployableWorkitemhandlerFileSet : classFilesToDeploy.entrySet()) {
      try {
        String filepathForKJar = deployableWorkitemhandlerFileSet.getKey();
        Path classFileForKJar = extractFileWhenIncludedInJar(deployableWorkitemhandlerFileSet.getValue().toPath());
        resources.add(
            ResourceFactory.newByteArrayResource(Files.readAllBytes(classFileForKJar)).setSourcePath(filepathForKJar));
      } catch (IOException e) {
        LOGGER.error("Error on reading workitemhandler class file {}",
            deployableWorkitemhandlerFileSet.getValue().getAbsolutePath(), e);
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

    File tmpdir = fileSystemUtils.createTempDirectory(!LOGGER.isDebugEnabled()).toFile();
    long timestamp = Instant.now().getEpochSecond();

    // build the kjar file that represents the kmodule
    File jarFile;
    try {
      String filename = release.getArtifactId() + "-" + release.getVersion() + "-" + timestamp + ".jar";
      jarFile = new File(tmpdir, filename);
      OutputStream os = new FileOutputStream(jarFile);
      InternalKieModule kieModule = (InternalKieModule) builder.getKieModule();
      os.write(kieModule.getBytes());
      os.close();
    } catch (IOException e) {
      throw new IOException("Kjar write error (jar)", e);
    }

    // build the pom file that represents the kmodule
    File pomFile;
    try {
      String filename = release.getArtifactId() + "-" + release.getVersion() + "-" + timestamp + ".pom";
      pomFile = new File(tmpdir, filename);
      OutputStream os = new FileOutputStream(pomFile);
      os.write(pomXml.getBytes());
      os.close();
    } catch (IOException e) {
      throw new IOException("Kjar write error (pom)", e);
    }

    // remove the generated default pom & src folder - is there any way to not generated it first?
    Map<String, String> zip_properties = new HashMap<>();
    zip_properties.put("create", "false");
    try (FileSystem zipfs = FileSystems.newFileSystem(URI.create("jar:" + jarFile.toURI()), zip_properties)) {
      fileSystemUtils.delete(zipfs.getPath("META-INF/maven/org.default"));
      fileSystemUtils.delete(zipfs.getPath("src"));
    } catch (IOException e) {
      throw new IOException("Kjar write error", e);
    }

    LOGGER.info("Kjar created: {}", jarFile.getAbsolutePath());
    Map files = new HashMap<>();
    files.put("jar", jarFile);
    files.put("pom", pomFile);
    return files;
  }

  /**
   * Generates a process instance that use a mock implementation
   *
   * @param deployableProcessToMock the process to deploy as mock
   * @return the process instance that use a mock implementation
   * @throws Exception if compilation fails or I/O error occurs
   */
  private IDeployableBPMNProcess mockProcess(Class<? extends IDeployableBPMNProcess> deployableProcessToMock) throws Exception {
    IDeployableBPMNProcess realInstance = deployableProcessToMock.newInstance();
    return new IDeployableBPMNProcess() {

      @Override
      public String getName() {
        return realInstance.getName();
      }

      @Override
      public String getVersion() {
        return realInstance.getVersion();
      }

      @Override
      public void buildBPMNModel(RuleFlowProcessFactory factory) {
        factory
            // header
            .name(getName())
            .version(getVersion())
            .packageName(getPackage())
            // variables

            // nodes (the mock just executes start and end)
            .startNode(1).name("Start").done()

            .endNode(2).name("End").done()

            // connections
            .connection(1, 2);
      }
    };
  }

  /**
   * Add all class-files of the @link{IDeployableDependency} to the list of class files
   *
   * @param dependency         the dependency definition
   * @param classFilesToDeploy the list of class files to extend
   */
  private void addClassFilesToDeployment(IDeployableDependency dependency, Map<String, File> classFilesToDeploy)
      throws IOException {
    File unzippedJar = fileSystemUtils.getUnzippedMavenDependencyJarFile(dependency);

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
   * Get the class-file - extract when without jar
   *
   * @param classfilePath the path to the class-file (could be in a jar-file)
   * @return the (extracted) class-file
   */
  private Path extractFileWhenIncludedInJar(Path classfilePath) {
    while (classfilePath.toAbsolutePath().toString().contains(".jar")) {  // extract from jar if class is packed
      File jarFile = new File(classfilePath.toString().split(".jar")[0] + ".jar");
      File unzippedJarFile = fileSystemUtils.unzip(jarFile);
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
    compiledClassesDir =
        (compiledClassesDir.startsWith("/") && compiledClassesDir.contains(":")) ? compiledClassesDir.substring(1)
            : compiledClassesDir;
    compiledClassesDir = URLDecoder.decode(compiledClassesDir, "UTF-8");
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
   * @param deployableWorkitemhandlers     the related workitemhandlers
   * @param globals                        the environment variables (globals) for the release
   * @param includeBICCWListeners          flag to indicate if the BICCW listeners has to be included
   * @param includeProcessinstancelistener flag to indicate if the BICCW ImprovedBicceProcessInstanceListener has to be included
   * @param includeTaskemaileventlistener  flag to indicate if the BICCW BicceTaskEmailEventListener has to be included
   * @param includeTaskeventListener  flag to indicate if the BICCW ImprovedBicceTaskEventListener has to be included
   * @return the kie-deployment-descriptor.xml file content
   */
  private String buildDeploymentDescriptor(List<Class<? extends IDeployableWorkItemHandler>> deployableWorkitemhandlers,
      Properties globals, boolean includeBICCWListeners, boolean includeProcessinstancelistener,
      boolean includeTaskemaileventlistener, boolean includeTaskeventListener) {
    String deplomentDescriptorXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        + "<deployment-descriptor xsi:schemaLocation=\"http://www.jboss.org/jbpm deployment-descriptor.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
        + "    <persistence-unit>org.jbpm.domain</persistence-unit>\n"
        + "    <audit-persistence-unit>org.jbpm.domain</audit-persistence-unit>\n"
        + "    <audit-mode>JPA</audit-mode>\n"
        + "    <persistence-mode>JPA</persistence-mode>\n"
        + "    <runtime-strategy>PER_PROCESS_INSTANCE</runtime-strategy>\n"
        + "    <marshalling-strategies/>\n";
    if (includeBICCWListeners) {
      deplomentDescriptorXml += "    <event-listeners>\n";
      if (includeProcessinstancelistener) {
        deplomentDescriptorXml += "     <event-listener>\n"
            + "            <resolver>reflection</resolver>\n"
            + "            <identifier>com.arvato.bicce.listener.ImprovedBicceProcessInstanceListener</identifier>\n"
            + "            <parameters/>\n"
            + "     </event-listener>\n";
      }
      deplomentDescriptorXml += "    </event-listeners>\n"
          + "    <task-event-listeners>\n";
      if (includeTaskemaileventlistener) {
        deplomentDescriptorXml += "        <task-event-listener>\n"
            + "            <resolver>reflection</resolver>\n"
            + "            <identifier>com.arvato.bicce.email.listener.BicceTaskEmailEventListener</identifier>\n"
            + "            <parameters/>\n"
            + "        </task-event-listener>\n";
      }
      if (includeTaskeventListener) {
        deplomentDescriptorXml += "        <task-event-listener>\n"
            + "            <resolver>reflection</resolver>\n"
            + "            <identifier>com.arvato.bicce.listener.ImprovedBicceTaskEventListener</identifier>\n"
            + "            <parameters/>\n"
            + "        </task-event-listener>\n";
      }
      deplomentDescriptorXml += "    </task-event-listeners>\n";
    } else {
      deplomentDescriptorXml += "    <event-listeners/>\n"
          + "    <task-event-listeners/>\n";
    }
    deplomentDescriptorXml += "    <globals>\n";
    for (String globalName : globals.stringPropertyNames()) {
      deplomentDescriptorXml +="        <global>\n"
          + "            <resolver>mvel</resolver>\n"
          + "            <identifier>\""+globals.getProperty(globalName)+"\"</identifier>\n"
          + "            <parameters/>\n"
          + "            <name>"+globalName+"</name>\n"
          + "        </global>\n";
    }
    deplomentDescriptorXml += "    </globals>\n"
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
  private String buildPomXml() {
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
        + "      <version>7.23.0.Final</version>\n"
        + "      <scope>provided</scope>\n"
        + "    </dependency>\n"
        + "    <dependency>\n"
        + "      <groupId>org.kie</groupId>\n"
        + "      <artifactId>kie-api</artifactId>\n"
        + "      <version>7.23.0.Final</version>\n"
        + "      <scope>provided</scope>\n"
        + "    </dependency>\n";
    pomXml += "  </dependencies>\n"
        + "  <build>\n"
        + "    <plugins>\n"
        + "      <plugin>\n"
        + "        <groupId>org.kie</groupId>\n"
        + "        <artifactId>kie-maven-plugin</artifactId>\n"
        + "        <version>7.23.0.Final</version>\n"
        + "        <extensions>true</extensions>\n"
        + "      </plugin>\n"
        + "    </plugins>\n"
        + "  </build>\n"
        + "</project>";
    return pomXml;
  }
}
