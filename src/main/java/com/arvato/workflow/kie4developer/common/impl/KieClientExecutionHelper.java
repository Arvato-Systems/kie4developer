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
package com.arvato.workflow.kie4developer.common.impl;

import com.arvato.workflow.kie4developer.common.interfaces.IExecutionHelper;
import com.arvato.workflow.kie4developer.common.interfaces.IRelease;
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

  @Override
  public void sendSignal(Long processInstanceId, String signalName, Object event) {
    kieClient.getProcessClient().signalProcessInstance(release.getContainerId(), processInstanceId, signalName, event);
  }
}
