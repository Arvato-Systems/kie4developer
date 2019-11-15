package org.kie.client.springboot.samples.common.interfaces;

import org.kie.api.runtime.process.WorkItemHandler;
import org.springframework.stereotype.Component;

/**
 * Helper to handle {@link WorkItemHandler}
 * @author TRIBE01
 */
@Deprecated
@Component
public interface IWorkItemHandlerHelper {

	/**
	 * Upload a new WorkItemHandler to the Engine
	 */
	void uploadWorkItemHandler();
	// TODO impl. see:http://fxapps.blogspot.com/2015/04/creating-custom-work-item-handler-in.html
	// TODO impl. see:https://www.codelikethewind.org/2017/10/30/how-to-create-a-custom-work-item-handler-in-jbpm/

	/**
	 * Register a WorkItemHandler to the Engine
	 * note: if you using Spring you can use the dependency injection feature
	 */
	void registerWorkItemHandler();

}
