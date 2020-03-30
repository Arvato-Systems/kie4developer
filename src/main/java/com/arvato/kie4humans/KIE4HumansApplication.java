package com.arvato.kie4humans;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * KIE4Humans Application
 *
 * @author TRIBE01
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.arvato.kie4humans"})
public class KIE4HumansApplication {

  public static void main(String[] args) {
    SpringApplication.run(KIE4HumansApplication.class, args);
  }

}
