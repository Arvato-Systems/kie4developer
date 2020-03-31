package com.arvato.workflow.kie4developer.workitemhandler;

import com.arvato.workflow.kie4developer.common.interfaces.IDeployableWorkItemHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import org.kie.api.runtime.process.ProcessWorkItemHandlerException;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link IDeployableWorkItemHandler} implementation for executing Java classes via reflection You must pass in the
 * variables <code>className</code> and <code>methodName</code> with optional <code>methodParameterType</code> and
 * <code>methodParameter</code> to invoke a custom class. The handler response with the map of results from the
 * invocation call.
 *
 * @author TRIBE01
 */
public class JavaWorkItemHandler implements IDeployableWorkItemHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(JavaWorkItemHandler.class);
  private final String VERSION = "1.0.0";

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

    LOGGER.info("Try to execute Java Class {} with method {} with parameter type {} and parameter value {}", className,
        methodName, parameterType, parameter);

    try {
      Class<?> c = Class.forName(className);
      Object instance = c.newInstance();
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
    } catch (ClassNotFoundException e) {
      LOGGER.error("Class {} could not be found", className, e);
      handleException(e);
    } catch (NoClassDefFoundError e) {
      LOGGER.error("Class {} could not be instantiated. "
          + "Please verify that no action blocks the class instantiation via default constructor", className, e);
      handleException(e);
    } catch (InstantiationException e) {
      LOGGER.error("Class {} could not be instantiated. "
          + "Please verify you defined a default constructor", className, e);
      handleException(e);
    } catch (IllegalAccessException e) {
      LOGGER.error("Method {} in class {} could not be accessed", methodName, className, e);
      handleException(e);
    } catch (NoSuchMethodException e) {
      LOGGER.error("Method {} could not be found in class {}", methodName, className, e);
      handleException(e);
    } catch (ClassCastException e) {
      LOGGER.error("Method {} in class {} must return the Type Map<String, Object>", methodName, className, e);
      handleException(e);
    } catch (InvocationTargetException e) {
      LOGGER.error("Method {} could not be found in class {}", methodName, className, e);
      handleException(e);
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
   * @param cause the exception cause
   * @see ProcessWorkItemHandlerException
   */
  private void handleException(Throwable cause) {
    throw new RuntimeException(cause);
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
      throw new IllegalArgumentException(String.format("Parameter %s not found in WorkItemHandler", key));
    }
    return parameterValue;
  }

}
