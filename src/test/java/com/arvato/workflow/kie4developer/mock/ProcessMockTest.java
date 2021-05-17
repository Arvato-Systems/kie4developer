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
package com.arvato.workflow.kie4developer.mock;

import com.arvato.workflow.kie4developer.AbstractProcessTest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
        "spring.application.autodeploy=false"
    })
public class ProcessMockTest extends AbstractProcessTest {

  @Before
  public void prepare() {
    // prepare release
    clientDeploymentHelper.setProcessesToDeploy(
        Collections.singletonList(FirstProcess.class),
        Collections.singletonList(SecondProcess.class)); // mock this process
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
    String processId = new FirstProcess().getProcessId();

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
    String processId = new FirstProcess().getProcessId();

    // execute the process
    Long processInstanceId = clientExecutionHelper.startNewProcessInstance(containerId, processId, new HashMap<>());
    Assert.assertNotNull("Process was not executed", processInstanceId);

    ProcessInstance processInstance = kieClient.getProcessClient().getProcessInstance(containerId, processInstanceId);
    Assert.assertEquals("ProcessInstance is not active anymore", org.jbpm.process.instance.ProcessInstance.STATE_COMPLETED,
        processInstance.getState().intValue());
  }
}
