package com.arvato.workflow.kie4developer.basic;

import com.arvato.workflow.kie4developer.AbstractProcessTest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.application.autodeploy=false",
        // this has to match with the pom.xml in the kjar file (here evaluation-jbpm-module.jar)
        "spring.application.groupid=evaluation",
        "spring.application.name=evaluation",
        "spring.application.version=1.0.0",
        "spring.application.project.name=EVALUATION"
    })
public class ProcessAsXMLTest extends AbstractProcessTest {

  @Before
  public void prepare() {
    // prepare release
    clientDeploymentHelper.setProcessesToDeploy(Collections.singletonList(EvaluationProcess.class));
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
    String processId = new EvaluationProcess().getProcessId();

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
    String processId = new EvaluationProcess().getProcessId();

    // mock external services and environment variables

    // execute the process
    Map<String, Object> params = new HashMap<>();
    params.put("employee", "john");
    params.put("reason", "test on spring boot");
    Long processInstanceId = clientExecutionHelper.startNewProcessInstance(containerId, processId, params);
    Assert.assertNotNull("Process was not executed", processInstanceId);

    ProcessInstance processInstance = kieClient.getProcessClient().getProcessInstance(containerId, processInstanceId);
    Assert.assertEquals("ProcessInstance is not active anymore", org.jbpm.process.instance.ProcessInstance.STATE_ACTIVE,
        processInstance.getState().intValue());
  }

}
