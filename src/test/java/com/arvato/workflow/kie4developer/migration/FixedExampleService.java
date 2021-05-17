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
