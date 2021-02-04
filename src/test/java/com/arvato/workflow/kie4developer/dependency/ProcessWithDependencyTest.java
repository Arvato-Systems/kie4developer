package com.arvato.workflow.kie4developer.dependency;

import com.arvato.workflow.kie4developer.AbstractProcessTest;
import com.arvato.workflow.kie4developer.common.interfaces.IDeployableDependency;
import com.arvato.workflow.kie4developer.workitemhandler.JavaWorkItemHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.api.model.definition.ProcessDefinition;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.TaskInstance;
import org.kie.server.api.model.instance.TaskSummary;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.application.autodeploy=false"
    })
public class ProcessWithDependencyTest extends AbstractProcessTest {

  private final String BATIK_VERSION = "1.13";

  @Before
  public void prepare() {
    // prepare release
    clientDeploymentHelper.setProcessesToDeploy(Collections.singletonList(DependencyProcess.class));
    clientDeploymentHelper.setWorkItemHandlersToDeploy(Collections.singletonList(JavaWorkItemHandler.class));
    clientDeploymentHelper.setServiceClassesToDeploy(Collections.singletonList(DependencyService.class));
    clientDeploymentHelper.setDependenciesToDeploy(Collections.singletonList(new IDeployableDependency() {
      @Override
      public String getMavenGroupId() {
        return "org.apache.xmlgraphics";
      }

      @Override
      public String getMavenArtifactId() {
        return "batik-all";
      }

      @Override
      public String getMavenVersionId() {
        return BATIK_VERSION;
      }
    }));
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
    String processId = new DependencyProcess().getProcessId();

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
    String processId = new DependencyProcess().getProcessId();

    // mock external services and environment variables

    // execute the process
    Long processInstanceId = clientExecutionHelper.startNewProcessInstance(containerId, processId, null);
    Assert.assertNotNull("Process was not executed", processInstanceId);

    // complete the human task
    TaskSummary task = clientExecutionHelper.getTasks("john").get(0);
    TaskInstance taskInstance = kieClient.getTaskClient()
        .getTaskInstance(containerId, task.getId(), true, false, false);
    String taskInput = (String) taskInstance.getInputData().get("taskInput");
    String taskOutput = taskInput + " modified by task";
    Map<String, Object> params = new HashMap<>();
    params.put("taskOutput", taskOutput);
    clientExecutionHelper.completeTask(task.getId(), "john", params);

    // verify that the process instance contains the expected variable values
    List<ProcessInstance> completedInstances = getCompletedProcessesInstancesByProcessId(processId);
    Assert.assertEquals("Process Instance was not completed", 1, completedInstances.size());
    for (ProcessInstance instance : completedInstances) {
      // instance.getVariables() returns null for completed instances, so we have to request the variables separately from audit history
      Map<String, Object> variables = getProcessInstanceVariables(instance.getId());
      // unluckily the returned variables are all Strings - more precise the .toString() representation of the variables.
      // Therefore cast to CentralCustomerDatasetDto is not possible. When a Instance is completed there is no way to get the real value again.
      // To make this more worse the audit log values are limited to 255 chars by default ("org.jbpm.var.log.length").
      String result = (String) variables.get("batikVersion");
      Assert.assertTrue("Batik version not found",result.endsWith(" modified by service modified by task"));
      Assert.assertEquals("Batik version not found", BATIK_VERSION,result.split(" modified by service modified by task")[0]);
    }
  }


}
