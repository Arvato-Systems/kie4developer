package com.arvato.workflow.kie4developer.common.interfaces;

/**
 * Interface to define a external lib which is required at runtime
 *
 * @author TRIBE01
 * @see IDeploymentHelper
 */
public interface IDeployableDependency {

  /**
   * Get the maven group id of the dependency
   *
   * @return the group identifier
   */
  String getMavenGroupId();

  /**
   * Get the maven artifact id of the dependency
   *
   * @return the artifact identifier
   */
  String getMavenArtifactId();

  /**
   * Get the maven version id of the dependency
   *
   * @return the version identifier
   */
  String getMavenVersionId();

}
