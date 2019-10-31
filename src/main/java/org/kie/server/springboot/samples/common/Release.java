package org.kie.server.springboot.samples.common;

import org.kie.api.KieServices;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.springboot.samples.common.interfaces.IRelease;
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
	 * get the group id for the release
	 */
	public String getGroupId() {
		return groupId;
	}

	/**
	 * get the artifact id for the release
	 */
	public String getArtifactId() {
		return artifactId;
	}

	/**
	 * get the version for the release
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * get the container Id
	 * @return the container id
	 */
	public String getContainerId() {
		return projectName + "_" + projectVersion;
	}

	/**
	 * get the unique release id
	 * @return release id
	 */
	public org.kie.server.api.model.ReleaseId getReleaseIdForServerAPI() {
		return new ReleaseId(getGroupId(), getArtifactId(), getVersion());
	}

	/**
	 * get the unique release id
	 * @return release id
	 */
	public org.kie.api.builder.ReleaseId getReleaseIdForClientAPI() {
		return KieServices.Factory.get().newReleaseId(getGroupId(), getArtifactId(), getVersion());
	}
}
