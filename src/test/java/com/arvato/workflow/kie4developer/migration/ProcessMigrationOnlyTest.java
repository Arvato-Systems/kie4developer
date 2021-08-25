package com.arvato.workflow.kie4developer.migration;

import static org.mockito.Mockito.when;

import com.arvato.workflow.kie4developer.AbstractProcessTest;
import com.arvato.workflow.kie4developer.basic.HelloWorldProcess;
import com.arvato.workflow.kie4developer.basic.HelloWorldWorkItemHandler;
import com.arvato.workflow.kie4developer.common.interfaces.IRelease;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerStatus;
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
        "spring.application.autodeploy=false",
        "spring.application.automigrate=false",
        "global.test=Bonjour"
    })
public class ProcessMigrationOnlyTest extends AbstractProcessTest {

  @SpyBean
  private IRelease mockRelease;

  @After
  public void dispose() {
    clientDeploymentHelper.undeploy(true);
  }

  private void prepareDeployment1() {
    // prepare release
    clientDeploymentHelper.setProcessesToDeploy(Collections.singletonList(HelloWorldProcess.class));
    clientDeploymentHelper.setWorkItemHandlersToDeploy(Collections.singletonList(HelloWorldWorkItemHandler.class));
    clientDeploymentHelper.setDependenciesToDeploy(new ArrayList<>());
    // deploy release
    clientDeploymentHelper.deploy(false);
  }

  private void prepareDeployment2() {
    // prepare release
    clientDeploymentHelper.setProcessesToDeploy(Collections.singletonList(HelloWorldProcessV2.class));
    clientDeploymentHelper.setWorkItemHandlersToDeploy(Collections.singletonList(HelloWorldWorkItemHandler.class));
    clientDeploymentHelper.setDependenciesToDeploy(new ArrayList<>());
    // deploy release
    clientDeploymentHelper.deploy(false);
  }

  private void migrate(String oldContainerId) {
    // prepare release
    clientDeploymentHelper.setProcessesToDeploy(Collections.singletonList(HelloWorldProcessV2.class));
    // deploy release
    List<MigrationReportInstance> migrationReport = clientDeploymentHelper.migrate(oldContainerId);
    Assert.assertTrue("Migration was not successful", migrationReport.get(0).isSuccessful());
  }

  @Test
  public void testMigratingDeployment() {
    prepareDeployment1();
    String containerId = clientDeploymentHelper.getRelease().getContainerId();
    String processId = new HelloWorldProcess().getProcessId();

    // verify that the release container is running
    KieContainerResource container = getContainer(containerId);
    Assert.assertEquals("Container was not started", KieContainerStatus.STARTED, container.getStatus());

    // verify that the process is available inside container
    ProcessDefinition processDefinition = getProcessDefinition(containerId, processId);
    Assert.assertEquals("Process Definition was not found", processId, processDefinition.getId());

    // execute the process
    Map<String, Object> params = new HashMap<>();
    params.put("employee", "john");
    params.put("reason", "test on spring boot");

    Long processInstanceId = clientExecutionHelper.startNewProcessInstance(containerId, processId, params);
    Assert.assertNotNull("Process was not executed", processInstanceId);

    // deploy version 2 for parallel execution
    when(mockRelease.getVersion()).thenReturn("1.0.1");  // fake new release version
    prepareDeployment2();

    // deploy with migration
    String oldContainerId = containerId;
    migrate(oldContainerId);
    processId = new HelloWorldProcessV2().getProcessId();
    containerId = clientDeploymentHelper.getRelease().getContainerId();

    // verify that the old release container was removed
    KieContainerResource oldContainer = getContainer(oldContainerId);
    Assert.assertNull(oldContainer);

    // verify that the new release container is running
    container = getContainer(containerId);
    Assert.assertEquals("Container was not started", KieContainerStatus.STARTED, container.getStatus());

    // verify that the migrated instance continues
    List<TaskSummary> tasks = clientExecutionHelper.getTasks("john");
    Assert.assertEquals("Migrated Task do not exist", 1, tasks.size());
    Assert.assertEquals("Task was not migrated", containerId, tasks.get(0).getContainerId());
    params = new HashMap<>();
    params.put("employee", "john");
    params.put("reason", "test on spring boot for migrated instance");
    clientExecutionHelper.completeTask(tasks.get(0).getId(), "john", params);
    tasks = clientExecutionHelper.getTasks("john");
    Assert.assertEquals("Migrated Task was not completed", 0, tasks.size());

    // execute the process with the new version
    params = new HashMap<>();
    params.put("employee", "john");
    params.put("reason", "test on spring boot");
    Long newProcessInstanceId = clientExecutionHelper.startNewProcessInstance(containerId, processId, params);
    Assert.assertNotNull("Process was not executed", newProcessInstanceId);

    cleanUp();
  }

  public void cleanUp() {
    // cancel all process instances and undeploy release
    Assert.assertTrue("Release was not undeployed", clientDeploymentHelper.undeploy(true));
  }
}
