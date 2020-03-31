package com.arvato.workflow.kie4developer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.arvato.workflow.kie4developer.common.impl.KieClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.server.common.rest.NoEndpointFoundException;
import org.kie.server.services.api.KieServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
public class KIE4DeveloperApplicationTest {

  @Autowired
  private KieServer kieServer;
  @Autowired
  private KieClient kieClient;
  // kie workbench connection
  @Value("${kieworkbench.protocol}")
  String kieWorkbenchProtocol;
  @Value("${kieworkbench.host}")
  String kieWorkbenchHost;
  @Value("${kieworkbench.port}")
  String kieWorkbenchPort;
  @Value("${kieworkbench.context}")
  String kieWorkbenchContext;

  @Test
  public void testStartupOfIntegratedKIEServer() {
    assertNotNull("KIE Server can't be found", kieServer);
    assertTrue("KIE Server is not able to handle BPMN",
        kieServer.getInfo().getResult().getCapabilities().contains("BPM"));
  }

  @Test
  public void testConnectionToKIEServer() {
    assertNotNull("KIE Client can't be found", kieClient);
    try {
      kieClient.getKieServicesClient().getServerInfo().getResult();
    } catch (NoEndpointFoundException e) {
      fail("Connection to KIE Server can't be established. Please check the connection properties");
    }
    assertTrue("KIE Server is not able to handle BPMN",
        kieClient.getKieServicesClient().getServerInfo().getResult().getCapabilities().contains("BPM"));
    assertEquals("KIE Server is not able to handle human tasks with bypass auth", "true",
        kieClient.getKieServicesClient().getServerState().getResult().getConfiguration()
            .getConfigItem("org.kie.server.bypass.auth.user").getValue());
  }

  @Test
  public void testConnectionToKIEWorkbench() {
    try {
      String kieWorkbenchUrl =
          kieWorkbenchProtocol + "://" + kieWorkbenchHost + ":" + kieWorkbenchPort + "/" + kieWorkbenchContext;
      ResponseEntity<String> kieWorkbenchWebsite = new RestTemplate().getForEntity(kieWorkbenchUrl, String.class);
      assertTrue("KIE Workbench website is not available", kieWorkbenchWebsite.getBody().contains("Business Central"));
    } catch (ResourceAccessException e) {
      fail("Connection to KIE Workbench can't be established. "
          + "Please start a external workbench: docker run -p 8080:8080 -p 8001:8001 -d --name jbpm-workbench jboss/jbpm-workbench-showcase:7.15.0.Final");
    }
  }

}
