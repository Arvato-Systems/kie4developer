package com.arvato.workflow.kie4developer.common.interfaces;

/**
 * Interface to define a Release aka JBPM DeploymentUnit
 *
 * @author TRIBE01
 * @see IDeploymentHelper
 */
public interface IRelease {

  /**
   * Get the group id for the release
   *
   * @return the group id
   */
  String getGroupId();

  /**
   * Get the artifact id for the release
   *
   * @return the artifact id
   */
  String getArtifactId();

  /**
   * Get the version for the release
   *
   * @return the version number
   */
  String getVersion();

  /**
   * Get the container Id
   *
   * @return the container id
   */
  String getContainerId();

  /**
   * Get the container alias
   *
   * @return the container alias
   */
  String getContainerAlias();

  /**
   * Get the project name for the release
   *
   * @return the project name
   */
  String getProjectName();

  /**
   * Get the project description for the release
   *
   * @return the project description
   */
  String getProjectDescription();

  /**
   * Check if the deployment target application is BICCW (true) and not plain KIE Server (false) if distributed to BICCW
   * required listeners are added to the deployment descriptor
   *
   * @return true if deployment target is BICCW
   */
  boolean isDeploymentTargetBICCW();

  /**
   * Check if the BICCW specific ImprovedBicceProcessInstanceListener should be included (true) or not (false)
   *
   * @return true if ImprovedBicceProcessInstanceListener should be included
   */
  boolean isIncludeProcessInstanceListener();

  /**
   * Check if the BICCW specific BicceTaskEmailEventListener should be included (true) or not (false)
   *
   * @return true if BicceTaskEmailEventListener should be included
   */
  boolean isIncludeTaskEmailEventListener();

  /**
   * Check if the BICCW specific ImprovedBicceTaskEventListener should be included (true) or not (false)
   *
   * @return true if ImprovedBicceTaskEventListener should be included
   */
  boolean isIncludeTaskEventListener();

  /**
   * Get the unique deployment id for the release.
   *
   * @return the deployment id in the form <code>package:name:version</code>
   */
  default String getDeploymentId() {
    return getGroupId() + ":" + getArtifactId() + ":" + getVersion();
  }

  /**
   * Get the release id
   *
   * @return the release id for the kie server
   */
  org.kie.server.api.model.ReleaseId getReleaseIdForServerAPI();

  /**
   * Get the release id
   *
   * @return the release id for the kie client lib
   */
  org.kie.api.builder.ReleaseId getReleaseIdForClientAPI();

}
