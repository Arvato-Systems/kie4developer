package com.arvato.workflow.kie4developer.common.impl;

import com.arvato.workflow.kie4developer.common.impl.kjar.JarUploader;
import com.arvato.workflow.kie4developer.common.impl.kjar.KJarBuilder;
import com.arvato.workflow.kie4developer.common.interfaces.IDeployableBPMNProcess;
import com.arvato.workflow.kie4developer.common.interfaces.IDeployableDependency;
import com.arvato.workflow.kie4developer.common.interfaces.IDeployableWorkItemHandler;
import com.arvato.workflow.kie4developer.common.interfaces.IDeploymentHelper;
import com.arvato.workflow.kie4developer.common.interfaces.IRelease;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.appformer.maven.integration.embedder.MavenSettings;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.kie.server.api.exception.KieServicesHttpException;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieServiceResponse.ResponseType;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.api.model.admin.MigrationReportInstance;
import org.kie.server.api.model.instance.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class KieClientDeploymentHelper implements IDeploymentHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(KieClientDeploymentHelper.class);
  private IRelease release;
  private KieClient kieClient;
  private KJarBuilder kJarBuilder;
  private JarUploader jarUploader;
  private List<Class<? extends IDeployableDependency>> dependenciesToDeploy;
  private List<Class<? extends IDeployableBPMNProcess>> processesToDeploy;
  private List<Class> serviceClassesToDeploy;
  private List<Class<? extends IDeployableWorkItemHandler>> workItemHandlersToDeploy;
  @Value("${maven.repository}")
  private String mavenRepoPath;
  @Value("${kieserver.location}")
  private String kieServerUrl;
  // kie workbench connection parameter
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
  private String workbenchPwd;

  public KieClientDeploymentHelper(KJarBuilder kJarBuilder, IRelease release, KieClient kieClient,
      JarUploader jarUploader) {
    this.kJarBuilder = kJarBuilder;
    this.release = release;
    this.kieClient = kieClient;
    this.jarUploader = jarUploader;
    this.dependenciesToDeploy = new ArrayList<>();
    this.processesToDeploy = new ArrayList<>();
    this.serviceClassesToDeploy = new ArrayList<>();
    this.workItemHandlersToDeploy = new ArrayList<>();
  }

  @Override
  public IRelease getRelease() {
    return release;
  }

  @Override
  public void setDependenciesToDeploy(List<Class<? extends IDeployableDependency>> dependenciesToDeploy) {
    this.dependenciesToDeploy = dependenciesToDeploy;
  }

  @Override
  public void setProcessesToDeploy(List<Class<? extends IDeployableBPMNProcess>> processesToDeploy) {
    this.processesToDeploy = processesToDeploy;
  }

  @Override
  public void setServiceClassesToDeploy(List<Class> serviceClassesToDeploy) {
    this.serviceClassesToDeploy = serviceClassesToDeploy;
  }

  @Override
  public void setWorkItemHandlersToDeploy(List<Class<? extends IDeployableWorkItemHandler>> workItemHandlerToDeploy) {
    this.workItemHandlersToDeploy = workItemHandlerToDeploy;
  }

  @Override
  public List<MigrationReportInstance> deployWithMigration(String oldContainerId) {
    List<MigrationReportInstance> migrationReport = new ArrayList<>();
    if (deploy(false)) {
      LOGGER.info("Migrating old process instances on KIE-Server...");
      // check if we have running process instances which can be migrated
      List<ProcessInstance> processInstances = kieClient.getProcessClient()
          .findProcessInstances(oldContainerId, 0, Integer.MAX_VALUE);
      List<Long> processInstanceIds;

      for (Class<? extends IDeployableBPMNProcess> processesToDeploy : processesToDeploy) {
        processInstanceIds = new ArrayList<>();
        for (ProcessInstance processInstance : processInstances) {
          try {
            IDeployableBPMNProcess instance = processesToDeploy.newInstance();
            if (processInstance.getProcessName().equals(instance.getName())) {
              processInstanceIds.add(processInstance.getId());
            }
            migrationReport.addAll(kieClient.getProcessAdminClient()
                .migrateProcessInstances(oldContainerId, processInstanceIds,
                    release.getContainerId(), instance.getProcessId()));
            for (MigrationReportInstance report : migrationReport){
              if (!report.isSuccessful()){
                LOGGER.error("MigrationReport - failed to migrate process instance {}.\n{}", report.getProcessInstanceId(), report.getLogs());
              }else{
                LOGGER.info("MigrationReport - process instance {} successful migrated.", report.getProcessInstanceId());
              }
            }
          } catch (InstantiationException e) {
            LOGGER.error("Error while creating new instance of class", e);
          } catch (IllegalAccessException e) {
            LOGGER.error("Error while creating new instance of class", e);
          }
        }
        // undeploy old container
        undeploy(oldContainerId, false);
      }
      LOGGER.info("Migration complete");
    }
    return migrationReport;
  }

  @Override
  public boolean deploy(boolean overwrite) {
    if (overwrite) {
      undeploy(true);
    }
    LOGGER.info("Deploying to KIE-Server...");

    /*  There exist multiple ways to make the kjar artifact available for the kie-server. In most of the cases the artifact gets fetched from the related workbench setup.
     *  Upload jar file to the Workbench maven repository by via Workbench GUI: Login into jbpm.console and goto 'Authoring'-->Artifact Repository-->Upload
     *  Upload jar file to the Workbench maven repository by maven deploy goal: https://access.redhat.com/documentation/en-us/red_hat_jboss_bpm_suite/6.1/html/development_guide/uploading_artifacts_to_maven_repository / mvn deploy:deploy-file -DgroupId=org.kie.example -DartifactId=project1 -Dversion=1.0.4 -Dpackaging=jar -Dfile=/NotBackedUp/repository1/project1/target/project1-1.0.4.jar -DrepositoryId=guvnor-m2-repo -Durl=http://localhost:8080/jbpm-console/maven2/
     *  Upload jar file to the Workbench maven repository by http request: curl -v -X POST -F data=@"/path/to/Project1-1.0.jar" -H 'Content-Type: multipart/form-data' -u User:Password http://localhost:8080/business-central/maven2/
     *  Upload jar file to the Workbench maven repository by using Workbench's REST API: https://docs.jboss.org/jbpm/release/7.6.0.Final/jbpm-docs/html_single/#_maven_calls / [POST] http://localhost:8080/business-central/rest/deployment/groupID:ArtifactID:Version/deploy
     *  Copy the jar into the workbench's maven repo folder: docker cp ...
     *  Copy the jar into the kie-server's maven repo folder: docker cp ...
     *  extend the maven settings.xml on the kie-server to fetch artifacts from other repositories
     */

    // first we have to build the kjar release file by our own
    File jarFile;
    try {
      jarFile = kJarBuilder
          .buildKjar(dependenciesToDeploy, processesToDeploy, workItemHandlersToDeploy, serviceClassesToDeploy);
    } catch (Exception e) {
      LOGGER.error("Error while creating the kjar file", e);
      return false;
    }

    // next we can upload this file into kie-server maven repo
    try {
      uploadJar(jarFile);
    } catch (Exception e) {
      LOGGER.error(String
          .format("Error while uploading jar file %s. This could be also caused by missing dependencies",
              jarFile.getAbsolutePath()), e);
      return false;
    }

    // next we create the new runtime container in kie-server
    try {
      createContainer();
    } catch (Exception e) {
      LOGGER.error(String.format("Error while creating container %s", release.getContainerId()), e);
      return false;
    }

    LOGGER.info("Deployment complete");
    return true;
  }

  /**
   * Upload a kjar into the KIE Server
   *
   * @param jarFile the file to upload
   * @throws Exception on any Exception
   * @see {https://developers.redhat.com/blog/2018/03/14/what-is-a-kjar/}
   */
  private void uploadJar(File jarFile) throws Exception {
    // Maven coordinates
    String groupIdAsUrl = release.getGroupId().replace('.', '/');
    String artifactId = release.getArtifactId();
    String versionId = release.getVersion();

    String mavenBaseUrl = workbenchProtocol + "://" + workbenchHost + ":" + workbenchPort + "/" + workbenchContext + "/"
        + workbenchMavenContext;
    String url = mavenBaseUrl + "/" + groupIdAsUrl + "/" + artifactId + "/" + versionId + "/"
        + artifactId
        + "-" + versionId + ".jar";

    ResponseEntity<String> response = jarUploader.uploadFile(jarFile, url);

    //if running the JBPM Server locally add the kie workbench as possible target to fetch artifacts... as alternative you could add this to your system local settings.xml
    MavenSettings.getMavenRepositoryConfiguration().getRemoteRepositoriesForRequest().add(
        new RemoteRepository.Builder("kie-workbench", "default", mavenBaseUrl)
            .setAuthentication(new AuthenticationBuilder().addUsername(workbenchUser).addPassword(workbenchPwd).build())
            .build()
    );

    if (response.getStatusCode().is2xxSuccessful()) {
      LOGGER.info("Jar file {} successful uploaded into kie workbench", jarFile.getName());
    } else {
      throw new IOException(String
          .format("Error while uploading jar file %s. This could be also caused by missing dependencies.",
              jarFile.getAbsolutePath()));
    }
  }

  /**
   * Start a new Container within KIE Server
   *
   * @throws Exception on any Exception
   */
  private void createContainer() throws Exception {
    String groupIdAsUrl = release.getGroupId().replace('.', '/');
    String artifactId = release.getArtifactId();
    String versionId = release.getVersion();

    // if running the JBPM Server local we can cleanup existing old versions with same name to prevent loading issues
    if (kieServerUrl.contains("localhost") || kieServerUrl.contains("127.0.0.1")){
      try {
        File oldJarFile = new File(mavenRepoPath + File.separator + groupIdAsUrl + File.separator + artifactId +
            File.separator + versionId + File.separator + artifactId + "-" + versionId + ".jar");
        if (oldJarFile.exists()){
          oldJarFile.delete();
        }
        File oldPomFile = new File(mavenRepoPath + File.separator + groupIdAsUrl + File.separator + artifactId +
            File.separator + versionId + File.separator + artifactId + "-" + versionId + ".pom");
        if (oldPomFile.exists()){
          oldPomFile.delete();
        }
        File resolverStatusFile = new File(mavenRepoPath + File.separator + groupIdAsUrl + File.separator + artifactId +
            File.separator + versionId + File.separator + "resolver-status.properties");
        if (resolverStatusFile.exists()){
          resolverStatusFile.delete();
        }
      } catch (Exception e) {
        LOGGER.warn("Can't delete old releases in local maven repository", e);
      }
    }

    String containerId = release.getContainerId();
    String containerAlias = release.getContainerAlias();
    ReleaseId releaseId = release.getReleaseIdForServerAPI();
    KieContainerResource resource = new KieContainerResource(containerId, releaseId);
    resource.setContainerAlias(containerAlias);

    // send deployment command to server
    ServiceResponse<KieContainerResource> createResponse = kieClient.getKieServicesClient()
        .createContainer(containerId, resource);
    if (createResponse.getType() == ResponseType.FAILURE) {
      throw new Exception(
          String.format("Error while creating container %s. Message: %s", containerId, createResponse.getMsg()));
    }
    LOGGER.info("Container {} for release {} successful created", containerId, releaseId);
  }

  private boolean undeploy(String containerId, boolean cancelAllRunningInstances) {
    LOGGER.info("Undeployment on KIE-Server...");
    // send deployment command to server
    KieContainerResource container = kieClient.getKieServicesClient().getContainerInfo(containerId).getResult();
    boolean result = false;
    if (container != null) {
      if (cancelAllRunningInstances) {
        boolean retry;
        do{
          retry = false;
          // check if we have running process instances
          List<ProcessInstance> processInstances = kieClient.getProcessClient()
              .findProcessInstances(containerId, 0, Integer.MAX_VALUE);
          List<Long> processInstanceIds = new ArrayList<>();
          for (ProcessInstance processInstance : processInstances) {
            processInstanceIds.add(processInstance.getId());
          }
          if (processInstanceIds.size() > 0) {
            try{
              kieClient.getProcessClient().abortProcessInstances(containerId, processInstanceIds);
              LOGGER.info("{} Process Instances aborted", processInstanceIds.size());
            }catch (KieServicesHttpException e){
              if (e.getResponseBody().contains("Could not find process instance with id")){
                // this case happens when a subprocess instance was already canceled by the related parent instance
                retry = true;
              }else{
                throw e;
              }
            }
          }
        } while (retry);
      }

      ServiceResponse<Void> responseDispose = kieClient.getKieServicesClient().disposeContainer(containerId);
      if (responseDispose.getType() == ResponseType.FAILURE) {
        LOGGER.error("Error disposing {}. Message: {}", containerId, responseDispose.getMsg());
      } else {
        LOGGER.warn(
            "Removing of deployed jars from the kie server and kie workbench internal maven repository isn't supported. "
                + "Please take care when you re-deploying jars with the same name");
        LOGGER.info("Container disposed: {}", containerId);
        result = true;
      }
    } else {
      LOGGER.info("Container {} does not exist", containerId);
      result = true;
    }

    LOGGER.info("Undeployment complete");
    return result;
  }

  @Override
  public boolean undeploy(boolean cancelAllRunningInstances) {
    return undeploy(release.getContainerId(), cancelAllRunningInstances);
  }

}
