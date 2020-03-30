package com.arvato.kie4humans.common.interfaces;

import java.util.List;
import org.kie.api.runtime.process.WorkItemHandler;

/**
 * Interface to define a {@link WorkItemHandler} aka JBPM Workitemhandler
 *
 * @author TRIBE01
 * @see IDeploymentHelper
 */
public interface IDeployableWorkItemHandler extends WorkItemHandler {

  /**
   * Get the name of the workitemhandler
   *
   * @return the name
   */
  default String getName() {
    return this.getClass().getSimpleName();
  }

  /**
   * Get the package of the workitemhandler
   *
   * @return the package
   */
  default String getPackage() {
    return this.getClass().getPackage().getName();
  }

  /**
   * Get the version of the workitemhandler
   *
   * @return the version number
   */
  String getVersion();

}
