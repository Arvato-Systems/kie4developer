package com.arvato.kie4developer.common.impl.kjar;

import java.io.File;
import java.nio.charset.Charset;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Helper to handle jar/kjar uploads to KIE workbench
 *
 * @author TRIBE01
 */
@Component
public class JarUploader {

  private static final Logger LOGGER = LoggerFactory.getLogger(JarUploader.class);
  @Value("${kieworkbench.user}")
  private String workbenchUser;
  @Value("${kieworkbench.pwd}")
  private String workbenchPassword;

  /**
   * Upload a single file into the kie-workbench by using Basic Auth
   *
   * @param file the file to upload
   * @param url  the target url
   * @return the http response
   */
  public ResponseEntity<String> uploadFile(File file, String url) {
    LOGGER.debug("uploading {} to {}", file, url);
    // Build header with basic auth & file upload http content type
    HttpHeaders headers = buildAuthHeader();
    // add the file to request
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", new FileSystemResource(file));
    // do the http post call
    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
    RestTemplate restTemplate = new RestTemplate();
    return restTemplate.postForEntity(url, requestEntity, String.class);
  }

  /**
   * Build http header with basic auth & file upload http content type
   *
   * @return the basic authentication header
   */
  private HttpHeaders buildAuthHeader() {
    return new HttpHeaders() {{
      String auth = workbenchUser + ":" + workbenchPassword;
      byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
      String authHeader = "Basic " + new String(encodedAuth);
      set("Authorization", authHeader);
      setContentType(MediaType.MULTIPART_FORM_DATA);
    }};
  }

}
