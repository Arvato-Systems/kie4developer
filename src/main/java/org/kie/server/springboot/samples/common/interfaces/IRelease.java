package org.kie.server.springboot.samples.common.interfaces;

import org.springframework.stereotype.Component;

/**
 * One single Release
 * @author TRIBE01
 */
@Component
public interface IRelease {

	String getGroupId();
	String getArtifactId();
	String getVersion();
	String getContainerId();

	/**
	 * Get the unique deployment id for the release.
	 * @return the deployment id in the form <code>package:name:version</code>
	 */
	default String getDeploymentId() {
		return getGroupId() + ":" + getArtifactId() + ":" + getVersion();
	}

	/**
	 * Get the release id
	 * @return the release id for the kie server
	 */
	org.kie.server.api.model.ReleaseId getReleaseIdForServerAPI();

	/**
	 * Get the release id
	 * @return the release id for the kie client lib
	 */
	org.kie.api.builder.ReleaseId getReleaseIdForClientAPI();

}
