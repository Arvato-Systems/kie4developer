package org.kie.client.springboot.samples.client;

import java.io.File;
import java.util.List;
import org.kie.client.springboot.samples.client.kjar.JarUploader;
import org.kie.client.springboot.samples.client.kjar.KJarBuilder;
import org.kie.client.springboot.samples.common.interfaces.IDeployableBPMNProcess;
import org.kie.client.springboot.samples.common.interfaces.IDeployableWorkItemHandler;
import org.kie.client.springboot.samples.common.interfaces.IDeploymentHelper;
import org.kie.client.springboot.samples.common.interfaces.IRelease;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieServiceResponse.ResponseType;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class KieClientDeploymentHelper implements IDeploymentHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(KieClientDeploymentHelper.class);
  @Value("${kieworkbench.protocol}")
  private String workbenchProtocol;
  @Value("${kieworkbench.host}")
  private String workbenchHost;
  @Value("${kieworkbench.port}")
  private int workbenchPort;
  @Value("${kieworkbench.context}")
  private String workbenchContext;
  @Value("${kieworkbench.context.maven}")
  private String workbenchMavenContext;
  @Value("${kieworkbench.user}")
  private String workbenchUser;
  @Value("${kieworkbench.pwd}")
  private String workbenchPassword;
  @Autowired
  private KieClient kieClient;
  @Autowired
  private IRelease release;
  @Autowired
  private KJarBuilder kJarBuilder;
  @Autowired
  private JarUploader jarUploader;
  private IDeployableBPMNProcess processToDeploy;
  private List<IDeployableWorkItemHandler> workItemHandlerToDeploy;

  @Override
  public IRelease getRelease() { return release; }

  @Override
  public void setProcessToDeploy(IDeployableBPMNProcess processToDeploy) { this.processToDeploy = processToDeploy; }

  @Override
  public void setWorkItemHandler(List<IDeployableWorkItemHandler> workItemHandlerToDeploy) { this.workItemHandlerToDeploy = workItemHandlerToDeploy; }

  @Override
  public boolean deploy() {
    deployWorkItemHandlerIfExist();
    deployJarIfExist();

    // send deployment command to server
    String containerId = release.getContainerId();
    ReleaseId releaseId = release.getReleaseIdForServerAPI();
    KieContainerResource resource = new KieContainerResource(containerId, releaseId);
    ServiceResponse<KieContainerResource> createResponse = kieClient.getKieServicesClient().createContainer(containerId, resource);
    if (createResponse.getType() == ResponseType.FAILURE) {
      LOGGER.error("Error creating " + containerId + ". Message: " + createResponse.getMsg());
      return false;
    } else {
      LOGGER.info("Container " + containerId + " for release " + releaseId + " successful created.");
    }
    return true;
  }

  /**
   * Deploy a kjar into the Server Container
   * @see {https://developers.redhat.com/blog/2018/03/14/what-is-a-kjar/}
   */
  private void deployJarIfExist() {
    File jarFile = processToDeploy.getJarFile();

    if (!processToDeploy.isDistributedAsJar()) {
      // in this case we have to build the kjar by our own first
			try{
        jarFile = kJarBuilder.buildKjar(processToDeploy, workItemHandlerToDeploy);
        //pomFile = kJarBuilder.buildPomXml();
      }catch (Exception e){
        throw new RuntimeException("Kjar or pom generation error", e);
      }
      LOGGER.info("Kjar created successfull: " + jarFile.getAbsolutePath());
    }

    // There exist multiple ways to make the kjar artifact available for the kie-server. In most of the cases the artifact gets fetched from the related workbench setup.
    // Upload jar file to the Workbench maven repository by via Workbench GUI: Login into jbpm.console and goto 'Authoring'-->Artifact Repository-->Upload
    // Upload jar file to the Workbench maven repository by maven deploy goal: https://access.redhat.com/documentation/en-us/red_hat_jboss_bpm_suite/6.1/html/development_guide/uploading_artifacts_to_maven_repository / mvn deploy:deploy-file -DgroupId=org.kie.example -DartifactId=project1 -Dversion=1.0.4 -Dpackaging=jar -Dfile=/NotBackedUp/repository1/project1/target/project1-1.0.4.jar -DrepositoryId=guvnor-m2-repo -Durl=http://localhost:8080/jbpm-console/maven2/
    // Upload jar file to the Workbench maven repository by http request: curl -v -X POST -F data=@"/path/to/Project1-1.0.jar" -H 'Content-Type: multipart/form-data' -u User:Password http://localhost:8080/business-central/maven2/
    // Upload jar file to the Workbench maven repository by using Workbench's REST API: https://docs.jboss.org/jbpm/release/7.6.0.Final/jbpm-docs/html_single/#_maven_calls / [POST] http://localhost:8080/business-central/rest/deployment/groupID:ArtifactID:Version/deploy
    // Copy the jar into the workbench's maven repo folder: docker cp ...
    // Copy the jar into the kie-server's maven repo folder: docker cp ...
    // extend the maven settings.xml on the kie-server to fetch artifacts from other repositories

    //Maven coordinates
    String groupId = release.getGroupId();
    String artifactId = release.getArtifactId();
    String version = release.getVersion();

    String url = workbenchProtocol + "://" + workbenchHost + ":" + workbenchPort + "/" + workbenchContext + "/"
        + workbenchMavenContext + "/" + groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId
        + "-" + version + ".jar";

    ResponseEntity<String> response = null;
    try {
      response = jarUploader.uploadFile(jarFile, url, workbenchUser, workbenchPassword);
      if (response.getStatusCode().is2xxSuccessful()) {
        LOGGER.info("Jar file " + jarFile.getName() + " successful uploaded into workbench");
      }
    } catch (Exception e) {
      throw new RuntimeException("Error while jar file upload. This could be also caused by missing dependencies.", e);
    }
  }

  /**
   * Deploy a jar (containing workitemhandler) into the Server Container
   * @see {https://developers.redhat.com/blog/2018/03/14/what-is-a-kjar/}
   */
  private void deployWorkItemHandlerIfExist() {
    if (workItemHandlerToDeploy != null){
      for (IDeployableWorkItemHandler workItemHandler : workItemHandlerToDeploy) {
        File jarFile = workItemHandler.getWorkItemHandlerJarFile();

        //Maven coordinates
        String groupId = workItemHandler.getPackage();
        String artifactId = workItemHandler.getName();
        String version = workItemHandler.getVersion();
        String url = workbenchProtocol + "://" + workbenchHost + ":" + workbenchPort + "/" + workbenchContext + "/"
            + workbenchMavenContext + "/" + groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/"
            + artifactId + "-" + version + ".jar";

        ResponseEntity<String> response = jarUploader.uploadFile(jarFile, url, workbenchUser, workbenchPassword);
        LOGGER.info("Jar file " + jarFile.getName() + " successful (status: " + response.getStatusCode()
            + ") uploaded into workbench");
      }
    }
  }

  @Override
  public boolean undeploy() {
    // send deployment command to server
    String containerId = release.getContainerId();
    KieContainerResource container = kieClient.getKieServicesClient().getContainerInfo(containerId).getResult();
    boolean result = false;
    if (container != null) {
      ServiceResponse<Void> responseDispose = kieClient.getKieServicesClient().disposeContainer(containerId);
      if (responseDispose.getType() == ResponseType.FAILURE) {
        LOGGER.error("Error disposing " + containerId + ". Message: " + responseDispose.getMsg());
      } else {
        LOGGER.info("Container disposed: " + containerId + ". ");
        undeployJarIfExist();
        undeployWorkItemHandlerIfExist();
        result = true;
      }
    }
    return result;
  }

  /**
   * Undeploy a kjar from the Server
   */
  private void undeployJarIfExist() {
    if (processToDeploy.isDistributedAsJar()) {
      LOGGER.warn("removing jars from the server repo isn't possible.");
    }
  }

  /**
   * Undeploy a jar (containing workitemhandler) from the Server
   */
  private void undeployWorkItemHandlerIfExist() {
    LOGGER.warn("removing jars from the server repo isn't possible.");
  }

}
