package org.kie.client.springboot.samples.common.interfaces;

import java.io.File;
import java.util.HashMap;
import org.kie.api.io.Resource;
import org.kie.api.runtime.process.WorkItemHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Interface to define a new BPMN Process
 * @author TRIBE01
 */
@Component
public interface IDeployableBPMNProcess {

	@Autowired
	IRelease release = null;

	/**
	 * Get the name of the Process
	 * @return the name
	 */
	default String getName() {
		return this.getClass().getSimpleName();
	}

	/**
	 * Get the package of the Process
	 * @return the package
	 */
	default String getPackage() {
		return this.getClass().getPackage().getName();
	}

	/**
	 * Get the version of the process
	 * @return the version number
	 */
	String getVersion();

	/**
	 * Get the unique process id for the process
	 * @return the process id in the form <code>package:name:version</code>
	 */
	default String getProcessId() {
		return getName();
	}

	/**
	 * Check if the process is distributed as jar (true) or as in in-memory file (false)
	 * if distributed as jar {@link #getJarFile()} and {@link #getPomFile()} can be used to retrieve the process.
	 * if distributed as in-memory file {@link #getBPMNModel()} can be used to retrieve the process.
	 * @return true if process files were provided as jar file
	 */
	boolean isDistributedAsJar();

	/**
	 * Check if the process needs workitemhandlers
	 * @return true if workitemhandler are required for the process
	 */
	boolean hasWorkItemHandler();

	/**
	 * Get the jar file that contains the WorkItemHandlers
	 * @return the .jar file
	 */
	File getWorkItemHandlerJarFile();

	/**
	 * Get the list of workitemhandlers for the process
	 * @return the list of workitemhandlers
	 */
	HashMap<String,WorkItemHandler> getWorkItemHandlers();

	/**
	 * Get the BPMN Process Model that can be used for deployment
	 * @return the BPM Process Definition
	 */
	Resource getBPMNModel();

	/**
	 * Get the jar file that contains the BPMN Process Model
	 * @return the .jar file
	 */
	File getJarFile();

	/**
	 * Get the pom file for the BPMN Process Model
	 * @return the .pom file
	 */
	File getPomFile();

}
