package com.arvato.workflow.kie4developer;

import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfiguration {

  static {
    // change the optimizer to not generate negative IDs for entities on unittests
    System.setProperty("hibernate.id.optimizer.pooled.preferred", "pooled-lo");
  }

}
