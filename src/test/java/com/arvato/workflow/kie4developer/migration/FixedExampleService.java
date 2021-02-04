package com.arvato.workflow.kie4developer.migration;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Simple fake service for performing business logic. Always finish successfully.
 */
@Component
public class FixedExampleService {

  private static final Logger LOGGER = LoggerFactory.getLogger(FixedExampleService.class);

  /**
   * dummy business logic
   * @param params
   */
  public void doIt(Map<String, Object> params) {
    LOGGER
        .debug("Try to execute Java class {} with method {} with parameter value {}.", this.getClass().getSimpleName(),
            "doIt", params);
    LOGGER.info("successfully executed business logic");
  }

}
