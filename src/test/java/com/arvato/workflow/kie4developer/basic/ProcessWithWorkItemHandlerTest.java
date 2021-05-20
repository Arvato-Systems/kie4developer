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
package com.arvato.workflow.kie4developer.basic;

import static org.mockito.Mockito.when;

import com.arvato.workflow.kie4developer.AbstractProcessTest;
import com.arvato.workflow.kie4developer.common.interfaces.IRelease;
import com.arvato.workflow.kie4developer.migration.BrokenExampleService;
import com.arvato.workflow.kie4developer.migration.FixedExampleService;
import com.arvato.workflow.kie4developer.workitemhandler.JavaWorkItemHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jbpm.process.instance.ProcessInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.server.api.exception.KieServicesHttpException;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.api.model.admin.ExecutionErrorInstance;
import org.kie.server.api.model.admin.MigrationReportInstance;
import org.kie.server.api.model.definition.ProcessDefinition;
import org.kie.server.api.model.instance.TaskSummary;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.application.autodeploy=false"
    })
public class ProcessWithWorkItemHandlerTest extends AbstractProcessTest {

  @SpyBean
  private IRelease mockRelease;

  @Before
  public void prepare() {
    // prepare release
    clientDeploymentHelper.setProcessesToDeploy(Collections.singletonList(SupportTicketProcess.class));
    clientDeploymentHelper.setWorkItemHandlersToDeploy(Collections.singletonList(JavaWorkItemHandler.class));
    clientDeploymentHelper.setServiceClassesToDeploy(Collections.singletonList(BrokenExampleService.class));
    clientDeploymentHelper.setDependenciesToDeploy(new ArrayList<>());
    // deploy release
    clientDeploymentHelper.deploy(true);
  }

  @After
  public void dispose() {
    clientDeploymentHelper.undeploy(true);
  }

  @Test
  public void testValidBPMN() {
    String containerId = clientDeploymentHelper.getRelease().getContainerId();
    String processId = new SupportTicketProcess().getProcessId();

    // verify that the release container is running
    KieContainerResource container = getContainer(containerId);
    Assert.assertEquals("Container was not started", KieContainerStatus.STARTED, container.getStatus());

    // verify that the process is available inside container
    ProcessDefinition processDefinition = getProcessDefinition(containerId, processId);
    Assert.assertEquals("Process Definition was not found", processId, processDefinition.getId());
  }

  @Test
  public void testExecution() {
    String containerId = clientDeploymentHelper.getRelease().getContainerId();
    String processId = new SupportTicketProcess().getProcessId();

    // verify that the release container is running
    KieContainerResource container = getContainer(containerId);
    Assert.assertEquals("Container was not started", KieContainerStatus.STARTED, container.getStatus());

    // verify that the process is available inside container
    ProcessDefinition processDefinition = getProcessDefinition(containerId, processId);
    Assert.assertEquals("Process Definition was not found", processId, processDefinition.getId());

    // execute the process
    Map<String, Object> params = new HashMap<>();
    params.put("className", BrokenExampleService.class.getName());
    params.put("methodName", "doIt");
    params.put("methodParameterType", "java.util.Map");
    Map<String, Object> methodParameter = new HashMap<>();
    methodParameter.put("someKey", "someValue");
    methodParameter.put("anotherKey", "anotherValue");
    params.put("methodParameter", methodParameter);

    Long processInstanceId = clientExecutionHelper.startNewProcessInstance(containerId, processId, params);
    Assert.assertNotNull("Process was not executed", processInstanceId);

    // proceed human task
    List<TaskSummary> johnsTasks = clientExecutionHelper.getTasks("john");
    Assert.assertEquals("Task is missing", 1, johnsTasks.size());
    //clientExecutionHelper.claimTask(johnsTasks.get(0).getId(), "john"); // claiming the task isn't possible, since the task is already assigned to one specific user
    //clientExecutionHelper.startTask(johnsTasks.get(0).getId(), "john", null); // start task work is optional
    clientExecutionHelper.completeTask(johnsTasks.get(0).getId(), "john", null);
    johnsTasks = clientExecutionHelper.getTasks("john");
    Assert.assertEquals("Task was not completed", 1, johnsTasks.size());
    clientExecutionHelper.completeTask(johnsTasks.get(0).getId(), "john", null);

    johnsTasks = clientExecutionHelper.getTasks("john");
    Assert.assertEquals("Task was not completed", 0, johnsTasks.size());
  }

  @Test
  public void testWorkItemHandlerExceptionHandlingWithVariableCorrection() {
    String containerId = clientDeploymentHelper.getRelease().getContainerId();
    String processId = new SupportTicketProcess().getProcessId();
    Long processInstanceId = null;

    // verify that the release container is running
    KieContainerResource container = getContainer(containerId);
    Assert.assertEquals("Container was not started", KieContainerStatus.STARTED, container.getStatus());

    // verify that the process is available inside container
    ProcessDefinition processDefinition = getProcessDefinition(containerId, processId);
    Assert.assertEquals("Process Definition was not found", processId, processDefinition.getId());

    // execute the process
    Map<String, Object> params = new HashMap<>();
    params.put("className", BrokenExampleService.class.getName());
    params.put("methodName", "doIt");
    params.put("methodParameterType", "java.util.Map");
    Map<String, Object> methodParameter = new HashMap<>();
    methodParameter.put("someKey", "someValue");
    methodParameter.put("anotherKey", "anotherValue");
    methodParameter.put("shouldFail", "true");
    params.put("methodParameter", methodParameter);

    List<ExecutionErrorInstance> executionErrorInstancesBefore = kieClient.getProcessAdminClient()
        .getErrors(containerId, true, 0, Integer.MAX_VALUE);
    try {
      processInstanceId = clientExecutionHelper.startNewProcessInstance(containerId, processId, params);
      TaskSummary task = clientExecutionHelper.getTasks("john").get(0);
      clientExecutionHelper.completeTask(task.getId(), "john", null);
      Assert.fail("Expected exception missing");
    } catch (KieServicesHttpException e) {
      List<ExecutionErrorInstance> executionErrorInstances = kieClient.getProcessAdminClient()
          .getErrors(containerId, true, 0, Integer.MAX_VALUE);
      Assert.assertEquals("Expected processinstance error not found", executionErrorInstancesBefore.size() + 1,
          executionErrorInstances.size());
      for (ExecutionErrorInstance executionErrorInstance : executionErrorInstances) {
        Assert
            .assertEquals("Processname was not found ", "SupportTicketProcess", executionErrorInstance.getProcessId());
        Assert.assertEquals("Activityname was not found ", "execute business logic",
            executionErrorInstance.getActivityName());
        Assert.assertNotNull("ProcessInstanceId was not found", executionErrorInstance.getProcessInstanceId());
        Assert.assertEquals("[SupportTicketProcess:" + executionErrorInstance.getProcessInstanceId()
                + " - execute business logic:5] -- java.lang.reflect.InvocationTargetException: com.arvato.workflow.kie4developer.migration.BrokenExampleService#doIt",
            executionErrorInstance.getErrorMessage());

        ExecutionErrorInstance executionErrorInstanceDetails = kieClient.getProcessAdminClient()
            .getError(containerId, executionErrorInstance.getErrorId());
        Assert.assertTrue("Stacktrace with Exception was not found",
            executionErrorInstanceDetails.getError()
                .contains("Caused by: java.lang.RuntimeException: error while executing business logic"));
        Assert.assertTrue("Stacktrace with Exception was not found",
            executionErrorInstanceDetails.getError().contains(BrokenExampleService.class.getSimpleName() + ".java"));
      }
      // correct the variable and retry
      Map<String, Object> newMethodParameter = new HashMap<>();
      newMethodParameter.put("someKey", "changedValue");
      newMethodParameter.put("anotherKey", "anotherChangedValue");
      newMethodParameter.put("shouldFail", "false");
      kieClient.getProcessClient()
          .setProcessVariable(containerId, processInstanceId, "methodParameter", newMethodParameter);
      TaskSummary task = clientExecutionHelper.getTasks("john").get(0);
      clientExecutionHelper.completeTask(task.getId(), "john", null);
      task = clientExecutionHelper.getTasks("john").get(0);
      clientExecutionHelper.completeTask(task.getId(), "john", null);
      Assert.assertEquals("Tasks were not completed successful", 0, clientExecutionHelper.getTasks("john").size());
      Assert.assertEquals("Process Instance was not completed successful", ProcessInstance.STATE_COMPLETED,
          kieClient.getProcessClient().getProcessInstance(containerId, processInstanceId).getState().intValue());
    }
  }

  @Test
  public void testWorkItemHandlerExceptionHandlingWithWorkItemHandlerCorrection() {
    String containerId = clientDeploymentHelper.getRelease().getContainerId();
    String processId = new SupportTicketProcess().getProcessId();
    Long processInstanceId = null;

    // verify that the release container is running
    KieContainerResource container = getContainer(containerId);
    Assert.assertEquals("Container was not started", KieContainerStatus.STARTED, container.getStatus());

    // verify that the process is available inside container
    ProcessDefinition processDefinition = getProcessDefinition(containerId, processId);
    Assert.assertEquals("Process Definition was not found", processId, processDefinition.getId());

    // execute the process
    Map<String, Object> params = new HashMap<>();
    params.put("className", BrokenExampleService.class.getName());
    params.put("methodName", "doIt");
    params.put("methodParameterType", "java.util.Map");
    Map<String, Object> methodParameter = new HashMap<>();
    methodParameter.put("someKey", "someValue");
    methodParameter.put("anotherKey", "anotherValue");
    methodParameter.put("shouldFail", "true");
    params.put("methodParameter", methodParameter);

    List<ExecutionErrorInstance> executionErrorInstancesBefore = kieClient.getProcessAdminClient()
        .getErrors(containerId, true, 0, Integer.MAX_VALUE);
    try {
      processInstanceId = clientExecutionHelper.startNewProcessInstance(containerId, processId, params);
      TaskSummary task = clientExecutionHelper.getTasks("john").get(0);
      clientExecutionHelper.completeTask(task.getId(), "john", null);
      Assert.fail("Expected exception missing");
    } catch (KieServicesHttpException e) {
      List<ExecutionErrorInstance> executionErrorInstances = kieClient.getProcessAdminClient()
          .getErrors(containerId, true, 0, Integer.MAX_VALUE);
      Assert.assertEquals("Expected processinstance error not found", executionErrorInstancesBefore.size() + 1,
          executionErrorInstances.size());
      for (ExecutionErrorInstance executionErrorInstance : executionErrorInstances) {
        Assert
            .assertEquals("Processname was not found ", "SupportTicketProcess", executionErrorInstance.getProcessId());
        Assert.assertEquals("Activityname was not found ", "execute business logic",
            executionErrorInstance.getActivityName());
        Assert.assertNotNull("ProcessInstanceId was not found", executionErrorInstance.getProcessInstanceId());
        Assert.assertEquals("[SupportTicketProcess:" + executionErrorInstance.getProcessInstanceId()
                + " - execute business logic:5] -- java.lang.reflect.InvocationTargetException: com.arvato.workflow.kie4developer.migration.BrokenExampleService#doIt",
            executionErrorInstance.getErrorMessage());

        ExecutionErrorInstance executionErrorInstanceDetails = kieClient.getProcessAdminClient()
            .getError(containerId, executionErrorInstance.getErrorId());
        Assert.assertTrue("Stacktrace with Exception was not found",
            executionErrorInstanceDetails.getError()
                .contains("Caused by: java.lang.RuntimeException: error while executing business logic"));
        Assert.assertTrue("Stacktrace with Exception was not found",
            executionErrorInstanceDetails.getError().contains(BrokenExampleService.class.getSimpleName() + ".java"));
      }
      // deploy a new release with fixed EmailService and migrate all running processes to the new version.
      prepareDeployment2(containerId);
      containerId = clientDeploymentHelper.getRelease().getContainerId();

      // update the processinstance variable "className" to use the new Service impl.
      kieClient.getProcessClient()
          .setProcessVariable(containerId, processInstanceId, "className", FixedExampleService.class.getName());

      // rerun on new version
      TaskSummary task = clientExecutionHelper.getTasks("john").get(0);
      clientExecutionHelper.completeTask(task.getId(), "john", null);
      task = clientExecutionHelper.getTasks("john").get(0);
      clientExecutionHelper.completeTask(task.getId(), "john", null);
      Assert.assertEquals("Tasks were not completed successful", 0, clientExecutionHelper.getTasks("john").size());
      Assert.assertEquals("Process Instance was not completed successful", ProcessInstance.STATE_COMPLETED,
          kieClient.getProcessClient().getProcessInstance(containerId, processInstanceId).getState().intValue());
    }
  }

  public void prepareDeployment2(String oldContainerId) {
    when(mockRelease.getVersion()).thenReturn("1.0.1");  // fake new release version

    // prepare release
    clientDeploymentHelper.setProcessesToDeploy(Collections.singletonList(SupportTicketProcess.class));
    clientDeploymentHelper.setWorkItemHandlersToDeploy(Collections.singletonList(JavaWorkItemHandler.class));
    clientDeploymentHelper.setServiceClassesToDeploy(Collections.singletonList(FixedExampleService.class));

    // deploy release
    List<MigrationReportInstance> migrationReport = clientDeploymentHelper.deployWithMigration(oldContainerId);
    Assert.assertTrue("Migration was not successful", migrationReport.get(0).isSuccessful());
  }

}
