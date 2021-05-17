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
package com.arvato.workflow.kie4developer.dependency;

import java.util.HashMap;
import java.util.Map;
import org.apache.batik.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Simple Service that use the external Apache Batik dependency dependency.
 */
@Component
public class DependencyService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DependencyService.class);

  public Map<String, Object> create() {
    Map<String, Object> result = new HashMap<>();
    String batikVersion = Version.getVersion();
    result.put("serviceResponse", batikVersion);
    return result;
  }

  public Map<String, Object> modify(String batikVersion) {
    batikVersion+=" modified by service";
    Map<String, Object> result = new HashMap<>();
    result.put("serviceResponse", batikVersion);
    return result;
  }

  public void print(String batikVersion) {
    LOGGER.info("Found version: {}", batikVersion);
  }

}
