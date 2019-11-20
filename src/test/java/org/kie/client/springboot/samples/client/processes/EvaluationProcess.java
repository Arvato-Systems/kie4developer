package org.kie.client.springboot.samples.client.processes;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.kie.api.io.Resource;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.client.springboot.samples.common.interfaces.IDeployableBPMNProcess;
import org.kie.internal.io.ResourceFactory;

/**
 * This is a sample {@link ProcessDefinition} that is based on a bpmn inside a kjar archive
 * @author TRIBE01
 */
public class EvaluationProcess implements IDeployableBPMNProcess {

	private final String VERSION = "1.0.0";
	private final boolean IS_JAR = true;
	private final boolean USE_WORKITEMHANDLERS = false;
	private final String PROCESSNAME = "evaluation";
	private final String PACKAGENAME = "org.jbpm.test";
	private final String KJAR_FILE = "src/test/resources/kjars/evaluation/jbpm-module.jar";
	private final String POM_FILE = "src/test/resources/kjars/evaluation/pom.xml";
	private final String BPMN_FILE = "evaluation.bpmn2";

	@Override
	public String getVersion() {
		return VERSION;
	}

	@Override
	public boolean isDistributedAsJar() {
		return IS_JAR;
	}

	@Override
	public boolean hasWorkItemHandler() {
		return USE_WORKITEMHANDLERS;
	}

	@Override
	public File getWorkItemHandlerJarFile() {
		return null;
	}

	@Override
	public HashMap<String, WorkItemHandler> getWorkItemHandlers() {
		return (HashMap<String, WorkItemHandler>) Collections.EMPTY_MAP;
	}

	@Override
	public String getName() {
		return PROCESSNAME; // comes from the xml
	}

	@Override
	public String getPackage() {
		return PACKAGENAME; // comes from the xml
	}

	@Override
	public Resource getBPMNModel() {
		Resource res = null;
		try {
			FileInputStream fis = new FileInputStream(KJAR_FILE);
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if (entry.getName().equals(BPMN_FILE)) {
					res = ResourceFactory.newInputStreamResource(zis);
					res.setSourcePath(BPMN_FILE); // source path or target path must be set to be added into kbase
				}
			}
			//zis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return res;
	}

	@Override
	public File getJarFile() {
		return new File(KJAR_FILE);
	}

	@Override
	public File getPomFile() {
		return new File(POM_FILE);
	}
}
