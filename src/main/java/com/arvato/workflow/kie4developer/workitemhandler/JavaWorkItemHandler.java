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
package com.arvato.workflow.kie4developer.workitemhandler;

import com.arvato.workflow.kie4developer.common.interfaces.IDeployableWorkItemHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import org.kie.api.runtime.process.ProcessWorkItemHandlerException;
import org.kie.api.runtime.process.ProcessWorkItemHandlerException.HandlingStrategy;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link IDeployableWorkItemHandler} implementation for executing Java classes via reflection. You must pass in the
 * variables <code>className</code> and <code>methodName</code> with optional <code>methodParameterType</code> and
 * <code>methodParameter</code> to invoke a custom class. You can pass in optional a <code>errorHandingProcessId</code>
 * to execute a exception handling process in case of an error. The handler response with the map of results from the
 * invocation call.
 *
 * @author TRIBE01
 */
public class JavaWorkItemHandler implements IDeployableWorkItemHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(JavaWorkItemHandler.class);
  private final String VERSION = "1.0.0";
  private int retries = 0;
  private final int MAX_RETRIES = (System.getProperty("spring.application.retries") == null ? 0 : Integer.parseInt(System.getProperty("spring.application.retries")));

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
    String detailErrorMsg = null;
    Map<String, Object> castedResult = null;
    String className = (String) getProcessVariableOrThrowException(workItem, "className");
    String methodName = (String) getProcessVariableOrThrowException(workItem, "methodName");
    String parameterType = (String) workItem.getParameters().get("methodParameterType"); // optional
    Object parameter = workItem.getParameters().get("methodParameter"); // optional
    String errorHandingProcessId = (String) workItem.getParameters().get("errorHandingProcessId"); // optional

    if (LOGGER.isDebugEnabled()) {
      // curl -X GET "http://{host}:{port}/kie-server/services/rest/server/containers/{containerid}/processes/instances/{processinstanceid}/variable/methodParameter" -H "accept: application/json"
      LOGGER.debug("Try to execute Java class {} with method {} with parameter type {} and parameter value {}.", className, methodName, parameterType, parameter);
    } else {
      LOGGER.info("Try to execute Java class {} with method {}.", className, methodName);
    }

    try {
      Class<?> c = Class.forName(className);
      Object instance = null;
      if (System.getProperties().containsKey("kieserver.location") && System.getProperty("kieserver.location").contains("localhost")) {
        try {
          instance = SpringContext.getBean(c); // try to load using spring dependency injection
        } catch (Exception e) {
          // ignore - fallback using java reflection below
        }
      }
      if (instance == null) {
        instance = c.getConstructor().newInstance(); // try to load using java reflection
      }
      Class<?>[] methodParameterTypes = null;
      Object[] params = null;
      if (parameter != null && parameterType != null) {
        methodParameterTypes = new Class<?>[]{Class.forName(parameterType)};
        params = new Object[]{parameter};
      }
      Method method = c.getMethod(methodName, methodParameterTypes);
      Object result = method.invoke(instance, params);
      castedResult = (Map<String, Object>) result;

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Executing Java class {} with method {} was successful. Result is: {}.", className, methodName, castedResult);
      } else {
        LOGGER.info("Executing Java class {} with method {} was successful.", className, methodName);
      }
    } catch (NoClassDefFoundError | Exception e){
      if (e.getClass().equals(NoClassDefFoundError.class)){
        detailErrorMsg = String.format("Class %s could not be instantiated. Please verify that no action blocks the class instantiation via default constructor.", className);
      } else if (e.getClass().equals(RuntimeException.class) || e.getClass().equals(InvocationTargetException.class)){
        detailErrorMsg = String.format("Runtime Error while execute Java class %s with method %s with parameter type %s and parameter value %s.", className, methodName, parameterType, parameter);
      } else if (e.getClass().equals(ReflectiveOperationException.class)){
        detailErrorMsg = String.format("Class %s or method %s could not be found or instantiated. Please verify the existence of a default constructor and method.", className, methodName);
      } else if (e.getClass().equals(Exception.class)){
        detailErrorMsg = String.format("Method %s in class %s throws an unexpected Exception.", methodName, className);
      }
      Exception detailError = new InvocationTargetException(e, detailErrorMsg);
      // wrap in a shortened error message otherwise database storage operation fails because of too much text
      String shortErrorMsg = String.format("%s#%s", className, methodName);
      Exception shortError = new InvocationTargetException(detailError, shortErrorMsg);
      handleException(workItem, manager, shortError, errorHandingProcessId);
    }

    manager.completeWorkItem(workItem.getId(), castedResult);
  }

  @Override
  public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    // No special implementation for aborting required
    manager.abortWorkItem(workItem.getId());
  }

  /**
   * Handle execution errors
   *
   * @param workItem              the workItem reference
   * @param manager               the manager reference
   * @param cause                 the exception cause
   * @param errorHandingProcessId the process id of a separate error handling subprocesses to start (optional)
   * @see ProcessWorkItemHandlerException
   */
  public void handleException(WorkItem workItem, WorkItemManager manager,
      Throwable cause, String errorHandingProcessId) {
    if (retries < MAX_RETRIES) {
      try {
        // increase time on every retry: 11min, 121min, 1331min ...
        Thread.sleep(Double.doubleToLongBits(Math.pow(11, retries)) * 60 * 1000);
      } catch (InterruptedException e) {
      }
      retries++;
      LOGGER.info("Retry execution #{}/{}.", retries, MAX_RETRIES);
      executeWorkItem(workItem, manager);
    } else if (errorHandingProcessId != null) {
      LOGGER.info("Starting error handling subprocess {}.", errorHandingProcessId);
      throw new ProcessWorkItemHandlerException(errorHandingProcessId, HandlingStrategy.COMPLETE, cause); // error gets handled by separate error handling subprocesses
    } else {
      throw new RuntimeException(cause);
    }
  }

  /**
   * Helper to retrieve a process variable.
   *
   * @param workItem the workitem with all process variables
   * @param key      the parameter name to find
   * @return the parameter value or the provided default
   * @throws IllegalArgumentException if the parameter could not be found
   */
  private Object getProcessVariableOrThrowException(WorkItem workItem, String key) throws IllegalArgumentException {
    Object parameterValue = workItem.getParameters().get(key);
    if (parameterValue == null) {
      throw new IllegalArgumentException(String.format("Parameter %s not found in WorkItemHandler.", key));
    }
    return parameterValue;
  }

}
