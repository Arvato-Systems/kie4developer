package org.kie.server.springboot.samples.client.processes;

import java.io.File;
import org.jbpm.bpmn2.xml.XmlBPMNProcessDumper;
import org.jbpm.process.core.datatype.impl.type.StringDataType;
import org.jbpm.process.instance.impl.Action;
import org.jbpm.process.instance.impl.JavaScriptAction;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.jbpm.ruleflow.core.RuleFlowProcessFactory;
import org.kie.api.io.Resource;
import org.kie.api.runtime.process.ProcessContext;
import org.kie.internal.io.ResourceFactory;
import org.kie.server.springboot.samples.common.interfaces.IDeployableBPMNProcess;

/**
 * This is a sample {@link ProcessDefinition} that is based on a bpmn that is build by the jbpm fluent api ({@link RuleFlowProcessFactory})
 * @author TRIBE01
 */
public class HelloWorldProcess implements IDeployableBPMNProcess {

	private final String VERSION = "1.0.0";

	@Override
	public String getVersion() {
		return VERSION;
	}

	@Override
	public boolean isDistributedAsJar() {
		return false;
	}

	@Override
	public Resource getBPMNModel() {

		RuleFlowProcessFactory factory = RuleFlowProcessFactory.createProcess(getProcessId());

		final String ENV_environment = "dev";
		final String ENV_helloworldtext = "Hello World";

		factory
			// header
				.name(getName())
				.version("1.0.0")
				.packageName(getPackage())
			// environment variables
				//TODO: seems that we can use here only classes?
//			.global("environment", ENV_environment)
//			.global("helloworldtext", ENV_helloworldtext)
			// process variables
			.variable("myvar", new StringDataType() ,"default value","ItemSubjectRef", "_" + "myvar")
			// nodes
			.startNode(1).name("Start").done()
			.actionNode(2).name("Java Action 1")
				.action("java","System.out.println(\"Hello Java Sourcecode!\");").done()
			.actionNode(3).name("Java Action 2")
				.action("java","System.out.println(\"Hello \" + kcontext.getVariable(\"employee\") + \" \" + kcontext.getVariable(\"myvar\"));").done()
			.actionNode(4).name("Java Action 3")
			//TODO: check why this action doesn'nt work
				.action(new Action()  {
							@Override
							public void execute(ProcessContext context) throws Exception {
									System.out.println("test " + context);
							}
						}
				).done()
			.actionNode(5).name("Java Script Action 1")
				//TODO: check why this action doesn'nt work
				.action(new JavaScriptAction("console.log(\"Reached Java Script Action 1!\");")).done()
			//TODO: check why this workitemhandler doesn'nt work
			/*.workItemNode(6).name("Work Item 1")
				.inMapping("employee","employee")
				.outMapping("employee","employee")
				.workName("Log")
				.workParameterDefinition("Message", new StringDataType())
				.workParameter("Message", "#{employee}").done()*/
			.humanTaskNode(7).name("Human Task 1").actorId("john").taskName("John's Task").done()
			.endNode(8).name("End").done()
			// connections
			.connection(1,2)
			.connection(2,3)
			.connection(3,4)
			.connection(4,5)
			.connection(5,7)
			.connection(7,8);
		RuleFlowProcess process = factory.validate().getProcess();

		Resource res = ResourceFactory.newByteArrayResource(XmlBPMNProcessDumper.INSTANCE.dump(process).getBytes());
		res.setSourcePath(getProcessId()+".bpmn2"); // source path or target path must be set to be added into kbase
		return res;
	}

	@Override
	public File getJarFile() {
		return null;
	}

	@Override
	public File getPomFile() {
		return null;
	}

}
