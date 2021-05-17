/*
 * Copyright 2021 Arvato Systems GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arvato.workflow.kie4developer.common.impl;

import com.arvato.workflow.kie4developer.common.impl.kjar.JarUploader;
import com.arvato.workflow.kie4developer.common.impl.kjar.KJarBuilder;
import com.arvato.workflow.kie4developer.common.interfaces.IDeployableBPMNProcess;
import com.arvato.workflow.kie4developer.common.interfaces.IDeployableDependency;
import com.arvato.workflow.kie4developer.common.interfaces.IDeployableWorkItemHandler;
import com.arvato.workflow.kie4developer.common.interfaces.IDeploymentHelper;
import com.arvato.workflow.kie4developer.common.interfaces.IRelease;
import com.arvato.workflow.kie4developer.workitemhandler.JavaWorkItemHandler;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.StreamSupport;
import org.apache.maven.model.Dependency;
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
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class KieClientDeploymentHelper implements IDeploymentHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(KieClientDeploymentHelper.class);
  private IRelease release;
  private EffectivePomReader effectivePomReader;
  private KieClient kieClient;
  private KJarBuilder kJarBuilder;
  private JarUploader jarUploader;
  private FileSystemUtils fileSystemUtils;
  private List<IDeployableDependency> dependenciesToDeploy;
  private List<Class<? extends IDeployableBPMNProcess>> processesToDeploy;
  private List<Class<? extends IDeployableBPMNProcess>> processesToMock;
  private List<Class> serviceClassesToDeploy;
  private List<Class<? extends IDeployableWorkItemHandler>> workItemHandlersToDeploy;
  private Properties globals;
  private String kieServerHost;
  private String kieServerUrl;
  private String workbenchProtocol;
  private String workbenchHost;
  private int workbenchPort;
  private String workbenchContext;
  private String workbenchMavenContext;

  static {
    // change the optimizer to not generate negative IDs for entities on unittests and to be able to reuse db connections
    System.setProperty("hibernate.id.optimizer.pooled.preferred", "pooled-lo");
  }

  public KieClientDeploymentHelper(
      IRelease release,
      EffectivePomReader effectivePomReader,
      KJarBuilder kJarBuilder,
      KieClient kieClient,
      JarUploader jarUploader,
      FileSystemUtils fileSystemUtils,
      @Value("${kieworkbench.protocol}") String workbenchProtocol,
      @Value("${kieworkbench.host}") String workbenchHost,
      @Value("${kieworkbench.port}") int workbenchPort,
      @Value("${kieworkbench.context}") String workbenchContext,
      @Value("${kieworkbench.context.maven}") String workbenchMavenContext,
      @Value("${kieserver.host}") String kieServerHost,
      @Value("${kieserver.location}") String kieServerUrl,
      @Autowired Environment springEnv) {
    this.release = release;
    this.effectivePomReader = effectivePomReader;
    this.kJarBuilder = kJarBuilder;
    this.kieClient = kieClient;
    this.jarUploader = jarUploader;
    this.fileSystemUtils = fileSystemUtils;
    this.kieServerHost = kieServerHost;
    this.kieServerUrl = kieServerUrl;
    this.workbenchProtocol = workbenchProtocol;
    this.workbenchHost = workbenchHost;
    this.workbenchPort = workbenchPort;
    this.workbenchContext = workbenchContext;
    this.workbenchMavenContext = workbenchMavenContext;
    this.processesToDeploy = new ArrayList<>(
        new Reflections(this.release.getGroupId() + ".processes").getSubTypesOf(IDeployableBPMNProcess.class));
    this.processesToMock = new ArrayList<>();
    this.serviceClassesToDeploy = new ArrayList<>(
        new Reflections(this.release.getGroupId() + ".services", new SubTypesScanner(false))
            .getSubTypesOf(Object.class));
    this.workItemHandlersToDeploy = new ArrayList<>(
        new Reflections(this.release.getGroupId() + ".workitemhandler")
            .getSubTypesOf(IDeployableWorkItemHandler.class));
    this.workItemHandlersToDeploy.add(JavaWorkItemHandler.class);
    this.dependenciesToDeploy = getDependencies();
    this.globals = getGlobals(springEnv);

    System.setProperty("kieserver.location", this.kieServerUrl); // required for JavaWorkItemHandler
  }

  /**
   * Get all application properties with prefix "global."
   * @param springEnv Spring environment
   * @return the global Properties
   */
  private Properties getGlobals(Environment springEnv){
    Properties globals = new Properties();
    MutablePropertySources propSrcs = ((AbstractEnvironment) springEnv).getPropertySources();
    StreamSupport.stream(propSrcs.spliterator(), false)
        .filter(ps -> ps instanceof EnumerablePropertySource)
        .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
        .flatMap(Arrays::stream)
        .filter(propName -> propName.startsWith("global."))
        .forEach(propName -> globals.setProperty(propName.substring("global.".length()), springEnv.getProperty(propName)));
    return globals;
  }

  /**
   * Get the dependencies defined within maven pom.xml
   *
   * @return the list of dependencies
   */
  private List<IDeployableDependency> getDependencies() {
    List<IDeployableDependency> dependencyList = new ArrayList<>();
    for (Dependency dependency : effectivePomReader.getPomModel().getDependencies()) {
      if ("compile".equalsIgnoreCase(dependency.getScope()) || dependency.getScope() == null) {
        dependencyList.add(new IDeployableDependency() {
          @Override
          public String getMavenGroupId() {
            return dependency.getGroupId();
          }

          @Override
          public String getMavenArtifactId() {
            return dependency.getArtifactId();
          }

          @Override
          public String getMavenVersionId() {
            return dependency.getVersion();
          }
        });
      }
    }
    return dependencyList;
  }

  @Override
  public IRelease getRelease() {
    return release;
  }

  @Override
  public void setDependenciesToDeploy(List<IDeployableDependency> dependenciesToDeploy) {
    this.dependenciesToDeploy = dependenciesToDeploy;
  }

  @Override
  public void setProcessesToDeploy(List<Class<? extends IDeployableBPMNProcess>> processesToDeploy) {
    this.processesToDeploy = processesToDeploy;
  }

  @Override
  public void setProcessesToDeploy(List<Class<? extends IDeployableBPMNProcess>> processesToDeploy, List<Class<? extends IDeployableBPMNProcess>> processesToMock) {
    this.processesToDeploy = processesToDeploy;
    this.processesToMock = processesToMock;
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
      if (!undeploy(true)){
        return false;
      }
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
          .buildKjar(dependenciesToDeploy, processesToDeploy, processesToMock, workItemHandlersToDeploy, serviceClassesToDeploy, globals);
    } catch (Exception e) {
      LOGGER.error("Error while creating the kjar file", e);
      return false;
    }

    // next we can upload this file into kie-server maven repo
    try {
      uploadJar(jarAndPomFile.get("jar"), jarAndPomFile.get("pom"));
    } catch (Exception e) {
      LOGGER.error("Error while uploading jar file {}. This could be also caused by missing dependencies", jarAndPomFile.get("jar").getAbsolutePath(), e);
      return false;
    }

    // next we create the new runtime container in kie-server
    try {
      createContainer();
    } catch (Exception e) {
      LOGGER.error("Error while creating container {}", release.getContainerId(), e);
      return false;
    }

    LOGGER.info("Deployment successful");
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

    if (kieServerHost.contains("localhost") || kieServerHost.contains("127.0.0.1")) {
      // if running on local jbpm server provide the artifacts via local maven repository
      File repositoryDir = fileSystemUtils.createTempDirectory().toFile();
      String repositoryUrl = repositoryDir.toURI().toURL().toExternalForm();

      // workaround to reset the maven repository for the jbpm server. This is required for running multiple unittest.
      MavenRepository.defaultMavenRepository = null;
      Aether.instance = null;
      KieRepositoryImpl.setInternalKieScanner(new KieRepositoryScannerImpl());
      MavenSettings.getSettings().setLocalRepository(repositoryDir.getAbsolutePath());

      // install the jar into the temp local repository
      MavenRepository.getMavenRepository().installArtifact(release.getReleaseIdForServerAPI(), jarFile, pomFile);
      File jarInRepo = new File(
          repositoryDir.getAbsolutePath() + "/" + groupIdAsUrl + "/" + artifactId + "/" + versionId + "/" + artifactId
              + "-" + versionId + ".jar");
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

  @Override
  public boolean undeploy(boolean cancelAllRunningInstances) {
    return undeploy(release.getContainerId(), cancelAllRunningInstances);
  }

  /**
   * Undeploy a running container
   *
   * @param containerId the container id to undeploy
   * @param cancelAllRunningInstances <code>true</code> if running process instances should be canceled
   * @return <code>true</code> if undeployment was successful, otherwise <code>false</code>
   */
  private boolean undeploy(String containerId, boolean cancelAllRunningInstances) {
    LOGGER.info("Undeployment on KIE-Server...");
    // send deployment command to server
    KieContainerResource container = kieClient.getKieServicesClient().getContainerInfo(containerId).getResult();
    if (container != null) {
      // if the container is unhealthy we must fix this first by re-deploy it.
      if (!container.getStatus().equals(KieContainerStatus.STARTED) && cancelAllRunningInstances) {
        LOGGER.error("Error while aborting Process Instances: KIE Container {} is in status {} which does not allow aborts of process instances", containerId, container.getStatus().name());
        return false;
      }

      boolean retry;
      do {
        retry = false;

        // check if we have running process instances
        List<ProcessInstance> processInstances = kieClient.getProcessClient()
            .findProcessInstances(containerId, 0, Integer.MAX_VALUE);
        List<Long> processInstanceIdsToAbort = new ArrayList<>();
        int chunkSize = 100;
        for (int i = 0; i < processInstances.size(); i++) {
          if (i == chunkSize) {
            retry = true;
            break;
          }
          processInstanceIdsToAbort.add(processInstances.get(i).getId());
        }

        if (processInstanceIdsToAbort.size() > 0) {
          if (!cancelAllRunningInstances) {
            LOGGER.error("Error disposing KIE Container {}. It contains active process instances", containerId);
            return false;
          }else {
            try {
              kieClient.getProcessClient().abortProcessInstances(containerId, processInstanceIdsToAbort);
              LOGGER.info("{} Process Instances aborted", processInstanceIdsToAbort.size());
            } catch (KieServicesHttpException e) {
              // this case happens when a subprocess instance was already canceled by the related parent instance
              if (e.getResponseBody().contains("Could not find process instance with id")) {
                LOGGER.warn("Error while aborting {} Process Instances. Message: {}", processInstanceIdsToAbort.size(),
                    e.getResponseBody());
                retry = true;
              } else {
                throw e;
              }
            }
          }
        }
      } while (retry);

      ServiceResponse<Void> responseDispose = kieClient.getKieServicesClient().disposeContainer(containerId);
      if (responseDispose.getType() == ResponseType.FAILURE) {
        LOGGER.error("Error disposing KIE Container {}. Message: {}", containerId, responseDispose.getMsg());
        return false;
      } else {
        LOGGER.warn(
            "Removing of deployed jars from the kie server and kie workbench internal maven repository isn't supported. "
                + "Please take care when you re-deploying jars with the same name.");
        LOGGER.info("KIE Container disposed: {}", containerId);
      }
    } else {
      LOGGER.info("KIE Container {} does not exist.", containerId);
    }

    LOGGER.info("Undeployment successful");
    return true;
  }
}
