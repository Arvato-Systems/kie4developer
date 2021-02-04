package com.arvato.workflow.kie4developer.basic;

import com.arvato.workflow.kie4developer.common.interfaces.IDeployableBPMNProcess;
import com.arvato.workflow.kie4developer.workitemhandler.JavaWorkItemHandler;
import org.jbpm.process.core.datatype.impl.type.ObjectDataType;
import org.jbpm.process.core.datatype.impl.type.StringDataType;
import org.jbpm.ruleflow.core.RuleFlowProcessFactory;
import org.jbpm.workflow.core.node.Join;
import org.jbpm.workflow.core.node.Split;

/**
 * This is a {@link IDeployableBPMNProcess} bpmn process model that is build with fluent api.
 *
 * @see RuleFlowProcessFactory
 */
public class SupportTicketProcess implements IDeployableBPMNProcess {

  private static final String VERSION = "1.0";
  private static final String ITEM_SUBJECT_REF = "ItemSubjectRef";
  private static final String CLASS_NAME = "className";
  private static final String METHOD_NAME = "methodName";
  private static final String METHOD_PARAMETER_TYPE = "methodParameterType";
  private static final String METHOD_PARAMETER = "methodParameter";

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

        // process variables
        // - yes, we can declare them here, but this is just optional because you can provide any variable anyhow
        // - yes, better would be to use the method with only name and type as parameter, but this has a bug in JBPM
        .variable(CLASS_NAME, new StringDataType(), null, ITEM_SUBJECT_REF, "_" + CLASS_NAME)
        .variable(METHOD_NAME, new StringDataType(), null, ITEM_SUBJECT_REF, "_" + METHOD_NAME)
        .variable(METHOD_PARAMETER_TYPE, new StringDataType(), null, ITEM_SUBJECT_REF, "_" + METHOD_PARAMETER_TYPE)
        .variable(METHOD_PARAMETER, new ObjectDataType(), null, ITEM_SUBJECT_REF, "_" + METHOD_PARAMETER)

        // nodes
        .startNode(1).name("Start")
        .done()

        .humanTaskNode(2).name("Confirm start")
        .taskName("Confirm start of automated execution").actorId("john")
        .done()

        .actionNode(3).name("Java Action")
        .action("java", "System.out.println(\"Java Action executed!\");")
        .done()

        .splitNode(4)
        .type(Split.TYPE_XOR)
        .constraint(5, "continue", "code", "java", "return className != null || methodName != null;")
        .constraint(7, "cancel", "code", "java", "/* else */ return true;")
        .done()

        .workItemNode(5).name("execute business logic")
        .workName(JavaWorkItemHandler.class.getSimpleName())
        .inMapping(CLASS_NAME, CLASS_NAME)
        .inMapping(METHOD_NAME, METHOD_NAME)
        .inMapping(METHOD_PARAMETER_TYPE, METHOD_PARAMETER_TYPE)
        .inMapping(METHOD_PARAMETER, METHOD_PARAMETER)
        .done()

        .humanTaskNode(6).name("Confirm execution")
        .taskName("Confirm completion of automated execution").actorId("john")
        .done()

        .joinNode(7)
        .type(Join.TYPE_XOR)
        .done()

        .endNode(8).name("End")
        .done()

        // connections
        .connection(1, 2)
        .connection(2, 3)
        .connection(3, 4)
        .connection(4, 5)
        .connection(4, 7)
        .connection(5, 6)
        .connection(6, 7)
        .connection(7, 8);
  }

}
