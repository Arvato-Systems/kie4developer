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
	 * get the unique deployment id for the release.
	 * @return the deployment id in the form <code>package:name:version</code>
	 */
	default String getDeploymentId() {
		return getGroupId() + ":" + getArtifactId() + ":" + getVersion();
	}

	org.kie.server.api.model.ReleaseId getReleaseIdForServerAPI();
	org.kie.api.builder.ReleaseId getReleaseIdForClientAPI();

}
