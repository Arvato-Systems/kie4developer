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
