package org.kie.server.springboot.samples.common.interfaces;

import org.kie.api.runtime.process.WorkItemHandler;
import org.springframework.stereotype.Component;

/**
 * Helper to handle {@link WorkItemHandler}
 * @author TRIBE01
 */
@Component
public interface IWorkItemHandlerHelper {

	// see http://fxapps.blogspot.com/2015/04/creating-custom-work-item-handler-in.html
	// see https://www.codelikethewind.org/2017/10/30/how-to-create-a-custom-work-item-handler-in-jbpm/
	
	/**
	 * Upload a new WorkItemHandler to the Engine
	 */
	public void uploadWorkItemHandler();
	
	/**
	 * Register a WorkItemHandler to the Engine
	 */
	public void registerWorkItemHandler();
	
}
