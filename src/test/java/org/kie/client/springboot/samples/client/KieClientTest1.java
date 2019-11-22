package org.kie.client.springboot.samples.client;

import com.sun.javafx.scene.control.skin.VirtualFlow.ArrayLinkedList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.client.springboot.samples.client.workitemhandler.HelloWorldWorkItemHandler;
import org.kie.client.springboot.samples.common.interfaces.IDeployableWorkItemHandler;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceList;
import org.kie.server.api.model.definition.ProcessDefinition;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.client.springboot.samples.client.processes.HelloWorldProcess;
import org.kie.client.springboot.samples.common.interfaces.IDeployableBPMNProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
public class KieClientTest1 {

	private static final Logger LOGGER = LoggerFactory.getLogger(KieClientTest1.class);
	@Autowired
	private KieClient kieClient;
	@Autowired
	private KieClientDeploymentHelper clientDeploymentHelper;
	@Autowired
	private KieClientExecutionHelper clientExecutionHelper;

	/**
	 * Run a series of client tests
	 * 1. create a new process
	 * 2. deploy a the process to the server
	 * 3. execute the process
	 * 4. show some useful information from the server
	 */
	@Test
	public void testDeployableBPMNProcessAsClass() {
		// 1. create a new process & set required workitemhandler
		IDeployableBPMNProcess processToDeploy = new HelloWorldProcess();
		List<IDeployableWorkItemHandler> workItemHandlerToDeploy = new ArrayList<>();
		workItemHandlerToDeploy.add(new HelloWorldWorkItemHandler());

		// 2. deploy a the process & workitemhandler to the server
		clientDeploymentHelper.setProcessToDeploy(processToDeploy);
		clientDeploymentHelper.setWorkItemHandler(workItemHandlerToDeploy);
		clientDeploymentHelper.deploy();

		// 3. execute the process
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("employee", "john");
		params.put("reason", "test on spring boot");

		Long processInstanceId = clientExecutionHelper.startNewProcessInstance(clientDeploymentHelper.getRelease().getContainerId(), processToDeploy.getProcessId(), params);
		LOGGER.info("Started new process with process instance id " + processInstanceId);

		//4. show some useful information from the server
		KieContainerResourceList containerList = showAvailableContainers();
		for (KieContainerResource container : containerList.getContainers()) {
			showAvailableProcessesDefinitions(container.getContainerId());
		}

		showAvailableActiveProcessesInstances();

		clientExecutionHelper.abortProcessInstance(processInstanceId);
		LOGGER.info("\t######### Aborted process instance with id " + processInstanceId);

		clientDeploymentHelper.undeploy();
	}

	// -------------------------------- helper ------------------------------------

    private KieContainerResourceList showAvailableContainers() {
    	KieContainerResourceList containers = kieClient.getKieServicesClient().listContainers().getResult();
        // check if the container is not yet deployed, if not deploy it
        if (containers != null) {
            for (KieContainerResource kieContainerResource : containers.getContainers()) {
							LOGGER.info("\t######### Found container " + kieContainerResource.getContainerId());
            }
        }
        return containers;
    }

    private List<ProcessDefinition> showAvailableProcessesDefinitions(String containerId) {
    	// query for all available process definitions
        List<ProcessDefinition> processes = kieClient.getQueryClient().findProcesses(0, Integer.MAX_VALUE);
        if (processes != null) {
        	for (ProcessDefinition process : processes) {
						LOGGER.info("\t######### Found process definition: " + process.getId());
						// get details of process definition
						ProcessDefinition definition =  kieClient.getProcessClient().getProcessDefinition(containerId, process.getId());
						LOGGER.info("\t######### Definition details: " + definition);
           }
        }
        return processes;
    }

    private List<ProcessInstance> showAvailableActiveProcessesInstances() {
        List<Integer> status = Arrays.asList(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE);
        List<ProcessInstance> activeInstances = kieClient.getQueryClient().findProcessInstancesByStatus(status, 0, Integer.MAX_VALUE);
        if (activeInstances != null) {
            for (ProcessInstance instance : activeInstances) {
							LOGGER.info("\t######### Found process instance: " + instance.getId());
            	Map<String, Object> variables =  kieClient.getProcessClient().getProcessInstanceVariables(instance.getContainerId(), instance.getId());
							LOGGER.info("\t######### Process instance variables: " + variables);
							kieClient.getProcessClient().setProcessVariables(instance.getContainerId(), instance.getId(), variables);
							LOGGER.info("\t######### Process instance variables changed: " + variables);
            }
        }
        return activeInstances;
    }

}
