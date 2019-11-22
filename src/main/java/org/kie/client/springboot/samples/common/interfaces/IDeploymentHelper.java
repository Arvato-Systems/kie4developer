package org.kie.client.springboot.samples.common.interfaces;

import java.util.List;

/**
 * Helper Interface to handle Deployments for the JBPM Server.
 * For a deployment to a JBPM Server you need one (runtime) Container. Into this Container you can deploy a DeploymentUnit.
 * A DeploymentUnit contains one ore more Process Definitions. Each Process Definition contains a (XML) BPMN Process.
 * Beside you can deploy Workitemhandler that will be used by the BPMN Process.
 * @author TRIBE01
 */
public interface IDeploymentHelper {

	/**
	 * Get the release information
	 * @return the release meta information
	 */
	IRelease getRelease();

	/**
	 * Define the Process(es) you want to deploy on the Server
	 * @param processToDeploy process to deploy
	 */
	void setProcessToDeploy(IDeployableBPMNProcess processToDeploy); //TODO: make it possible to deploy multiple processes

	/**
	 * Define the Workitemhandler(s) you want to deploy on the Server
	 * @param workItemHandlerToDeploy workitemhandler to deploy
	 */
	void setWorkItemHandler(List<IDeployableWorkItemHandler> workItemHandlerToDeploy);

	/**
	 * Deploy all given Processes into the Server Container
	 */
	boolean deploy();

	/**
	 * Undeploy the Server Container
	 */
	boolean undeploy();

}
