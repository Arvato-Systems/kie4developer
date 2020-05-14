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
 * <code>methodParameter</code> to invoke a custom class. The handler response with the map of results from the
 * invocation call.
 *
 * @author TRIBE01
 */
public class JavaWorkItemHandler implements IDeployableWorkItemHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(JavaWorkItemHandler.class);
  private final String VERSION = "1.0.0";
  private int retries = 0;
  private final int MAX_RETRIES = 0;

  /**
   * Supported strategies: COMPLETE - it completes the workitemhandler task with the variables from the completed
   * replacement subprocess instance (processid in ProcessWorkItemHandlerException instance) - these variables will be
   * given to the workitemhandler task as output of the service interaction and thus mapped to main process instance
   * variables. ABORT - it aborts the workitemhandler task and moves on the process without setting any variables. RETRY
   * - it retries the workitemhandler task logic with variables from both the original workitemhandler task parameters
   * and the variables from replacement subprocess instance - variables from replacement subprocess instance overrides
   * any variables of the same name. RETHROW - it simply throws the error back to the caller - this strategy should not
   * be used with wait state replacement subprocesses as it will simply rollback the transaction and thus the completion
   * of the subprocess instance.
   */
  private static final HandlingStrategy STRATEGY = HandlingStrategy.RETHROW;

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
    String errorhandingprocessId = (String) workItem.getParameters().get("errorhandingprocessId"); // optional

    if (LOGGER.isDebugEnabled()) {
      LOGGER
          .debug("Try to execute Java Class {} with method {} with parameter type {} and parameter value {}", className,
              methodName, parameterType, parameter);
    } else {
      LOGGER.info("Try to execute Java Class {} with method {}", className, methodName);
    }

    try {
      Class<?> c = Class.forName(className);
      Object instance = c.getConstructor().newInstance();
      Class<?>[] methodParameterTypes = null;
      Object[] params = null;
      if (parameter != null && parameterType != null) {
        methodParameterTypes = new Class<?>[]{Class.forName(parameterType)};
        params = new Object[]{parameter};
      }
      Method method = c.getMethod(methodName, methodParameterTypes);
      Object result = method.invoke(instance, params);
      Map<String, Object> castedResult = (Map<String, Object>) result;
      LOGGER.info("Executing Java class {} with method {} was successful. Result is: {}", className, methodName,
          castedResult);
      manager.completeWorkItem(workItem.getId(), castedResult);
    } catch (NoClassDefFoundError e) {
      LOGGER.error("Class {} could not be instantiated. "
          + "Please verify that no action blocks the class instantiation via default constructor", className, e);
      handleException(errorhandingprocessId, workItem, manager, e);
    } catch (InvocationTargetException | RuntimeException e) {
      LOGGER.error(
          "Runtime Error while execute Java class {} with method {} with parameter type {} and parameter value {}.",
          className, methodName, parameterType, parameter, e);
      handleException(errorhandingprocessId, workItem, manager, e);
    } catch (ReflectiveOperationException e) {
      LOGGER.error("Class {} or method {} could not be found or instantiated. "
          + "Please verify the existence of a default constructor and method.", className, methodName, e);
      handleException(errorhandingprocessId, workItem, manager, e);
    } catch (Exception e) {
      LOGGER.error("Method {} in class {} throws an unexpected Exception.", methodName, className, e);
      handleException(errorhandingprocessId, workItem, manager, e);
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
   * @param errorhandingprocessId the process id of a separate error handling subprocesses
   * @param workItem              the workItem reference
   * @param manager               the manager reference
   * @param cause                 the exception cause
   * @see ProcessWorkItemHandlerException
   */
  public void handleException(String errorhandingprocessId, WorkItem workItem, WorkItemManager manager,
      Throwable cause) {
    if (retries < MAX_RETRIES) {
      retries++;
      LOGGER.info("Retry execution #{}/{}.", retries, MAX_RETRIES);
      executeWorkItem(workItem, manager);
    } else if (errorhandingprocessId != null) {
      LOGGER.info("Starting error handling subprocess {}.", errorhandingprocessId);
      throw new ProcessWorkItemHandlerException(errorhandingprocessId, HandlingStrategy.COMPLETE,
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
