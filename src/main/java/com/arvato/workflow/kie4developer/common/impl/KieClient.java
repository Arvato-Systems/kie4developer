package com.arvato.workflow.kie4developer.common.impl;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.client.DocumentServicesClient;
import org.kie.server.client.JobServicesClient;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.ProcessServicesClient;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.client.UIServicesClient;
import org.kie.server.client.UserTaskServicesClient;
import org.kie.server.client.admin.ProcessAdminServicesClient;
import org.kie.server.client.admin.UserTaskAdminServicesClient;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Access to all KIE server clients
 *
 * @author TRIBE01
 */
@Component
public class KieClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(KieClient.class);
  private KieServicesConfiguration configuration;
  private KieServicesClient kieServicesClient;
  private Environment environment;
  // kie server connection parameter
  private String kieServerUrl;
  private String kieServerUser;
  private String kieServerPwd;
  private Long timeout;
  private String serializationGroupId;

  public KieClient(Environment environment, @Value("${kieserver.location}") String kieServerUrl,
      @Value("${kieserver.user}") String kieServerUser, @Value("${kieserver.pwd}") String kieServerPwd,
      @Value("${org.kie.server.timeout}") Long timeout,
      @Value("${spring.application.groupid.serialization}") String serializationGroupId) {
    this.environment = environment;
    this.kieServerUrl = kieServerUrl;
    this.kieServerUser = kieServerUser;
    this.kieServerPwd = kieServerPwd;
    this.timeout = timeout;
    this.serializationGroupId = serializationGroupId;
  }

  public KieServicesClient getKieServicesClient() {
    if (kieServicesClient == null) {
      LOGGER.info("Connecting to KIE-Server...");
      if (kieServerUrl.contains(":0")) {
        String rndPort = environment.getProperty("local.server.port");
        kieServerUrl = kieServerUrl.replace(":0", ":" + rndPort);
      }
      configuration = KieServicesFactory.newRestConfiguration(kieServerUrl, kieServerUser, kieServerPwd);
      configuration.setTimeout(timeout); // default is 5s
      configuration.setMarshallingFormat(MarshallingFormat.JSON);
      Set<Class<?>> customJAXBClasses = new HashSet<>();
      Reflections ref = new Reflections(serializationGroupId);
      for (Class<?> cl : ref.getSubTypesOf(Serializable.class)) {
        if (cl.getName().startsWith(serializationGroupId)) {
          customJAXBClasses.add(cl);
        }
      }
      configuration.setExtraClasses(customJAXBClasses);
      kieServicesClient = KieServicesFactory.newKieServicesClient(configuration);
      LOGGER.info("Connection established");
    }
    return kieServicesClient;
  }

  public QueryServicesClient getQueryClient() {
    return getKieServicesClient().getServicesClient(QueryServicesClient.class);
  }

  public JobServicesClient getJobServicesClient() {
    return getKieServicesClient().getServicesClient(JobServicesClient.class);
  }

  public DocumentServicesClient getDocumentClient() {
    return getKieServicesClient().getServicesClient(DocumentServicesClient.class);
  }

  public UIServicesClient getUiServicesClient() {
    return getKieServicesClient().getServicesClient(UIServicesClient.class);
  }

  public ProcessAdminServicesClient getProcessAdminClient() {
    return getKieServicesClient().getServicesClient(ProcessAdminServicesClient.class);
  }

  public UserTaskAdminServicesClient getUserTaskAdminClient() {
    return getKieServicesClient().getServicesClient(UserTaskAdminServicesClient.class);
  }

  public ProcessServicesClient getProcessClient() {
    return getKieServicesClient().getServicesClient(ProcessServicesClient.class);
  }

  public UserTaskServicesClient getTaskClient() {
    return getKieServicesClient().getServicesClient(UserTaskServicesClient.class);
  }

}
