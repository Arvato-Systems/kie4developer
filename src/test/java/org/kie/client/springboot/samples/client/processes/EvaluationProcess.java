package org.kie.client.springboot.samples.client.processes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.kie.api.io.Resource;
import org.kie.client.springboot.samples.common.interfaces.IDeployableBPMNProcess;
import org.kie.internal.io.ResourceFactory;
import org.kie.server.api.model.definition.ProcessDefinition;

/**
 * This is a sample {@link ProcessDefinition} that is based on a bpmn inside a kjar archive
 * @author TRIBE01
 */
public class EvaluationProcess implements IDeployableBPMNProcess {

	private final String VERSION = "1.0.0";
	private final boolean IS_JAR = true;
	private final String PROCESSNAME = "evaluation";
	private final String PACKAGENAME = "org.jbpm.test";
	private final String KJAR_FILE = "src/test/resources/kjars/evaluation/jbpm-module.jar";
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
	public String getName() {
		return PROCESSNAME; // comes from the xml
	}

	@Override
	public String getPackage() {
		return PACKAGENAME; // comes from the xml
	}

	@Override
	public File getJarFile() {
		return new File(KJAR_FILE);
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

}
