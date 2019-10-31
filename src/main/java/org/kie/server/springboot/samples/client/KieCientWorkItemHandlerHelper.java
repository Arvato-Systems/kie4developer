package org.kie.server.springboot.samples.client;

import org.kie.server.springboot.samples.common.interfaces.IRelease;
import org.kie.server.springboot.samples.common.interfaces.IWorkItemHandlerHelper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Client implementation of the {@link IWorkItemHandlerHelper}
 * @author TRIBE01
 */
public class KieCientWorkItemHandlerHelper implements IWorkItemHandlerHelper {

	@Autowired
	private IRelease release;
	@Autowired
	private KieClient kieClient;
	
	@Override
	public void uploadWorkItemHandler() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void registerWorkItemHandler() {
		// TODO Auto-generated method stub
		
	}


	
}
