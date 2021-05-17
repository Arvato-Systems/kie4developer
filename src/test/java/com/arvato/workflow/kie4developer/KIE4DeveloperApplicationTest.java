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
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.application.autodeploy=false"
    })
public class KIE4DeveloperApplicationTest {

  @Autowired
  private KieServer kieServer;
  @Autowired
  private KieClient kieClient;

  // kie workbench connection
  @Value("${kieworkbench.protocol}")
  private String kieWorkbenchProtocol;
  @Value("${kieworkbench.host}")
  private String kieWorkbenchHost;
  @Value("${kieworkbench.port}")
  private String kieWorkbenchPort;
  @Value("${kieworkbench.context}")
  private String kieWorkbenchContext;

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
    if (!kieWorkbenchHost.contains("localhost") && !kieWorkbenchHost.contains("127.0.0.1")) {
      try {
        String kieWorkbenchUrl =
            kieWorkbenchProtocol + "://" + kieWorkbenchHost + ":" + kieWorkbenchPort + "/" + kieWorkbenchContext
                + "/kie-wb.jsp";
        ResponseEntity<String> kieWorkbenchWebsite = new RestTemplate().getForEntity(kieWorkbenchUrl, String.class);
        assertTrue("KIE Workbench website is not available",
            kieWorkbenchWebsite.getBody().contains("Business Central"));
      } catch (ResourceAccessException e) {
        fail(
            "Connection to KIE Workbench can't be established. Please start a external workbench: docker run -p 8080:8080 -p 8001:8001 -d --name jbpm-workbench jboss/jbpm-workbench-showcase:7.23.0.Final && docker logs -f jbpm-workbench");
      }
    }
  }

}
