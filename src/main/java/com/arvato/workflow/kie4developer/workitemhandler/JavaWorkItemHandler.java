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
import org.springframework.beans.BeansException;

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
  private final int MAX_RETRIES = 0;

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
    String className = (String) getProcessVariableOrThrowException(workItem, "className");
    String methodName = (String) getProcessVariableOrThrowException(workItem, "methodName");
    String parameterType = (String) workItem.getParameters().get("methodParameterType"); // optional
    Object parameter = workItem.getParameters().get("methodParameter"); // optional
    String errorHandingProcessId = (String) workItem.getParameters().get("errorHandingProcessId"); // optional

    if (LOGGER.isDebugEnabled()) {
      // curl -X GET "http://{host}:{port}/kie-server/services/rest/server/containers/{containerid}/processes/instances/{processinstanceid}/variable/methodParameter" -H "accept: application/json"
      LOGGER
          .debug("Try to execute Java Class {} with method {} with parameter type {} and parameter value {}.",
              className,
              methodName, parameterType, parameter);
    } else {
      LOGGER.info("Try to execute Java Class {} with method {}.", className, methodName);
    }

    try {
      Class<?> c = Class.forName(className);
      Object instance;
      try {
        instance = SpringContext.getBean(c); // try to load using spring
      } catch (BeansException e) {
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
      Map<String, Object> castedResult = (Map<String, Object>) result;
      LOGGER.info("Executing Java class {} with method {} was successful. Result is: {}.", className, methodName,
          castedResult);
      manager.completeWorkItem(workItem.getId(), castedResult);
    } catch (NoClassDefFoundError e) {
      LOGGER.error("Class {} could not be instantiated. "
          + "Please verify that no action blocks the class instantiation via default constructor.", className, e);
      handleException(workItem, manager, e, errorHandingProcessId);
    } catch (InvocationTargetException | RuntimeException e) {
      LOGGER.error(
          "Runtime Error while execute Java class {} with method {} with parameter type {} and parameter value {}.",
          className, methodName, parameterType, parameter, e);
      handleException(workItem, manager, e, errorHandingProcessId);
    } catch (ReflectiveOperationException e) {
      LOGGER.error("Class {} or method {} could not be found or instantiated. "
          + "Please verify the existence of a default constructor and method.", className, methodName, e);
      handleException(workItem, manager, e, errorHandingProcessId);
    } catch (Exception e) {
      LOGGER.error("Method {} in class {} throws an unexpected Exception.", methodName, className, e);
      handleException(workItem, manager, e, errorHandingProcessId);
    }
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
      throw new ProcessWorkItemHandlerException(errorHandingProcessId, HandlingStrategy.COMPLETE,
          cause);  // error gets handled by separate error handling subprocesses
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
