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
package com.arvato.workflow.kie4developer.common;

import com.arvato.workflow.kie4developer.basic.HelloWorldProcess;
import com.arvato.workflow.kie4developer.common.impl.ProcessBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jbpm.process.svg.SVGImageProcessor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class ProcessImageBuilderTest {

  @Test
  public void testBuildSVG() throws IOException {
    HelloWorldProcess helloWorldProcess = new HelloWorldProcess();
    List<Resource> build = ProcessBuilder.build(helloWorldProcess);
    Optional<Resource> svgImage = build.stream().filter((resource) -> resource.getSourcePath().endsWith(".svg")).findFirst();
    Assert.assertTrue("Generated svg image not found", svgImage.isPresent());

    InputStream resource = svgImage.get().getInputStream();
    List<String> completedSteps = new ArrayList<>();
    List<String> activeSteps = new ArrayList<>();

    String svgImageAsString = SVGImageProcessor.transform(resource, completedSteps, activeSteps);

    Assert.assertNotNull("Transforming svg image failed", svgImageAsString);
  }

}
