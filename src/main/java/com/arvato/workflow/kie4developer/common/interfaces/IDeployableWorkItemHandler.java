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
