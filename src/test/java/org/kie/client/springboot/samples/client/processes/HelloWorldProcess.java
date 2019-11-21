package org.kie.client.springboot.samples.client.processes;

import java.io.File;
import java.util.HashMap;
import org.jbpm.bpmn2.xml.XmlBPMNProcessDumper;
import org.jbpm.process.core.datatype.impl.type.StringDataType;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.jbpm.ruleflow.core.RuleFlowProcessFactory;
import org.kie.api.io.Resource;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.client.springboot.samples.common.interfaces.IDeployableBPMNProcess;
import org.kie.client.springboot.samples.common.interfaces.IDeployableWorkItemHandler;
import org.kie.internal.io.ResourceFactory;
import org.kie.client.springboot.samples.client.workitemhandler.HelloWorldWorkItemHandler;
import org.kie.server.api.model.definition.ProcessDefinition;

/**
 * This is a sample {@link ProcessDefinition} that is based on a bpmn that is build by the jbpm fluent api ({@link RuleFlowProcessFactory})
 * @author TRIBE01
 */
public class HelloWorldProcess implements IDeployableBPMNProcess {

	private final String VERSION = "1.0.0";
	private final boolean IS_JAR = false;

	@Override
	public String getVersion() {
		return VERSION;
	}

	@Override
	public boolean isDistributedAsJar() {
		return IS_JAR;
	}

	@Override
	public HashMap<String, IDeployableWorkItemHandler> getWorkItemHandlers() {
		HashMap<String,IDeployableWorkItemHandler> workitemhandler = new HashMap<>();
		HelloWorldWorkItemHandler helloWorldWorkItemHandler = new HelloWorldWorkItemHandler();
		workitemhandler.put(helloWorldWorkItemHandler.getName(), helloWorldWorkItemHandler);
		return workitemhandler;
	}

	@Override
	public Resource getBPMNModel() {
		RuleFlowProcessFactory factory = RuleFlowProcessFactory.createProcess(getProcessId());
		factory
				// header
				.name(getName())
				.version("1.0.0")
				.packageName(getPackage())
				// environment variables ... but folks suggest to not use them
				//.global("environment", "org.kie.server.springboot.samples.server.globals.EnvironmentGlobal")
				// process variables
				.variable("myvar", new StringDataType(),"default value","ItemSubjectRef", "_" + "myvar")
				// nodes
				.startNode(1).name("Start").done()
				.actionNode(2).name("Java Action 1")
				.action("java","System.out.println(\"Hello Java Sourcecode!\");").done()
				.actionNode(3).name("Java Action 2")
				.action("java","System.out.println(\"Hello \" + kcontext.getVariable(\"employee\") + \" \" + kcontext.getVariable(\"myvar\") + \" \" + kcontext.getVariable(\"environment\"));").done()
				// javascript seems not to work...
				//	.actionNode(4).name("Java Script Action 1")
				//	.action(new JavaScriptAction("print('Reached Java Script Action 1!');")).done()
				//.actionNode(4).name("Javascript Action 2")
				//	.action("javascript","print('Reached Java Script Action 2!');").done()
				.actionNode(4).name("Java Action end")
				.action("java","kcontext.setVariable(\"myvar\", \"Bonjour\");").done()
				.workItemNode(5).name("Work Item 1")
				.workName("HelloWorldWorkItemHandler")
				//.workParameter("helloworldtext", "Hello") // this can be used to provide constants
				//.workParameter("helloworldtext", "#{myvar}") // this can be used as alternative to inMapping
				.inMapping("helloworldtext", "myvar") // optional
				.outMapping("result","myvar") // optional
				.done()
				.actionNode(6).name("Java Action end")
				.action("java","System.out.println(kcontext.getVariable(\"myvar\"));").done()
				.humanTaskNode(7).name("Human Task 1").actorId("john").taskName("John's Task").done()
				.endNode(8).name("End").done()
				// connections
				.connection(1,2)
				.connection(2,3)
				.connection(3,4)
				.connection(4,5)
				.connection(5,6)
				.connection(6,7)
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
