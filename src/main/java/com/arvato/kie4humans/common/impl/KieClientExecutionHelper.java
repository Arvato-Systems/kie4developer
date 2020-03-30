package com.arvato.kie4humans.common.impl;

import com.arvato.kie4humans.common.interfaces.IExecutionHelper;
import com.arvato.kie4humans.common.interfaces.IRelease;
import java.util.List;
import java.util.Map;
import org.kie.server.api.model.instance.TaskSummary;
import org.springframework.stereotype.Component;

@Component
public class KieClientExecutionHelper implements IExecutionHelper {

  private IRelease release;
  private KieClient kieClient;

  public KieClientExecutionHelper(IRelease release, KieClient kieClient) {
    this.release = release;
    this.kieClient = kieClient;
  }

  @Override
  public Long startNewProcessInstance(String deploymentId, String processId, Map<String, Object> params) {
    if (!deploymentId.equals(release.getContainerId())) {
      throw new RuntimeException(String
          .format("deploymentId %s has to be equal to containerId %s for REST Client", deploymentId,
              release.getContainerId()));
    }
    if (params == null) {
      return kieClient.getProcessClient().startProcess(deploymentId, processId);
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
  public void delegateTask(Long taskId, String username, String newUsername) {
    kieClient.getTaskClient().delegateTask(release.getContainerId(), taskId, username, newUsername);
  }

  @Override
  public void claimTask(Long taskId, String username) {
    kieClient.getTaskClient().claimTask(release.getContainerId(), taskId, username);
  }

  @Override
  public void releaseTask(Long taskId, String username) {
    kieClient.getTaskClient().releaseTask(release.getContainerId(), taskId, username);
  }

  @Override
  public void startTask(Long taskId, String username, Map<String, Object> params) {
    kieClient.getTaskClient().startTask(release.getContainerId(), taskId, username);
  }

  @Override
  public void completeTask(Long taskId, String username, Map<String, Object> params) {
    kieClient.getTaskClient().completeAutoProgress(release.getContainerId(), taskId, username, params);
  }

}
