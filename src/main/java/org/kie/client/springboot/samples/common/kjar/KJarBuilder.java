package org.kie.client.springboot.samples.common.kjar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.io.Resource;
import org.kie.client.springboot.samples.common.interfaces.IDeployableBPMNProcess;
import org.kie.client.springboot.samples.common.interfaces.IDeployableWorkItemHandler;
import org.kie.client.springboot.samples.common.interfaces.IRelease;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Helper to create Kjar alias KModule for a release
 * @see {https://github.com/kiegroup/jbpm/blob/7.6.x/jbpm-runtime-manager/src/test/java/org/jbpm/runtime/manager/impl/deploy/AbstractDeploymentDescriptorTest.java}
 * @author TRIBE01
 */
@Component
public class KJarBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(KJarBuilder.class);
  @Autowired
  private IRelease release;

  /**
   * Build the Kjar with
   * @param deployableProcesses the related processes
   * @param deployableWorkitemhandlers the related workitemhandlers
   * @return the kjar file
   * @throws IOException on any I/O Exceptions
   * @throws RuntimeException if compilation fails
   */
  public File buildKjar(List<IDeployableBPMNProcess> deployableProcesses, List<IDeployableWorkItemHandler> deployableWorkitemhandlers) {
    // create the Kmodule alias Kjar
    KieServices ks = KieServices.Factory.get();
    KieFileSystem kfs = ks.newKieFileSystem();

    // generate content
    String deploymentDescriptor = buildDeploymentDescriptor(deployableWorkitemhandlers); // META-INF/kie-deployment-descriptor.xml
    String kmoduleInfo = buildKmoduleInfo(); // META-INF/kmodule.info
    String kmoduleXml = buildKmoduleXml(); // META-INF/kmodule.xml
    String persistenceXml = buildPersistence(); // META-INF/persistence.xml
    String pomXml = buildPomXml(deployableWorkitemhandlers); // META-INF/maven/<project group id>/<project id>/pom.xml
    String pomProperties = buildPomProperties();  // META-INF/maven/<project group id>/<project id>/pom.properties

    // convert to Resources
    Resource deploymentDescriptorResource = ResourceFactory.newByteArrayResource(deploymentDescriptor.getBytes()).setSourcePath("META-INF/kie-deployment-descriptor.xml");
    Resource kmoduleInfoResource = ResourceFactory.newByteArrayResource(kmoduleInfo.getBytes()).setSourcePath("META-INF/kmodule.info");
    Resource kmoduleXmlResource = ResourceFactory.newByteArrayResource(kmoduleXml.getBytes()).setSourcePath("META-INF/kmodule.xml");
    Resource persistenceXmlResource = ResourceFactory.newByteArrayResource(persistenceXml.getBytes()).setSourcePath("META-INF/persistence.xml");
    Resource pomXmlResource = ResourceFactory.newByteArrayResource(pomXml.getBytes()).setSourcePath("META-INF/maven/"+release.getGroupId()+"/"+release.getArtifactId()+"/pom.xml");
    Resource pomPropertiesResource = ResourceFactory.newByteArrayResource(pomProperties.getBytes()).setSourcePath("META-INF/maven/"+release.getGroupId()+"/"+release.getArtifactId()+"/pom.properties");
    List<Resource> bpmnResources = new ArrayList<>();
    for (IDeployableBPMNProcess deployableProcess : deployableProcesses){
      bpmnResources.add(deployableProcess.getBPMNModel()); // .bpmn
    }

    // write to kmodule
    kfs.write(deploymentDescriptorResource);
    kfs.write(kmoduleInfoResource);
    kfs.write(kmoduleXmlResource);
    kfs.write(persistenceXmlResource);
    kfs.write(pomXmlResource);
    kfs.write(pomPropertiesResource);
    for (Resource bpmnResource : bpmnResources){
      kfs.write(bpmnResource);
    }

    // build the kmodule
    KieBuilder builder = ks.newKieBuilder(kfs).buildAll();

    // remove the generated default pom
    kfs.delete("META-INF/maven/org.default");

    // validate the Kmodule
    if (builder.getResults().hasMessages(Message.Level.ERROR)) {
      LOGGER.error("Process compilation error: " + builder.getResults().getMessages().toString());
      throw new RuntimeException("Process compilation error: " + builder.getResults().getMessages().toString());
    }

    // build the kjar file that represents the kmodule
    File jarFile;
    try {
      jarFile = File.createTempFile(release.getArtifactId() + "-" + release.getVersion(), ".jar");
      OutputStream os = new FileOutputStream(jarFile);
      InternalKieModule kieModule = (InternalKieModule) builder.getKieModule();
      os.write(kieModule.getBytes());
      os.close();
    } catch (IOException e) {
      throw new RuntimeException("Kjar write error", e);
    }
    return jarFile;
  }

  /**
   * Build the persistence.xml for the provided release
   * @return the persistence.xml file content
   */
  private String buildPersistence() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        + "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" xmlns:orm=\"http://java.sun.com/xml/ns/persistence/orm\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"2.0\" xsi:schemaLocation=\"http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd http://java.sun.com/xml/ns/persistence/orm http://java.sun.com/xml/ns/persistence/orm_2_0.xsd\">\n"
        + "    <persistence-unit name=\""+release.getDeploymentId()+"\" transaction-type=\"JTA\">\n"
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
   * @return the kmodule.xml file content
   */
  private String buildKmoduleXml() {
    return "<kmodule xmlns=\"http://www.drools.org/xsd/kmodule\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>";
  }

  /**
   * Build the kmodule.info for the provided release
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
   * @param deployableWorkitemhandlers the related workitemhandlers
   * @return the kie-deployment-descriptor.xml file content
   */
  private String buildDeploymentDescriptor(List<IDeployableWorkItemHandler> deployableWorkitemhandlers){
     String deplomentDescriptorXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        + "<deployment-descriptor xsi:schemaLocation=\"http://www.jboss.org/jbpm deployment-descriptor.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
        + "    <persistence-unit>org.jbpm.domain</persistence-unit>\n"
        + "    <audit-persistence-unit>org.jbpm.domain</audit-persistence-unit>\n"
        + "    <audit-mode>JPA</audit-mode>\n"
        + "    <persistence-mode>JPA</persistence-mode>\n"
        + "    <runtime-strategy>PER_PROCESS_INSTANCE</runtime-strategy>\n"
        + "    <marshalling-strategies/>\n"
        + "    <event-listeners/>\n"
        + "    <task-event-listeners/>\n"
        + "    <globals/>\n"
        + "    <work-item-handlers>\n";
    for (IDeployableWorkItemHandler workitemhandler : deployableWorkitemhandlers) {
      deplomentDescriptorXml += "        <work-item-handler>\n"
          + "            <resolver>mvel</resolver>\n"
          + "            <identifier>new " + workitemhandler.getClass().getName() + "()</identifier>\n"
          + "            <parameters/>\n"
          + "            <name>" + workitemhandler.getName() + "</name>\n"
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
   * @return the pom.properties file content
   */
  private String buildPomProperties() {
    return "groupId="+release.getGroupId()+"\n"
        + "artifactId="+release.getArtifactId()+"\n"
        + "version="+release.getVersion()+"\n";
  }

  /**
   * Build the pom.xml for the provided release
   * @param deployableWorkitemhandlers the related workitemhandlers
   * @return the pom.xml file content
   */
  private String buildPomXml(List<IDeployableWorkItemHandler> deployableWorkitemhandlers) {
    String pomXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<project xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
        + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
        + "  <modelVersion>4.0.0</modelVersion>\n"
        + "  <groupId>"+release.getGroupId()+"</groupId>\n"
        + "  <artifactId>"+release.getArtifactId()+"</artifactId>\n"
        + "  <version>"+release.getVersion()+"</version>\n"
        + "  <packaging>kjar</packaging>\n"
        + "  <name>"+release.getProjectName()+"</name>\n"
        + "  <description>"+release.getProjectDescription()+"</description>\n"
        + "  <dependencies>\n"
//        + "    <dependency>\n"
//        + "      <groupId>com.thoughtworks.xstream</groupId>\n"
//        + "      <artifactId>xstream</artifactId>\n"
//        + "      <version>1.4.10</version>\n"
//        + "      <scope>test</scope>\n"
//        + "    </dependency>\n"
        + "    <dependency>\n"
        + "      <groupId>org.kie</groupId>\n"
        + "      <artifactId>kie-internal</artifactId>\n"
        + "      <version>7.15.0.Final</version>\n"
        + "      <scope>provided</scope>\n"
        + "    </dependency>\n"
//        + "    <dependency>\n"
//        + "      <groupId>org.optaplanner</groupId>\n"
//        + "      <artifactId>optaplanner-core</artifactId>\n"
//        + "      <version>7.15.0.Final</version>\n"
//        + "      <scope>provided</scope>\n"
//        + "    </dependency>\n"
//        + "    <dependency>\n"
//        + "      <groupId>junit</groupId>\n"
//        + "      <artifactId>junit</artifactId>\n"
//        + "      <version>4.12</version>\n"
//        + "      <scope>test</scope>\n"
//        + "    </dependency>\n"
//        + "    <dependency>\n"
//        + "      <groupId>org.optaplanner</groupId>\n"
//        + "      <artifactId>optaplanner-persistence-jaxb</artifactId>\n"
//        + "      <version>7.15.0.Final</version>\n"
//        + "      <scope>provided</scope>\n"
//        + "    </dependency>\n"
        + "    <dependency>\n"
        + "      <groupId>org.kie</groupId>\n"
        + "      <artifactId>kie-api</artifactId>\n"
        + "      <version>7.15.0.Final</version>\n"
        + "      <scope>provided</scope>\n"
        + "    </dependency>\n";

    for (IDeployableWorkItemHandler workitemhandler : deployableWorkitemhandlers) {
      pomXml+= "    <dependency>\n"
            + "      <groupId>"+workitemhandler.getPackage()+"</groupId>\n"
            + "      <artifactId>"+workitemhandler.getName()+"</artifactId>\n"
            + "      <version>1.0.0</version>\n"
            + "      <scope>compile</scope>\n"
            + "    </dependency>\n";
    }
  pomXml+= "  </dependencies>\n"
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
