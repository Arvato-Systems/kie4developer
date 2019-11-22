package org.kie.client.springboot.samples.client.workitemhandler;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.client.springboot.samples.common.interfaces.IDeployableWorkItemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Simple {@link WorkItemHandler} implementation that print out some 'Hello world' text.
 * You can pass in the variable <code>helloworldtext</code> to print a custom text.
 * The handler response with the variable <code>result</code> with the value 'Ola'.
 * @author TRIBE01
 */
@Component("HelloWorldWorkItemHandler") // with Spring this Handler get autowired to the KIE-Server
public class HelloWorldWorkItemHandler implements WorkItemHandler, IDeployableWorkItemHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorldWorkItemHandler.class);

	private final String VERSION = "1.0.0";
	private final String WORKITEMHANDLER_FILE = "src/test/resources/workitemhandlers/HelloWorldWorkItemHandler-1.0.0.jar";

	@Override
	public String getVersion() {
		return VERSION;
	}

	@Override
	public File getWorkItemHandlerJarFile() {
		return new File(WORKITEMHANDLER_FILE);
	}

	@Override
	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		LOGGER.info("start execution of " + HelloWorldWorkItemHandler.class);
		Map<String, Object> result = new HashMap<String, Object>();
		
		String helloworldtext = (String) getProcessVariableOrDefault(workItem, "helloworldtext", "Hello world");
		System.out.println(helloworldtext);

		result.put("result", "Ola");

		manager.completeWorkItem(workItem.getId(), result);
		LOGGER.info("end execution of " + HelloWorldWorkItemHandler.class);
	}

	@Override
	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		// No implementation for aborting required
	}

	/**
	 * Helper to retrieve a process variable
	 * @param workItem the workitem with all process variables
	 * @param key the parameter name to find
	 * @param defaultValue the default value if parameter is not found or null
	 * @return the parameter value or the provided default
	 */
	private Object getProcessVariableOrDefault(WorkItem workItem, String key, Object defaultValue) {
		return workItem.getParameters().get(key) == null ? defaultValue : workItem.getParameters().get(key);
	}
}
