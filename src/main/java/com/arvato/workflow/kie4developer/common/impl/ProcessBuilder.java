package com.arvato.workflow.kie4developer.common.impl;

import com.arvato.workflow.kie4developer.common.interfaces.IDeployableBPMNProcess;
import org.jbpm.bpmn2.xml.XmlBPMNProcessDumper;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.jbpm.ruleflow.core.RuleFlowProcessFactory;
import org.jbpm.workflow.core.node.EndNode;
import org.jbpm.workflow.core.node.EventNode;
import org.jbpm.workflow.core.node.FaultNode;
import org.jbpm.workflow.core.node.Join;
import org.jbpm.workflow.core.node.Split;
import org.jbpm.workflow.core.node.StartNode;
import org.jbpm.workflow.core.node.TimerNode;
import org.kie.api.definition.process.Node;
import org.kie.api.io.Resource;
import org.kie.internal.io.ResourceFactory;

/**
 * Helper to create BPM Process Definitions
 *
 * @author TRIBE01
 */
public class ProcessBuilder {

  /**
   * Build the BPM Process Definition (XML of Process model)
   *
   * @param deployableBPMNProcess the Business Process
   * @return the layouted BPM Process Definition Resource
   */
  public static Resource build(IDeployableBPMNProcess deployableBPMNProcess) {
    Resource res = deployableBPMNProcess.buildBPMNModel();
    if (res == null){
      RuleFlowProcessFactory factory = RuleFlowProcessFactory.createProcess(deployableBPMNProcess.getProcessId());
      deployableBPMNProcess.buildBPMNModel(factory);

      // validate process model
      RuleFlowProcess process = factory.validate().getProcess();

      // give any process model node a unique id for later process migrations
      for (Node node : process.getNodes()) {
        node.getMetaData().put("UniqueId", "_jbpm-unique-" + node.getId());
      }

      // layout the process model nodes
      layoutElements(process);

      // generate the xml representation
      res = ResourceFactory.newByteArrayResource(XmlBPMNProcessDumper.INSTANCE.dump(process).getBytes());
    }
    res.setSourcePath(deployableBPMNProcess.getProcessId() + ".bpmn2"); // source path or target path must be set to be added into kbase
    return res;
  }

  /**
   * Auto layout the process model nodes of a given process
   *
   * @param process the process model to layout
   */
  private static void layoutElements(RuleFlowProcess process) {
    //TODO: support gateway layouting.
    //TODO: Layout more pretty.
    int height;
    int width;

    int nodeWidth = 100;
    int nodeHeight = 100;
    int eventHeight = 48;
    int eventWidth = 48;
    int magicOffset = 8;

    int offsetX = 40;
    int offsetY;

    for (int i = 0; process.getNodes().length > i; i++) {
      Node node = process.getNodes()[i];

      if (node instanceof StartNode || node instanceof EndNode || node instanceof EventNode || node instanceof FaultNode
          || node instanceof TimerNode
          || node instanceof Join
          || node instanceof Split) {
        offsetY = ((nodeHeight - eventHeight) / 2) + magicOffset;
        height = eventHeight;
        width = eventWidth;
      } else {
        offsetY = 0;
        height = nodeHeight;
        width = nodeWidth;
      }
      int x = nodeWidth * i + offsetX * i + nodeWidth;
      int y = nodeHeight + offsetY;

      node.getMetaData().put("x", x);
      node.getMetaData().put("y", y);
      node.getMetaData().put("height", height);
      node.getMetaData().put("width", width);
    }
  }
}
