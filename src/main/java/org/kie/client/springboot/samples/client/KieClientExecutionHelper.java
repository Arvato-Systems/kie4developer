package org.kie.client.springboot.samples.client;

import java.util.List;
import java.util.Map;

import org.kie.client.springboot.samples.client.kjar.KJarBuilder;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.client.springboot.samples.common.interfaces.IExecutionHelper;
import org.kie.client.springboot.samples.common.interfaces.IRelease;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KieClientExecutionHelper implements IExecutionHelper<TaskSummary> {

	private static final Logger LOGGER = LoggerFactory.getLogger(KieClientExecutionHelper.class);
	@Autowired
	private IRelease release;
	@Autowired
	private KieClient kieClient;

	@Override
	public Long startNewProcessInstance(String deploymentId, String processId, Map<String, Object> params) {
		if (!deploymentId.equals(release.getContainerId())){
			throw new RuntimeException("deploymentId '" + deploymentId + "' has to be equals to containerId '"+release.getContainerId()+"' for deployment via REST Client!");
		}
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
