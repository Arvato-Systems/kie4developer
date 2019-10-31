package org.kie.server.springboot.samples.client.workitemhandler;

import java.util.HashMap;
import java.util.Map;

import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

/**
 * Simple {@link WorkItemHandler} implementation that print out some 'hello world' text.
 * You can pass in the variable <code>helloworldtext</code> to show a custom text.
 * @author TRIBE01
 */
public class HelloWorldWorkItemHandler implements WorkItemHandler {
	
	@Override
	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		Map<String, Object> result = new HashMap<String, Object>();
		
		Object helloworldtext = getProcessVariableOrDefault(workItem, "helloworldtext", "Hello World!");
		System.out.println(helloworldtext);
		
		manager.completeWorkItem(workItem.getId(), result);
	}
	
	/**
	 * 
	 * @param workItem the workitem with all process variables
	 * @param key the variable name to find
	 * @param defaultValue the default value if variable is not found or null
	 * @return the process variable value or the default
	 */
	private Object getProcessVariableOrDefault(WorkItem workItem, String key, Object defaultValue) {
		return workItem.getParameters().get(key) == null ? defaultValue : workItem.getParameters().get(key);
	}

	@Override
	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		// No implementation for aborting required
	}

}
