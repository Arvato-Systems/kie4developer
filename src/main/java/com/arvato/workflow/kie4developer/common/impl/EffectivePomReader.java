package com.arvato.workflow.kie4developer.common.impl;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectModelResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.apache.maven.settings.io.DefaultSettingsReader;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.internal.impl.DefaultRemoteRepositoryManager;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RemoteRepository.Builder;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.transport.wagon.WagonTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.ConservativeAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Helper to read maven pom.xml
 *
 * @author TRIBE01
 */
@Component
public class EffectivePomReader {

  private static final Logger LOGGER = LoggerFactory.getLogger(EffectivePomReader.class);
  private FileSystemUtils fileSystemUtils;
  private Path mavenRepository;
  private Path mavenSettings;
  private Model cachedModel;

  public EffectivePomReader(FileSystemUtils fileSystemUtils, @Value("${maven.repository}") String mavenRepository, @Value("${maven.settings}") String mavenSettings) {
    this.fileSystemUtils = fileSystemUtils;
    this.mavenRepository = Paths.get(mavenRepository);
    this.mavenSettings = Paths.get(mavenSettings);
    this.cachedModel = null;
  }

  /**
   * Get the effective pom.xml
   *
   * @return the resolved maven model or <code>null</code>
   */
  public Model getPomModel() {
    if (cachedModel != null) {
      return cachedModel;
    }
    File pomFile;
    if (fileSystemUtils.runAsFatJar()) {
      pomFile = fileSystemUtils.getFile( Paths.get("META-INF", "maven")).listFiles()[0].listFiles()[0].toPath().resolve("pom.xml").toFile();
    } else {
      pomFile = fileSystemUtils.getFile( Paths.get("pom.xml"));
    }
    try {
      DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
      locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
      locator.addService(TransporterFactory.class, FileTransporterFactory.class);
      locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
      locator.addService(TransporterFactory.class, WagonTransporterFactory.class);

      RepositorySystem system = locator.getService(RepositorySystem.class);
      DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
      LocalRepository localRepo = new LocalRepository(mavenRepository.toFile());
      session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

      RequestTrace requestTrace = new RequestTrace(null);
      RemoteRepositoryManager remoteRepositoryManager = locator.getService(RemoteRepositoryManager.class);
      List<RemoteRepository> repos = new ArrayList<>();

      // load profile from settings.xml
      File mavenSettingsFile = mavenSettings.toFile();
      if (mavenSettingsFile.exists()){
        Settings settings = new DefaultSettingsReader().read( mavenSettingsFile, Collections.emptyMap() );
        List<Repository> profileRepositories = settings.getProfilesAsMap().get(settings.getActiveProfiles().get(0)).getRepositories();

        repos = profileRepositories.stream()
            .map( profileRepository -> {
              Builder builder = new RemoteRepository.Builder(profileRepository.getId(), "default", profileRepository.getUrl());
              Optional<Server> profileServer = settings.getServers().stream().filter(s -> s.getId().equalsIgnoreCase(profileRepository.getId())).findFirst();
              if (profileServer.isPresent()){
                Authentication auth = new AuthenticationBuilder().addUsername(profileServer.get().getUsername()).addPassword(profileServer.get().getPassword()).build();
                builder.setAuthentication(auth);
              }
              return builder.build();
            })
            .collect(Collectors.toList());
      }else{
        LOGGER.warn("Maven settings.xml file not found");
      }

      // add the default remote repository
      repos.add(new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build());

      DefaultRepositorySystem repositorySystem = new DefaultRepositorySystem();
      repositorySystem.initService(locator);
      ModelResolver modelResolver =
          new ProjectModelResolver(session, requestTrace,
              repositorySystem, remoteRepositoryManager, repos,
              ProjectBuildingRequest.RepositoryMerging.POM_DOMINANT,
              null);

      DefaultModelBuildingRequest modelBuildingRequest = new DefaultModelBuildingRequest();
      modelBuildingRequest.setPomFile(pomFile);
      modelBuildingRequest.setModelResolver(modelResolver);
      modelBuildingRequest.setSystemProperties(System.getProperties());

      DefaultModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();

      cachedModel = modelBuilder.build(modelBuildingRequest).getEffectiveModel();
    } catch (ModelBuildingException | IOException e) {
      LOGGER.error("Error while resolving pom.xml", e);
    }
    return cachedModel;
  }

  private AuthenticationSelector createAuthenticationSelector(SettingsDecryptionResult decryptedSettings) {
    DefaultAuthenticationSelector selector = new DefaultAuthenticationSelector();
    for (Server server : decryptedSettings.getServers()) {
      AuthenticationBuilder auth = new AuthenticationBuilder();
      auth.addUsername(server.getUsername()).addPassword(server.getPassword());
      auth.addPrivateKey(server.getPrivateKey(), server.getPassphrase());
      selector.add(server.getId(), auth.build());
    }
    return new ConservativeAuthenticationSelector(selector);
  }

}
