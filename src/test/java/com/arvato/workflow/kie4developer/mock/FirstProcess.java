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
package com.arvato.workflow.kie4developer.mock;

import com.arvato.workflow.kie4developer.common.interfaces.IDeployableBPMNProcess;
import org.jbpm.ruleflow.core.RuleFlowProcessFactory;

/**
 * This is a {@link IDeployableBPMNProcess} bpmn process model that is build with fluent api.
 *
 * @see RuleFlowProcessFactory
 */
public class FirstProcess implements IDeployableBPMNProcess {

  private static final String VERSION = "1.0";

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

        // nodes
        .startNode(1).name("Start")
        .done()

        .actionNode(2).name("Java Action 1")
        .action("java", "System.out.println(\"Hello First Process!\");")
        .done()

        .subProcessNode(3)
        .name(new SecondProcess().getName())
        .processId(new SecondProcess().getProcessId())
        .waitForCompletion(true)
        .independent(false)
        .done()

        .endNode(4).name("End")
        .done()

        // connections
        .connection(1, 2)
        .connection(2, 3)
        .connection(3, 4);
  }

}
