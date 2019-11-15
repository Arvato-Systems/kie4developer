package org.kie.server.springboot.samples.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.server.common.rest.NoEndpointFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class KIEClientApplicationTests {

  @Autowired
  private KieClient kieClient;

  @Test
  public void testStartup() {
    assertNotNull("KIE Client can't be found", kieClient);
    try{
      kieClient.getKieServicesClient().getServerInfo().getResult();
    }catch(NoEndpointFoundException e){
      throw new RuntimeException("Please start a KIE Server & KIE Workbench for the test: docker run -p 8080:8080 -p 8001:8001 -d --name jbpm-workbench jboss/jbpm-workbench-showcase:7.15.0.Final && docker run -p 8180:8080 -d --name kie-server --link jbpm-workbench:kie-wb jboss/kie-server-showcase:7.15.0.Final  && docker logs -f kie-server",e);
    }
    assertTrue("KIE Server is not able to handle BPMN", kieClient.getKieServicesClient().getServerInfo().getResult().getCapabilities().contains("BPM"));
  }

}
