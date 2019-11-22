package org.kie.client.springboot.samples.client.processes;

import org.kie.api.io.Resource;
import org.kie.client.springboot.samples.common.interfaces.IDeployableBPMNProcess;
import org.kie.internal.io.ResourceFactory;
import org.kie.server.api.model.definition.ProcessDefinition;

/**
 * This is a sample {@link ProcessDefinition} that is based on a xml bpmn file
 * @author TRIBE01
 */
public class EvaluationProcess implements IDeployableBPMNProcess {

	private final String VERSION = "1.0.0";
	private final String PROCESSNAME = "evaluation"; // comes from the xml
	private final String PACKAGENAME = "org.jbpm.test"; // comes from the xml
	private final String BPMN_FILE = "src/test/resources/kjars/evaluation/evaluation.bpmn2";

	@Override
	public String getVersion() {
		return VERSION;
	}

	@Override
	public String getName() {	return PROCESSNAME;	}

	@Override
	public String getPackage() { return PACKAGENAME;	}

	@Override
	public Resource getBPMNModel() {
		Resource res = ResourceFactory.newFileResource(BPMN_FILE);
		res.setSourcePath(getProcessId()+".bpmn2"); // source path or target path must be set to be added into kbase
		return res;

//CODE to get the BPMN file ou of the jar module file
//		Resource res = null;
//		try {
//			FileInputStream fis = new FileInputStream(KJAR_FILE);
//			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
//			ZipEntry entry;
//			while ((entry = zis.getNextEntry()) != null) {
//				if (entry.getName().equals(BPMN_FILE)) {
//					res = ResourceFactory.newInputStreamResource(zis);
//					res.setSourcePath(BPMN_FILE); // source path or target path must be set to be added into kbase
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return res;
	}

}
