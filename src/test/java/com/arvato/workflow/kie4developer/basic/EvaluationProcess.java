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
package com.arvato.workflow.kie4developer.basic;

import com.arvato.workflow.kie4developer.common.interfaces.IDeployableBPMNProcess;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.jbpm.ruleflow.core.RuleFlowProcessFactory;
import org.kie.api.io.Resource;
import org.kie.internal.io.ResourceFactory;

/**
 * This is a {@link IDeployableBPMNProcess} bpmn process model that is based on a xml bpmn file.
 *
 * @see RuleFlowProcessFactory
 */
public class EvaluationProcess implements IDeployableBPMNProcess {

  private static final String VERSION = "1.0";
  private static final String PROCESSNAME = "evaluation"; // comes from the xml
  private static final String PACKAGENAME = "org.jbpm.test"; // comes from the xml
  private static final String BPMN_FILE = "/processdefinitions/example/evaluation.bpmn2";

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public String getName() {
    return PROCESSNAME;
  }

  @Override
  public String getPackage() {
    return PACKAGENAME;
  }

  @Override
  public void buildBPMNModel(RuleFlowProcessFactory factory) {
    // no impl. required - we use #getBPMNModelAsRessource to load the plain .bpmn file
  }

  @Override
  public Resource buildBPMNModel() {
    Resource res;
    try {
      InputStream is = getClass().getResourceAsStream(BPMN_FILE);
      res = ResourceFactory.newByteArrayResource(IOUtils.toByteArray(is));
      res.setSourcePath(getProcessId() + ".bpmn2"); // source path or target path must be set to be added into kbase
    } catch (IOException e) {
      throw new RuntimeException(String.format("Can't read Process Model file: %s", BPMN_FILE), e);
    } catch (NullPointerException e) {
      throw new RuntimeException(String.format("Process Model file not found: %s", BPMN_FILE), e);
    }
    return res;
  }

}
