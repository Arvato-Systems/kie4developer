package com.arvato.workflow.kie4developer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * KIE4Developer Application
 *
 * @author TRIBE01
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.arvato.workflow.kie4developer"})
public class KIE4DeveloperApplication {

  public static void main(String[] args) {
    SpringApplication.run(KIE4DeveloperApplication.class, args);
  }

}
