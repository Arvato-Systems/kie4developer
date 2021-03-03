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
        "global.test=Bonjour"
    })
public class ProcessMigrationTest extends AbstractProcessTest {

  @SpyBean
  private IRelease mockRelease;

  @After
  public void dispose() {
    clientDeploymentHelper.undeploy(true);
  }

  public void prepareDeployment1() {
    // prepare release
    clientDeploymentHelper.setProcessesToDeploy(Collections.singletonList(HelloWorldProcess.class));
    clientDeploymentHelper.setWorkItemHandlersToDeploy(Collections.singletonList(HelloWorldWorkItemHandler.class));
    clientDeploymentHelper.setDependenciesToDeploy(new ArrayList<>());
    // deploy release
    clientDeploymentHelper.deploy(true);
  }

  public void prepareDeployment2(String oldContainerId) {
    // prepare release
    clientDeploymentHelper.setProcessesToDeploy(Collections.singletonList(HelloWorldProcessV2.class));
    clientDeploymentHelper.setWorkItemHandlersToDeploy(Collections.singletonList(HelloWorldWorkItemHandler.class));
    // deploy release
    List<MigrationReportInstance> migrationReport = clientDeploymentHelper.deployWithMigration(oldContainerId);
    Assert.assertTrue("Migration was not successful", migrationReport.get(0).isSuccessful());
  }

  public void prepareDeployment3() {
    // prepare release
    clientDeploymentHelper.setProcessesToDeploy(Collections.singletonList(HelloWorldProcessV2.class));
    clientDeploymentHelper.setWorkItemHandlersToDeploy(Collections.singletonList(HelloWorldWorkItemHandler.class));
    // deploy release
    clientDeploymentHelper.deploy(true);
  }

  public void prepareDeployment4(String oldContainerId) {
    // prepare release
    clientDeploymentHelper.setProcessesToDeploy(Collections.singletonList(HelloWorldProcessV3.class));
    clientDeploymentHelper.setWorkItemHandlersToDeploy(Collections.singletonList(HelloWorldWorkItemHandler.class));
    // deploy release
    List<MigrationReportInstance> migrationReport = clientDeploymentHelper.deployWithMigration(oldContainerId);
    Assert.assertTrue("Migration was successful", !migrationReport.get(0).isSuccessful());
  }

  @Test
  public void testOverwritingDeployment() {
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

    // deploy with overwrite
    prepareDeployment3();
    containerId = clientDeploymentHelper.getRelease().getContainerId();
    processId = new HelloWorldProcessV2().getProcessId();

    // execute the process
    params = new HashMap<>();
    params.put("employee", "john");
    params.put("reason", "test on spring boot");

    Long newProcessInstanceId = clientExecutionHelper.startNewProcessInstance(containerId, processId, params);
    Assert.assertNotNull("Process was not executed", newProcessInstanceId);

    cleanUp();
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

    // deploy with migration
    String oldContainerId = containerId;
    when(mockRelease.getVersion()).thenReturn("1.0.1");  // fake new release version
    prepareDeployment2(oldContainerId);
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
    when(mockRelease.getVersion()).thenCallRealMethod(); // remove mock
    cleanUp();
  }

  @Test
  public void testMigratingDeploymentWithManualCorrection() {
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

    // deploy with migration
    String oldContainerId = containerId;
    when(mockRelease.getVersion()).thenReturn("1.0.1");  // fake new release version
    prepareDeployment4(oldContainerId);
    processId = new HelloWorldProcessV3().getProcessId();
    containerId = clientDeploymentHelper.getRelease().getContainerId();

    // verify that the old release container was not removed (= expected migration failure)
    KieContainerResource oldContainer = getContainer(oldContainerId);
    Assert.assertNotNull("Container was removed", oldContainer);

    // verify that the new release container is running
    container = getContainer(containerId);
    Assert.assertEquals("Container was not started", KieContainerStatus.STARTED, container.getStatus());

    // verify that the migrated instance continues
    List<TaskSummary> tasks = clientExecutionHelper.getTasks("john");
    Assert.assertEquals("Migrated Task do not exist", 1, tasks.size());
    Assert.assertEquals("Task was already migrated", oldContainerId, tasks.get(0).getContainerId());

    // do manual migration with manual node mapping as result of the non possible auto. migration
    String newProcessId = new HelloWorldProcessV3().getProcessId();
    String oldNodeId = "_jbpm-unique-7";
    String newNodeId = "_jbpm-unique-8";
    Map<String,String> nodeMapping = new HashMap<>();
    nodeMapping.put(oldNodeId, newNodeId);
    MigrationReportInstance migrationReportInstance = kieClient.getProcessAdminClient()
        .migrateProcessInstance(oldContainerId, processInstanceId, containerId, newProcessId, nodeMapping);
    Assert.assertTrue("Migration was not successful", migrationReportInstance.isSuccessful());

    // now remove the old container
    kieClient.getKieServicesClient().disposeContainer(oldContainerId);
    oldContainer = getContainer(oldContainerId);
    Assert.assertNull("Container was not removed", oldContainer);

    // verify that the migrated instance continues
    tasks = clientExecutionHelper.getTasks("john");
    Assert.assertEquals("Migrated Task do not exist", 1, tasks.size());
    Assert.assertEquals("Task was not migrated", containerId, tasks.get(0).getContainerId());
    Assert.assertEquals("Task was not migrated", "Human Task 2", tasks.get(0).getName());

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
    when(mockRelease.getVersion()).thenCallRealMethod(); // remove mock
    cleanUp();
  }

  public void cleanUp() {
    // cancel all process instances and undeploy release
    Assert.assertTrue("Release was not undeployed", clientDeploymentHelper.undeploy(true));
  }
}
