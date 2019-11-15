package org.kie.client.springboot.samples.common;

import org.kie.api.KieServices;
import org.kie.server.api.model.ReleaseId;
import org.kie.client.springboot.samples.common.interfaces.IRelease;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Representation of a JBPM Release
 * @author TRIBE01
 */
@Component
public class Release implements IRelease{

	@Value("${spring.application.groupid}")
	private String groupId;
	@Value("${spring.application.name}")
	private String artifactId;
	@Value("${spring.application.version}")
	private String version;

	@Value("${spring.application.container.name}")
	private String projectName;
	@Value("${spring.application.container.version}")
	private String projectVersion;


	/**
	 * Get the group id for the release
	 * @return the group id
	 */
	public String getGroupId() {
		return groupId;
	}

	/**
	 * Get the artifact id for the release
	 * @return the artifact id
	 */
	public String getArtifactId() {
		return artifactId;
	}

	/**
	 * Get the version for the release
	 * @return the version number
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Get the container Id
	 * @return the container id
	 */
	public String getContainerId() {
		return projectName + "_" + projectVersion;
	}

	/**
	 * Get the unique release id
	 * @return release id
	 */
	public org.kie.server.api.model.ReleaseId getReleaseIdForServerAPI() {
		return new ReleaseId(getGroupId(), getArtifactId(), getVersion());
	}

	/**
	 * Get the unique release id
	 * @return release id
	 */
	public org.kie.api.builder.ReleaseId getReleaseIdForClientAPI() {
		return KieServices.Factory.get().newReleaseId(getGroupId(), getArtifactId(), getVersion());
	}

}
