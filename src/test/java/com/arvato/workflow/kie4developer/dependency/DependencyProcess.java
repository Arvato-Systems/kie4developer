package com.arvato.workflow.kie4developer.dependency;

import com.arvato.workflow.kie4developer.common.interfaces.IDeployableBPMNProcess;
import com.arvato.workflow.kie4developer.workitemhandler.JavaWorkItemHandler;
import org.jbpm.process.core.datatype.impl.type.StringDataType;
import org.jbpm.ruleflow.core.RuleFlowProcessFactory;

/**
 * This is a {@link IDeployableBPMNProcess} bpmn process model that is build with fluent api.
 *
 * @see RuleFlowProcessFactory
 */
public class DependencyProcess implements IDeployableBPMNProcess {

  private static final String VERSION = "1.0";
  private static final String CLASS_NAME = "className";
  private static final String METHOD_NAME = "methodName";


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
        .variable("batikVersion", new StringDataType(), null, "ItemSubjectRef", "_" + "batikVersion")

        // nodes
        .startNode(1).name("Start").done()
        .workItemNode(2).name("create")
        .workName(JavaWorkItemHandler.class.getSimpleName())
        .workParameter(CLASS_NAME, "\"" + DependencyService.class.getName() + "\"")
        .workParameter(METHOD_NAME, "\"create\"")
        .outMapping("serviceResponse", "batikVersion")
        .done()

        .workItemNode(3).name("modify")
        .workName(JavaWorkItemHandler.class.getSimpleName())
        .workParameter(CLASS_NAME, "\"" + DependencyService.class.getName() + "\"")
        .workParameter(METHOD_NAME, "\"modify\"")
        .workParameter("methodParameterType", "\"" + String.class.getName() + "\"")
        .inMapping("methodParameter", "batikVersion")
        .outMapping("serviceResponse", "batikVersion")
        .done()

        .humanTaskNode(4).name("Human Task")
        .actorId("john")
        .taskName("Verify")
        .inMapping("taskInput", "batikVersion")
        .outMapping("taskOutput", "batikVersion")
        .done()

        .workItemNode(5).name("print")
        .workName(JavaWorkItemHandler.class.getSimpleName())
        .workParameter(CLASS_NAME, "\"" + DependencyService.class.getName() + "\"")
        .workParameter(METHOD_NAME, "\"print\"")
        .workParameter("methodParameterType", "\"" + String.class.getName() + "\"")
        .inMapping("methodParameter", "batikVersion")
        .done()

        .endNode(6).name("End")
        .done()

        // connections
        .connection(1, 2)
        .connection(2, 3)
        .connection(3, 4)
        .connection(4, 5)
        .connection(5, 6);
  }

}
