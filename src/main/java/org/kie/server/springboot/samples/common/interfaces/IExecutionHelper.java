package org.kie.server.springboot.samples.common.interfaces;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Interface for process instance and task execution
 * @param <TaskSummery> the TaskSummery Implementation
 * @author TRIBE01
 */
@Component
public interface IExecutionHelper<TaskSummery> {

	/**
	 * Start a new Process Instance of the given Process
	 * @param processId the deployment id (typical equal to the process id)
	 * @param processId the process id
	 * @param params the process instance variables
	 * @return the new process instance id
	 */
	Long startNewProcessInstance(String deploymentId, String processId, Map<String, Object> params);

	/**
	 * Abort a running Process Instance
	 * @param processInstanceId the process instance id
	 */
	void abortProcessInstance(Long processInstanceId);

	/**
	 * Get all assigned tasks
	 * @param username the username
	 * @return list of tasks that can be processed
	 */
	List<TaskSummery> getTasks(String username);

	/**
	 * Claim a task for the user
	 * @param taskId the task id
	 * @param username the username
	 */
	void claimTask(Long taskId, String username);

	/**
	 * Complete a task by the user
	 * @param taskId the task id
	 * @param username the username
	 * @param params the process variables
	 */
	void completeTask(Long taskId, String username, Map<String, Object> params);

}
