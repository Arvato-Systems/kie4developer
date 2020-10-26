package com.arvato.workflow.kie4developer.common.impl;

import com.arvato.workflow.kie4developer.common.interfaces.IRelease;
import org.apache.maven.model.Model;
import org.kie.api.KieServices;
import org.kie.server.api.model.ReleaseId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Release implements IRelease {

  private String groupId;
  private String artifactId;
  private String version;
  private String projectName;
  private String projectDescription;
  private Boolean deploymentTargetBICCW;

  public Release(
      EffectivePomReader effectivePomReader,
      @Value("${spring.application.deploymentTargetBICCW}") Boolean deploymentTargetBICCW) {
    Model pom = effectivePomReader.getPomModel();
    this.groupId = pom.getGroupId();
    this.artifactId = pom.getArtifactId();
    this.version = pom.getVersion();
    this.projectName = pom.getName() == null ? pom.getArtifactId() : pom.getName();
    this.projectDescription = pom.getDescription();
    this.deploymentTargetBICCW = deploymentTargetBICCW;
  }

  @Override
  public String getGroupId() {
    return groupId;
  }

  @Override
  public String getArtifactId() {
    return artifactId;
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public String getContainerId() {
    return projectName + "_" + getVersion();
  }

  @Override
  public String getContainerAlias() {
    return projectName;
  }

  @Override
  public String getProjectName() {
    return projectName;
  }

  @Override
  public String getProjectDescription() {
    return projectDescription;
  }

  @Override
  public boolean isDeploymentTargetBICCW() {
    return deploymentTargetBICCW;
  }

  @Override
  public org.kie.server.api.model.ReleaseId getReleaseIdForServerAPI() {
    return new ReleaseId(getGroupId(), getArtifactId(), getVersion());
  }

  @Override
  public org.kie.api.builder.ReleaseId getReleaseIdForClientAPI() {
    return KieServices.Factory.get().newReleaseId(getGroupId(), getArtifactId(), getVersion());
  }

}
