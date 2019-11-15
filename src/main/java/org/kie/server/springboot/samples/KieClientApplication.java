package org.kie.server.springboot.samples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class KieClientApplication {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(KieClientApplication.class);
    
    public static void main(String[] args) {
        SpringApplication.run(KieClientApplication.class, args);
    }

    @Bean
    CommandLineRunner deployAndValidate() {
        return new CommandLineRunner() {

            @Override
            public void run(String... strings) {
                //execute this to setup a test kie env: docker run -p 8080:8080 -p 8001:8001 -d --name jbpm-workbench jboss/jbpm-workbench-showcase:7.15.0.Final && docker run -p 8180:8080 -d --name kie-server --link jbpm-workbench:kie-wb jboss/kie-server-showcase:7.15.0.Final  && docker logs -f kie-server
                LOGGER.info("KieClient started");
            }

        };
    }
    
}
