package com.arvato.workflow.kie4developer.common.interfaces;

import java.util.List;
import java.util.Map;
import org.kie.server.api.model.instance.TaskSummary;

/**
 * Interface for execution handling of process instances and human tasks
 *
 * @author TRIBE01
 */
public interface IExecutionHelper {

  /**
   * Start a new Process Instance of the given Process
   *
   * @param deploymentId the deployment id (typical equal to the process id)
   * @param processId    the process id
   * @param params       the process instance variables
   * @return the new process instance id
   */
  Long startNewProcessInstance(String deploymentId, String processId, Map<String, Object> params);

  /**
   * Abort a running Process Instance.
   *
   * @param processInstanceId the process instance id
   */
  void abortProcessInstance(Long processInstanceId);

  /**
   * Get all assigned tasks.
   *
   * @param username the username
   * @return list of tasks that can be processed
   */
  List<TaskSummary> getTasks(String username);

  /**
   * Delegate task to a new user.
   *
   * @param taskId      the task id
   * @param username    the username
   * @param newUsername the username who should get the task
   */
  void delegateTask(Long taskId, String username, String newUsername);

  /**
   * Claim a task (in status ready) for the user. The task will move to status reserved. Note that a task that only has
   * one potential actor will automatically be assigned to that actor upon task creation.
   *
   * @param taskId   the task id
   * @param username the username
   */
  void claimTask(Long taskId, String username);

  /**
   * Release a claimed task.
   *
   * @param taskId   the task id
   * @param username the username
   */
  void releaseTask(Long taskId, String username);

  /**
   * Start a task (in status reserved) for the user. The task will move to status in progress.
   *
   * @param taskId   the task id
   * @param username the username
   * @param params   the process variables
   */
  void startTask(Long taskId, String username, Map<String, Object> params);

  /**
   * Complete a task (in status in progress) by the user. The task will move to status completed.
   *
   * @param taskId   the task id
   * @param username the username
   * @param params   the process variables
   */
  void completeTask(Long taskId, String username, Map<String, Object> params);

}
