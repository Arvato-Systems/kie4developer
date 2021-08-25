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
package com.arvato.workflow.kie4developer.common.interfaces;

import java.util.List;
import org.kie.server.api.model.admin.MigrationReportInstance;

/**
 * Helper Interface to handle Deployments for the JBPM Server.
 * For a deployment to a JBPM Server you need one (runtime) Container. Into this Container you can deploy a DeploymentUnit.
 * A DeploymentUnit contains one ore more Process Definitions. Each Process Definition contains a (XML) BPMN Process.
 * Beside you can deploy Workitemhandler that will be used by the BPMN Process to execute tasks within a transaction context.
 *
 * @author TRIBE01
 */
public interface IDeploymentHelper {

	// TODO: Support form upload - this is a BICCW specific thing

	/**
	 * Get the release information
	 *
	 * @return the release meta information
	 */
	IRelease getRelease();

	/**
	 * Define the external Dependencies you want to deploy on the Server
	 *
	 * @param dependenciesToDeploy maven dependencies to deploy
	 */
	void setDependenciesToDeploy(List<IDeployableDependency> dependenciesToDeploy);

	/**
	 * Define the Process(es) you want to deploy on the Server
	 *
	 * @param processesToDeploy process to deploy
	 */
	void setProcessesToDeploy(List<Class<? extends IDeployableBPMNProcess>> processesToDeploy);

	/**
	 * Define the Process(es) and Mock(s) you want to deploy on the Server
	 *
	 * @param processesToDeploy process to deploy
	 * @param processesToMock process to mock
	 */
	void setProcessesToDeploy(List<Class<? extends IDeployableBPMNProcess>> processesToDeploy, List<Class<? extends IDeployableBPMNProcess>> processesToMock);

	/**
	 * Define the Service class(es) you want to deploy on the Server
	 *
	 * @param serviceClassesToDeploy classes to deploy
	 */
	void setServiceClassesToDeploy(List<Class> serviceClassesToDeploy);

	/**
	 * Define the Workitemhandler(s) you want to deploy on the Server
	 *
	 * @param workItemHandlerToDeploy workitemhandler to deploy
	 */
	void setWorkItemHandlersToDeploy(List<Class<? extends IDeployableWorkItemHandler>> workItemHandlerToDeploy);

	/**
	 * Deploy all given Processes into the Server Container
	 *
	 * @param overwrite overwrite existing container with same container id
	 * @return <code>true</code> if deployment was successful, otherwise <code>false</code>
	 */
	boolean deploy(boolean overwrite);

	/**
	 * Undeploy the Server Container
	 *
	 * @oaram cancelAllRunningInstances cancel running process instances
	 * @return <code>true</code> if undeployment was successful, otherwise <code>false</code>
	 */
	boolean undeploy(boolean cancelAllRunningInstances);

	/**
	 * Deploy all given Processes into the Server Container and migrate those active process instances to the
	 * new Server Container for which a Process with the same name exist. Old Container gets undeployed
	 * param oldContainerId old container id of process instances that should be migrated
	 *
	 * @param oldContainerId the container id containing the old active process instances
	 * @return migration result reports
	 * @see {@link MigrationReportInstance}
	 * @see {@link IDeploymentHelper#undeploy(boolean)}
	 */
	List<MigrationReportInstance> deployWithMigration(String oldContainerId);

	/**
	 * Migrate all active process instances from one Server Container into another.
	 *
	 * @param oldContainerId the container id containing the old active process instances
	 * @return migration result reports
	 * @see {@link MigrationReportInstance}
	 */
	List<MigrationReportInstance> migrate(String oldContainerId);

}
