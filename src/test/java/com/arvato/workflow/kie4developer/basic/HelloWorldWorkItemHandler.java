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
package com.arvato.workflow.kie4developer.basic;

import com.arvato.workflow.kie4developer.common.interfaces.IDeployableWorkItemHandler;
import java.util.HashMap;
import java.util.Map;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple {@link IDeployableWorkItemHandler} implementation that print out some 'Hello world' text.
 * You can pass in the variable <code>helloworldtext</code> to print a custom text.
 * The handler responses with the variable <code>result</code> with the value 'Ola Hello world'.
 */
public class HelloWorldWorkItemHandler implements IDeployableWorkItemHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorldWorkItemHandler.class);
  private static final String VERSION = "1.0";

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
    LOGGER.info("start execution of {}", HelloWorldWorkItemHandler.class);
    Map<String, Object> result = new HashMap<>();

    String helloworldtext = (String) getProcessVariableOrDefault(workItem, "helloworldtext", "Hello world");
    LOGGER.info(helloworldtext);

    result.put("result", "Ola " + helloworldtext);

    manager.completeWorkItem(workItem.getId(), result);
    LOGGER.info("end execution of {}", HelloWorldWorkItemHandler.class);
  }

  @Override
  public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    // No special implementation for aborting required
    manager.abortWorkItem(workItem.getId());
  }

  /**
   * Helper to retrieve a process variable
   *
   * @param workItem     the workitem with all process variables
   * @param key          the parameter name to find
   * @param defaultValue the default value if parameter is not found or null
   * @return the parameter value or the provided default
   */
  private Object getProcessVariableOrDefault(WorkItem workItem, String key, Object defaultValue) {
    return workItem.getParameters().get(key) == null ? defaultValue : workItem.getParameters().get(key);
  }

}
