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
