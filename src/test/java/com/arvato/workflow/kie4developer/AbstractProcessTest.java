package com.arvato.workflow.kie4developer;

import com.arvato.workflow.kie4developer.common.impl.KieClient;
import com.arvato.workflow.kie4developer.common.interfaces.IDeploymentHelper;
import com.arvato.workflow.kie4developer.common.interfaces.IExecutionHelper;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceList;
import org.kie.server.api.model.definition.ProcessDefinition;
import org.kie.server.api.model.instance.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Common Testing class for Process tests
 */
public abstract class AbstractProcessTest {

  @Autowired
  protected IDeploymentHelper clientDeploymentHelper;
  @Autowired
  protected IExecutionHelper clientExecutionHelper;
  @Autowired
  protected KieClient kieClient;

  /**
   * Get all KIE containers
   *
   * @return list of containers or <code>null</code>
   */
  protected KieContainerResourceList getAllContainers() {
    return kieClient.getKieServicesClient().listContainers().getResult();
  }

  /**
   * Get KIE containers with provided id
   *
   * @param containerId the container to search for
   * @return the found container or <code>null</code>
   */
  protected KieContainerResource getContainer(String containerId) {
    return kieClient.getKieServicesClient().getContainerInfo(containerId).getResult();
  }

  /**
   * Get all process definitions inside a given container
   *
   * @param containerId the container to search in
   * @return the list of process definitions or <code>null</code>
   */
  protected List<ProcessDefinition> getProcessDefinitions(String containerId) {
    return kieClient.getQueryClient().findProcessesByContainerId(containerId, 0, Integer.MAX_VALUE);
  }

  /**
   * Get process definition inside a given container
   *
   * @param containerId the container to search in
   * @param processId   the process id to search for
   * @return the found process definition or <code>null</code>
   */
  protected ProcessDefinition getProcessDefinition(String containerId, String processId) {
    return kieClient.getProcessClient().getProcessDefinition(containerId, processId);
  }

  /**
   * Get active process instances inside a given container
   *
   * @param containerId the container to search in
   * @return the found process instances or <code>null</code>
   */
  protected List<ProcessInstance> getActiveProcessesInstancesByContainerId(String containerId) {
    List<Integer> status = Arrays.asList(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE);
    return kieClient.getQueryClient().findProcessInstancesByContainerId(containerId, status, 0, Integer.MAX_VALUE);
  }

  /**
   * Get active process instances inside a given container
   *
   * @param processId the process id to search for
   * @return the found process instances or <code>null</code>
   */
  protected List<ProcessInstance> getActiveProcessesInstancesByProcessId(String processId) {
    List<Integer> status = Arrays.asList(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE);
    return kieClient.getQueryClient().findProcessInstancesByProcessId(processId, status, 0, Integer.MAX_VALUE);
  }

  /**
   * Get completed process instances inside a given container Note: completed process instances doesn't contain
   * variables
   *
   * @param processId the process id to search for
   * @return the found process instances or <code>null</code>
   */
  protected List<ProcessInstance> getCompletedProcessesInstancesByProcessId(String processId) {
    List<Integer> status = Arrays.asList(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED);
    return kieClient.getQueryClient().findProcessInstancesByProcessId(processId, status, 0, Integer.MAX_VALUE);
  }

  /**
   * Get variables of a process instance Note that if the instance is already in status completed the returned variables
   * are just the <code>toString()</code> representation of the variables.
   *
   * @param instanceId the process instance id to search for
   * @return the found process instance variables or <code>null</code>
   */
  protected Map<String, Object> getProcessInstanceVariables(Long instanceId) {
    return kieClient.getQueryClient().findProcessInstanceById(instanceId, true).getVariables();
  }

}
