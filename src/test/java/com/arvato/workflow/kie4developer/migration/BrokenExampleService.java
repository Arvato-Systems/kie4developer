package com.arvato.workflow.kie4developer.migration;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Simple fake service for performing business logic. Throws RuntimeException when parameter <code>shouldFail</code> is
 * <code>true</code>.
 */
@Component
public class BrokenExampleService {

  private static final Logger LOGGER = LoggerFactory.getLogger(BrokenExampleService.class);

  /**
   * Dummy business logic
   */
  @SuppressWarnings("squid:S112")
  public void doIt(Map<String, Object> params) {
    LOGGER
        .debug("Try to execute Java class {} with method {} with parameter value {}.", this.getClass().getSimpleName(),
            "doIt", params);
    if (params.containsKey("shouldFail") && params.get("shouldFail").equals("true")) {
      throw new RuntimeException("error while executing business logic");
    }
    LOGGER.info("successfully executed business logic");
  }

}
