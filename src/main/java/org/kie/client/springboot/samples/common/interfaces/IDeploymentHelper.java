package org.kie.client.springboot.samples.common.interfaces;

import org.springframework.stereotype.Component;

/**
 * Helper Interface to handle Deployments for the JBPM Server.
 * For a deployment to a JBPM Server you need one (runtime) Container. Into this Container you can deploy a DeploymentUnit.
 * A DeploymentUnit contains one ore more Process Definitions. Each Process Definition contains a (XML) BPMN Process.
 * @author TRIBE01
 */
@Component
public interface IDeploymentHelper {

	/**
	 * Get the release information
	 * @return the release meta information
	 */
	public IRelease getRelease();

	/**
	 * Define the Process(es) you want to deploy on the Server
	 * @param processToDeploy process to deploy
	 */
	public void setProcessToDeploy(
			IDeployableBPMNProcess processToDeploy); //TODO: make it possible to deploy multiple processes

	/**
	 * Deploy all given Processes into the Server Container
	 */
	public boolean deploy();

	/**
	 * Undeploy the Server Container
	 */
	public boolean undeploy();

}
