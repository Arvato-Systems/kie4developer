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
package com.arvato.workflow.kie4developer.common.interfaces;

import org.jbpm.ruleflow.core.RuleFlowProcessFactory;
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
   * Build the BPMN Process Model via fluent API
   *
   * @param builder the BPM Process builder
   */
  void buildBPMNModel(RuleFlowProcessFactory builder);

  /**
   * Build the BPMN Process Model via .bpmn file
   *
   * @return the BPMN Process Model as XML file Resource
   */
  default Resource buildBPMNModel() { return null; }

  /**
   * Build the BPMN Process Model Image
   *
   * @return the BPMN Process Model as SVG file Resource
   */
  default Resource buildBPMNModelImage() { return null; }

}
