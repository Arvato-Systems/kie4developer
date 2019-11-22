package org.kie.client.springboot.samples.common.interfaces;

import java.io.File;

/**
 * One single Release
 * @author TRIBE01
 */
public interface IRelease {

	/**
	 * Get the group id for the release
	 * @return the group id
	 */
	String getGroupId();

	/**
	 * Get the artifact id for the release
	 * @return the artifact id
	 */
	String getArtifactId();

	/**
	 * Get the version for the release
	 * @return the version number
	 */
	String getVersion();

	/**
	 * Get the container Id
	 * @return the container id
	 */
	String getContainerId();

	/**
	 * Get the project name for the release
	 * @return the project name
	 */
	String getProjectName();

	/**
	 * Get the project description for the release
	 * @return the project description
	 */
	String getProjectDescription();

	/**
	 * Get the kjar file that contains the Release (KModule)
	 * @see #isDistributedAsJar()
	 * @return the .jar file
	 */
	File getJarFile();

	/**
	 * Check if the release is distributed as kjar (true) or if it has to be build (false)
	 * if distributed as jar {@link #getJarFile()} can be used to retrieve the file
	 * @return true if release files were provided as jar file
	 */
	default boolean isDistributedAsJar() { return getJarFile() != null; }

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
