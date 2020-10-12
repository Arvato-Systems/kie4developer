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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.appformer.maven.integration.Aether;
import org.appformer.maven.integration.MavenRepository;
import org.appformer.maven.integration.embedder.MavenSettings;
import org.drools.compiler.kie.builder.impl.KieRepositoryImpl;
import org.eclipse.aether.repository.RemoteRepository;
import org.kie.scanner.KieRepositoryScannerImpl;
import org.kie.server.api.exception.KieServicesHttpException;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerStatus;
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
  @Value("${kieserver.host}")
  private String kieserverHost;

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
            for (MigrationReportInstance report : migrationReport) {
              if (!report.isSuccessful()) {
                LOGGER.error("MigrationReport - failed to migrate process instance {}.\n{}",
                    report.getProcessInstanceId(), report.getLogs());
              } else {
                LOGGER
                    .info("MigrationReport - process instance {} successful migrated.", report.getProcessInstanceId());
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
    Map<String, File> jarAndPomFile;
    try {
      jarAndPomFile = kJarBuilder
          .buildKjar(dependenciesToDeploy, processesToDeploy, workItemHandlersToDeploy, serviceClassesToDeploy);
    } catch (Exception e) {
      LOGGER.error("Error while creating the kjar file", e);
      return false;
    }

    // next we can upload this file into kie-server maven repo
    try {
      uploadJar(jarAndPomFile.get("jar"), jarAndPomFile.get("pom"));
    } catch (Exception e) {
      LOGGER.error(String
          .format("Error while uploading jar file %s. This could be also caused by missing dependencies",
              jarAndPomFile.get("jar").getAbsolutePath()), e);
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
   * @param jarFile the jar file to upload/install
   * @param pomFile the pom file to upload/install
   * @throws Exception on any Exception
   * @see {https://developers.redhat.com/blog/2018/03/14/what-is-a-kjar/}
   */
  private void uploadJar(File jarFile, File pomFile) throws Exception {
    // Maven coordinates
    String groupIdAsUrl = release.getGroupId().replace('.', '/');
    String artifactId = release.getArtifactId();
    String versionId = release.getVersion();

    if (kieserverHost.contains("localhost") || kieserverHost.contains("127.0.0.1")) {
      // if running on local jbpm server provide the artifacts via local maven repository
      File repositoryDir = Files.createTempDirectory(UUID.randomUUID().toString()).toFile();
      String repositoryUrl = repositoryDir.toURI().toURL().toExternalForm();

      // workaround to reset the maven repository for the jbpm server. This is required for running multiple unittest.
      MavenRepository.defaultMavenRepository = null;
      Aether.instance = null;
      KieRepositoryImpl.setInternalKieScanner(new KieRepositoryScannerImpl());
      MavenSettings.getSettings().setLocalRepository(repositoryDir.getAbsolutePath());

      // install the jar into the temp local repository
      MavenRepository.getMavenRepository().installArtifact(release.getReleaseIdForServerAPI(), jarFile, pomFile);
      File jarInRepo = new File(repositoryDir.getAbsolutePath() + "/" + groupIdAsUrl + "/" + artifactId + "/" + versionId + "/" + artifactId + "-" + versionId + ".jar");
      if (jarInRepo.exists()) {
        LOGGER
            .info("Jar file {} successful installed into local maven repository: {}", jarFile.getName(), repositoryDir);
      } else {
        throw new IOException(String
            .format("Error while installing jar file into local maven repository %s.",
                jarFile.getAbsolutePath()));
      }
      // add the temp local maven repository as new remote repository for the local running kie server; the server fetch from there
      MavenSettings.getMavenRepositoryConfiguration().getRemoteRepositoriesForRequest().clear();
      RemoteRepository remoteRepository = new RemoteRepository.Builder("local", "maven2", repositoryUrl).build();
      MavenSettings.getMavenRepositoryConfiguration().getRemoteRepositoriesForRequest().add(remoteRepository);
    } else {
      // if deployment target is a external jbpm server provide the artifacts via kie workbench
      String mavenBaseUrl =
          workbenchProtocol + "://" + workbenchHost + ":" + workbenchPort + "/" + workbenchContext + "/"
              + workbenchMavenContext;
      String url = mavenBaseUrl + "/" + groupIdAsUrl + "/" + artifactId + "/" + versionId + "/"
          + artifactId
          + "-" + versionId + ".jar";

      ResponseEntity<String> response = jarUploader.uploadFile(jarFile, url);
      if (response.getStatusCode().is2xxSuccessful()) {
        LOGGER.info("Jar file {} successful uploaded into kie workbench: {}", jarFile.getName(), mavenBaseUrl);
      } else {
        throw new IOException(String
            .format(
                "Error while uploading jar file %s to kie workbench. This could be also caused by missing dependencies.",
                jarFile.getAbsolutePath()));
      }
    }
  }

  /**
   * Start a new Container within KIE Server
   *
   * @throws Exception on any Exception
   */
  private void createContainer() throws Exception {
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

      // if the container is unhealthy we must fix this first by re-deploy it.
      if (!container.getStatus().equals(KieContainerStatus.STARTED) && cancelAllRunningInstances){
        LOGGER.warn("Failure while aborting Process Instances: Container is in status {} which does not allow aborts.", container.getStatus().name());
        cancelAllRunningInstances = false;
      }

      if (cancelAllRunningInstances) {
        boolean retry;
        do {
          retry = false;

          // check if we have running process instances
          List<ProcessInstance> processInstances = kieClient.getProcessClient()
              .findProcessInstances(containerId, 0, Integer.MAX_VALUE);
          List<Long> processInstanceIds = new ArrayList<>();
          int chunkSize = 100;
          for (int i = 0; i < processInstanceIds.size(); i++){
            if (i == chunkSize){
              retry = true;
              break;
            }
            processInstanceIds.add(processInstances.get(i).getId());
          }

          if (processInstanceIds.size() > 0) {
            try {
              kieClient.getProcessClient().abortProcessInstances(containerId, processInstanceIds);
              LOGGER.info("{} Process Instances aborted", processInstanceIds.size());
            } catch (KieServicesHttpException e) {
              // this case happens when a subprocess instance was already canceled by the related parent instance
              if (e.getResponseBody().contains("Could not find process instance with id")) {
                LOGGER.warn("Failure while aborting {} Process Instances: {}", processInstanceIds.size(),
                    e.getResponseBody());
                retry = true;
              } else {
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
