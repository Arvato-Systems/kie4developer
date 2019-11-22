package org.kie.client.springboot.samples.common;

import java.io.File;
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
	@Value("${spring.application.project.name}")
	private String projectName;
	@Value("${spring.application.project.description}")
	private String projectDescription;
	@Value("${spring.application.project.jar}")
	private String jarFile;

	@Override
	public String getGroupId() {
		return groupId;
	}

	@Override
	public String getArtifactId() {
		return artifactId;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public String getContainerId() {
		return projectName + "_" + getVersion();
	}

	@Override
	public String getProjectName() {
		return projectName;
	}

	@Override
	public String getProjectDescription() {	return projectDescription; }

	@Override
	public File getJarFile() { return (jarFile == null || jarFile.length() == 0 ? null : new File(jarFile));	}

	@Override
	public org.kie.server.api.model.ReleaseId getReleaseIdForServerAPI() {
		return new ReleaseId(getGroupId(), getArtifactId(), getVersion());
	}

	@Override
	public org.kie.api.builder.ReleaseId getReleaseIdForClientAPI() {
		return KieServices.Factory.get().newReleaseId(getGroupId(), getArtifactId(), getVersion());
	}

}
