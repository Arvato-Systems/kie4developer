package org.kie.server.springboot.samples.client;

import java.util.List;
import java.util.Map;

import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.springboot.samples.common.interfaces.IExecutionHelper;
import org.kie.server.springboot.samples.common.interfaces.IRelease;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KieClientExecutionHelper implements IExecutionHelper<TaskSummary> {

	@Autowired
	private IRelease release;
	@Autowired
	private KieClient kieClient;

	@Override
	public Long startNewProcessInstance(String deploymentId, String processId, Map<String, Object> params) {
		// attention: deploymentId has to be the containerId for deployment via REST Client
		return kieClient.getProcessClient().startProcess(deploymentId, processId, params);
	}

	@Override
	public void abortProcessInstance(Long processInstanceId) {
		kieClient.getProcessClient().abortProcessInstance(release.getContainerId(), processInstanceId);
	}

	@Override
	public List<TaskSummary> getTasks(String username) {
		return kieClient.getTaskClient().findTasksAssignedAsPotentialOwner(username, 0, Integer.MAX_VALUE);
	}

	@Override
	public void claimTask(Long taskId, String username) {
		kieClient.getTaskClient().claimTask(release.getContainerId(), taskId, username);
	}

	@Override
	public void completeTask(Long taskId, String username, Map<String, Object> params) {
		kieClient.getTaskClient().completeAutoProgress(release.getContainerId(), taskId, username, params);
	}

}
