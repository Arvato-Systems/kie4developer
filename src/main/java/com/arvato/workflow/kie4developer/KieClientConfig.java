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
