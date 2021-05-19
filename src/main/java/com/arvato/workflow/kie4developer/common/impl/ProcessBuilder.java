/*
 * Copyright 2021 Arvato Systems GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arvato.workflow.kie4developer.common.impl;

import com.arvato.workflow.kie4developer.common.interfaces.IDeployableBPMNProcess;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jbpm.bpmn2.xml.XmlBPMNProcessDumper;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.jbpm.ruleflow.core.RuleFlowProcessFactory;
import org.jbpm.workflow.core.node.ActionNode;
import org.jbpm.workflow.core.node.EndNode;
import org.jbpm.workflow.core.node.EventNode;
import org.jbpm.workflow.core.node.HumanTaskNode;
import org.jbpm.workflow.core.node.Join;
import org.jbpm.workflow.core.node.Split;
import org.jbpm.workflow.core.node.StartNode;
import org.jbpm.workflow.core.node.SubProcessNode;
import org.jbpm.workflow.core.node.TimerNode;
import org.jbpm.workflow.core.node.WorkItemNode;
import org.kie.api.definition.process.Node;
import org.kie.api.io.Resource;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder to create BPM Process Definitions
 *
 * @author TRIBE01
 */
public class ProcessBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessBuilder.class);

  private ProcessBuilder() {
  }

  /**
   * Build the BPM Process Definition (XML) and Image (SVG)
   *
   * @param deployableBPMNProcess the Business Process
   * @return the layouted BPM Process Definition Resource
   */
  public static List<Resource> build(IDeployableBPMNProcess deployableBPMNProcess) throws IOException {
    List<Resource> resList = new ArrayList<>();
    Resource res = deployableBPMNProcess.buildBPMNModel();
    if (res == null) {
      RuleFlowProcessFactory factory = RuleFlowProcessFactory.createProcess(deployableBPMNProcess.getProcessId());
      deployableBPMNProcess.buildBPMNModel(factory);

      // validate process model
      RuleFlowProcess process = factory.validate().getProcess();

      // give any process model node a unique id with prefix for later process migrations
      for (Node node : process.getNodes()) {
        node.getMetaData().put("UniqueId", "_jbpm-unique-" + node.getId());
      }

      // layout the process model nodes
      try {
        layoutElements(process);
      } catch (Exception e) {
        LOGGER.error("Error while creating process model layout", e);
      }

      // generate the svg representation
      try {
        Resource processImageResource = ResourceFactory.newByteArrayResource(ProcessImageBuilder.createImage(process));
        processImageResource.setSourcePath(deployableBPMNProcess.getProcessId() + "-svg.svg"); // source path or target path must be set to be added into kbase
        resList.add(processImageResource);
      } catch (Exception e) {
        LOGGER.error("Error while creating process model image", e);
      }

      // generate the xml representation
      res = ResourceFactory.newByteArrayResource(XmlBPMNProcessDumper.INSTANCE.dump(process).getBytes());
    } else {
      // add related process image
      Resource processImageResource = deployableBPMNProcess.buildBPMNModelImage();
      if (processImageResource != null) {
        processImageResource.setSourcePath(deployableBPMNProcess.getProcessId()
            + ".svg"); // source path or target path must be set to be added into kbase
        resList.add(processImageResource);
      } else {
        LOGGER.warn("No process image found for process model with id " + deployableBPMNProcess.getProcessId());
      }
    }

    res.setSourcePath(deployableBPMNProcess.getProcessId()
        + ".bpmn2"); // source path or target path must be set to be added into kbase
    resList.add(res);

    return resList;
  }

  // space between elements
  static final int DEFAULT_SPACE_X = 70;
  static final int DEFAULT_SPACE_Y = 70;

  // start position
  static final int START_X = 100;
  static final int START_Y = 100;

  /**
   * Auto layout the process model nodes of a given process
   *
   * @param process the process model to layout
   */
  private static void layoutElements(RuleFlowProcess process) {
    LOGGER.debug("Layouting process with id '{}'", process.getId());
    //TODO: Support attached Events
    //TODO: Layout more pretty.
    int height = 38;
    int width = 38;

    // bpmn.io node sizes
    final int eventHeight = 38;
    final int eventWidth = 38;
    final int gatewayHeight = 54;
    final int gatewayWidth = 54;
    final int activityHeight = 82;
    final int activityWidth = 102;

    final int[] allHeights = {eventHeight, gatewayHeight, activityHeight};
    final int maxHeight = Arrays.stream(allHeights).max().getAsInt();
    final int eventOffsetY = (maxHeight - eventHeight) / 2;
    final int gatewayOffsetY = (maxHeight - gatewayHeight) / 2;
    final int activityOffsetY = (maxHeight - activityHeight) / 2;

    // calculation coordinates
    int x;
    int y;
    int offsetX;
    int offsetY;
    int numberOfOpenGateways;

    // let's build the left-to-right graph
    numberOfOpenGateways = 0;
    x = START_X;
    y = START_Y;
    for (int i = 0; process.getNodes().length > i; i++) {
      Node node = process.getNodes()[i];
      offsetX = DEFAULT_SPACE_X;
      offsetY = numberOfOpenGateways * DEFAULT_SPACE_Y;

      if (node instanceof StartNode || node instanceof EndNode || node instanceof TimerNode || node instanceof EventNode) {
        height = eventHeight;
        width = eventWidth;
        offsetY += eventOffsetY;
      } else if (node instanceof Join || node instanceof Split) {
        height = gatewayHeight;
        width = gatewayWidth;
        offsetY += gatewayOffsetY;

        if (node instanceof Split) {
          numberOfOpenGateways++;
        } else {
          numberOfOpenGateways--;
          offsetY -= DEFAULT_SPACE_Y; //works not for deeper nested branches
        }
      } else if (node instanceof ActionNode || node instanceof WorkItemNode || node instanceof HumanTaskNode
          || node instanceof SubProcessNode) {
        height = activityHeight;
        width = activityWidth;
        offsetY += activityOffsetY;
      } else {
        LOGGER.warn(String.format("unsupported node with id '%s' on process with id '%s'", node.getId(), process.getId()));
      }
      node.getMetaData().put("x", x + offsetX);
      node.getMetaData().put("y", y + offsetY);
      node.getMetaData().put("height", height);
      node.getMetaData().put("width", width);

      x += width + offsetX;
    }
  }

}
