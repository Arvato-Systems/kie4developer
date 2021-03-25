package com.arvato.workflow.processes;

import com.arvato.workflow.kie4developer.common.interfaces.IDeployableBPMNProcess;
import org.jbpm.process.core.datatype.impl.type.StringDataType;
import org.jbpm.ruleflow.core.RuleFlowProcessFactory;

/**
 * This is a {@link IDeployableBPMNProcess} bpmn process model that is build with fluent api.
 *
 * @see RuleFlowProcessFactory
 */
public class HelloWorldProcess implements IDeployableBPMNProcess {

  private static final String VERSION = "1.0";
  private static final String MY_VAR = "myvar";
  private static final String GLOBAL_VAR = "test";

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public void buildBPMNModel(RuleFlowProcessFactory factory) {
    factory
        // header
        .name(getName())
        .version(VERSION)
        .packageName(getPackage())

        // environment variables
        // - define within application.properties
        // - when defined must be used in at least one place
        .global(GLOBAL_VAR, new StringDataType().getStringType())

        // process variables
        // - yes, we can declare them here, but this is just optional because you can provide any variable anyhow
        // - yes, better would be to use the method with only name and type as parameter, but this has a bug in JBPM
        .variable(MY_VAR, new StringDataType(), null, "ItemSubjectRef", "_" + MY_VAR)

        // nodes
        .startNode(1).name("Start")
        .done()

        .actionNode(2).name("Java Action 1")
        .action("java", "System.out.println(\"Hello Java Sourcecode!\");")
        .done()

        .actionNode(3).name("Java Action 2")
        .action("java", "System.out.println(\"Hello \" + kcontext.getVariable(\"employee\"));")
        .done()

        // javascript seems not to work...
        //	.actionNode(4).name("Java Script Action 1")
        //	.action(new JavaScriptAction("print('Reached Java Script Action 1!');"))
        //	.done()

        //.actionNode(4).name("Javascript Action 2")
        //	.action("javascript","print('Reached Java Script Action 2!');")
        //	.done()

        .actionNode(4).name("Java Action 3")
        .action("java", "kcontext.setVariable(\""+MY_VAR+"\", kcontext.getKnowledgeRuntime().getGlobal(\""+GLOBAL_VAR+"\"));")
        .done()

        .actionNode(6).name("Java Action 4")
        .action("java", "System.out.println(kcontext.getVariable(\""+MY_VAR+"\"));")
        .done()

        .humanTaskNode(7).name("Human Task 1")
        .actorId("john")
        .taskName("John's Task")
        .done()

        .endNode(8).name("End")
        .done()

        // connections
        .connection(1, 2)
        .connection(2, 3)
        .connection(3, 4)
        .connection(4, 6)
        .connection(6, 7)
        .connection(7, 8);
  }

}
