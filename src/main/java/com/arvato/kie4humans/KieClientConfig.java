package com.arvato.kie4humans;

import org.kie.server.api.KieServerConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Common configuration for the KIE Client
 *
 * @author TRIBE01
 */
@Configuration
public class KieClientConfig {

  public KieClientConfig(@Value("${kieworkbench.user}") String kieWorkbenchUser,
      @Value("${kieworkbench.pwd}") String kieWorkbenchPwd,
      @Value("${org.kie.server.bypass.auth.user}") String bypassAuth) {

    /**
     * Configure the connection settings to the kie workbench connection here,
     * cause the spring boot starter for kie-server does'nt look into the application.properties
     */
    System.setProperty(KieServerConstants.CFG_KIE_CONTROLLER_USER, kieWorkbenchUser);
    System.setProperty(KieServerConstants.CFG_KIE_CONTROLLER_PASSWORD, kieWorkbenchPwd);

    /**
     * Configure the security settings for working with human tasks here,
     * cause the spring boot starter for kie-server does'nt look into the application.properties
     */
    System.setProperty(KieServerConstants.CFG_BYPASS_AUTH_USER, bypassAuth);
  }

}
