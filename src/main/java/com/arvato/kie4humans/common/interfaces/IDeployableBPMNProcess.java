package com.arvato.kie4humans.common.interfaces;

import org.kie.api.io.Resource;

/**
 * Interface to define a BPMN Business Process aka JBPM Process Definition
 *
 * @author TRIBE01
 * @see IDeploymentHelper
 */
public interface IDeployableBPMNProcess {

  /**
   * Get the name of the Process
   *
   * @return the name
   */
  default String getName() {
    return this.getClass().getSimpleName();
  }

  /**
   * Get the package of the Process
   *
   * @return the package
   */
  default String getPackage() {
    return this.getClass().getPackage().getName();
  }

  /**
   * Get the version of the process
   *
   * @return the version number
   */
  String getVersion();

  /**
   * Get the unique process id for the process
   *
   * @return the process id in the form <code>package:name:version</code>
   */
  default String getProcessId() {
    return getName();
  }

  /**
   * Get the BPMN Process Model that can be used for deployment
   *
   * @return the BPM Process Definition
   */
  Resource getBPMNModel();

}
