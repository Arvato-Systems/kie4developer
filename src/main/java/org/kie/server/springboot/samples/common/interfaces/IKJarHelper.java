package org.kie.server.springboot.samples.common.interfaces;

import java.io.File;

import org.springframework.stereotype.Component;

/**
 * Helper to handle KJars
 * @author TRIBE01
 */
@Component
public interface IKJarHelper {

	/**
	 * Build a kjar that can be used for deployment on the engine
	 * @return the created kjar
	 */
	public File buildKJar();
	// https://github.com/kiegroup/jbpm/blob/7.6.x/jbpm-runtime-manager/src/test/java/org/jbpm/runtime/manager/impl/deploy/AbstractDeploymentDescriptorTest.java
	
}
