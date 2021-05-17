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
package com.arvato.workflow.kie4developer.migration;

import com.arvato.workflow.kie4developer.basic.HelloWorldProcess;
import com.arvato.workflow.kie4developer.basic.HelloWorldWorkItemHandler;
import com.arvato.workflow.kie4developer.common.interfaces.IDeployableBPMNProcess;
import org.jbpm.process.core.datatype.impl.type.StringDataType;
import org.jbpm.ruleflow.core.RuleFlowProcessFactory;

/**
 * This is a {@link IDeployableBPMNProcess} bpmn process model that is build with fluent api.
 *
 * @see RuleFlowProcessFactory
 */
public class HelloWorldProcessV3 implements IDeployableBPMNProcess {

  private static final String VERSION = "1.2";
  private static final String MY_VAR = "myvar";
  private static final String GLOBAL_VAR = "test";

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public String getName() {
    return HelloWorldProcess.class.getSimpleName(); // fake the process that it looks like HelloWordProcess
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
        .action("java", "kcontext.setVariable(\"myvar\", \"Buen día\");")
        .done()

        .workItemNode(5).name("Work Item 1")
        .workName(HelloWorldWorkItemHandler.class.getSimpleName())
        //.workParameter("helloworldtext", "Hello") // this can be used to provide constants
        //.workParameter("helloworldtext", "#{myvar}") // this can be used as alternative to inMapping
        .inMapping("helloworldtext", MY_VAR) // optional
        .outMapping("result", MY_VAR) // optional
        .done()

        .actionNode(6).name("Java Action 4")
        .action("java", "System.out.println(kcontext.getVariable(\"myvar\"));")
        .done()

        // ID 7 was removed

        // extension
        .humanTaskNode(8).name("Human Task 2")
        .actorId("john")
        .taskName("John's new task")
        .done()

        .endNode(9).name("End")
        .done()

        // connections
        .connection(1, 2)
        .connection(2, 3)
        .connection(3, 4)
        .connection(4, 5)
        .connection(5, 6)
        .connection(6, 8)
        .connection(8, 9);
  }

}
