KIE4Developer - Work with KIE the easy way
========================================

Library for creating and deploying [JBPM](https://www.jbpm.org) workflows using [Java](https://java.com).

- Create BPMN Processes (Process Definitions) as simple Java classes with fluent API or use existing .bpmn xml-files
- Write simple Java classes for executing business logic in service tasks
- Unittest the BPMN Processes with JUnit on your local system
- Package a release with all dependencies as single self-executable jar artifact
- Deploy to a KIE Server with selectable deployment strategies e.g. overwrite or migration

How to configure it[](how-to-configure-it)
------------------------------

You can use KIE4Developer to build and deploy your own workflow solutions.
On startup a deployment unit (kjar) for the JBPM Runtime is generated which can be executed on the embedded KIE Server (default) or on a remote Server.

Complete configuration is done via [application.properties](src/main/resources/application.properties) file.

To use a remote server just configure the connection properties to a non-local server via following properties:
```properties
kieserver.protocol=http
kieserver.host=externalhost
kieserver.port=8180
kieserver.user=kieserver
kieserver.pwd=kieserver1!
```

When connect to a remote KIE Server you need a remote KIE Workbench (alias Business Central) as well which can be connected via following properties:
```properties
kieworkbench.protocol=http
kieworkbench.host=localhost
kieworkbench.port=8080
kieworkbench.context=business-central
kieworkbench.user=admin
kieworkbench.pwd=admin
```

How to build it
------------------------------

Ensure that the default installed java version in the environment is JDK 8.

To create the KIE4Developer jar run `mvn:install`.

How to run it
------------------------------

Please read the chapter [How to configure it](#how-to-configure-it) first.

You can run the application by simply starting `mvn clean spring-boot:run`.

If you like to use the KIE Server REST API of the embedded KIE Server you can access it via Swagger UI:
http://localhost:8180/kie-server/services/rest/api-docs?url=/kie-server/services/rest/swagger.json with these credentials: `kieserver`/`kieserver1!`

If you like to work with external KIE Server and Business Central please run this separately.
E.g. use the existing docker images of [business-central-workbench-showcase](https://hub.docker.com/r/jboss/business-central-workbench-showcase) and [kie-server-showcase](https://hub.docker.com/r/jboss/kie-server-showcase):

```shell
# Run Business Central
docker run -p 8080:8080 -p 8001:8001 -d --name jbpm-workbench jboss/business-central-workbench-showcase:7.23.0.Final
# URL & Credentials (start-up takes some time...):
# - http://localhost:8080/business-central
# - admin/admin

# Run KIE-Server
docker run -p 8180:8080 -d --env "JAVA_OPTS=-Xms256m -Xmx1024m -Djava.net.preferIPv4Stack=true -Dorg.kie.server.bypass.auth.user=true" --name kie-server --link jbpm-workbench:kie-wb jboss/kie-server-showcase:7.23.0.Final
# URL & Credentials (start-up takes some time...):
# - http://localhost:8180/kie-server/docs/
# - kieserver/kieserver1!
```

How to implement your own workflow solution
------------------------------

Add the KIE4Developer lib as dependency to your maven project:
```xml
 <!-- attention: not hosted within maven central -->
 <dependency>
    <groupId>com.arvato.workflow</groupId>
    <artifactId>kie4developer</artifactId>
    <version>7.23.0.Final</version>
    <scope>provided</scope>
 </dependency>
 
 <!-- optional BIC Cloud Workflow libs -->
 <dependency>
    <groupId>com.arvato.biccw</groupId>
    <artifactId>biccw-jbpm-email-listener</artifactId>
    <version>1.7.0</version>
 </dependency>
 <dependency>
    <groupId>com.arvato.biccw</groupId>
    <artifactId>biccw-jbpm-listener</artifactId>
    <version>1.7.0</version>
 </dependency>
```

Add the plugin to create the executable Spring Boot fat jar:
```xml
  <build>
    <plugins>
      <!-- create a executable jar and a jar with dependencies to use in child-projects -->
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <version>${version.org.spring-boot}</version>
        <configuration>
          <classifier>exec</classifier>
          <executable>true</executable>
        </configuration>
        <executions>
          <execution>
            <goals>
              <goal>repackage</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```

Write a simple startup class that triggers the deployment:
```java
/**
 * Example Workflow Application (powered by KIE4Developer)
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.arvato.workflow.kie4developer", "com.arvato.example.workflow"})
public class WorkflowApplication extends KIE4DeveloperApplication {

  private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowApplication.class);
  @Value("${spring.application.autodeploy}")
  private Boolean autodeploy;
  @Value("${spring.application.autodeploy.overwrite}")
  private Boolean overwrite;
  @Value("${spring.application.automigrate.oldContainerId}")
  private String oldContainerId;

  public static void main(String[] args) {
    ConfigurableApplicationContext app = SpringApplication.run(WorkflowApplication.class, args);
    app.close(); // shutdown after deployment to external server has taken place
  }

  @Bean
  CommandLineRunner deployAndMigrateOnStart(IDeploymentHelper clientDeploymentHelper) {
    return (String... strings) -> {
      LOGGER.info("KIE4Developer Settings: Autodeploy={}, Overwrite={}, MigrationContainerId={}", autodeploy, overwrite, oldContainerId);
      boolean success = true;
      if (autodeploy.booleanValue()) {
        success = clientDeploymentHelper.deploy(overwrite);
      } else if (oldContainerId != null && oldContainerId.length() > 0) {
        for (MigrationReportInstance migrationReportInstance : clientDeploymentHelper.deployWithMigration(oldContainerId)) {
          if (!migrationReportInstance.isSuccessful()) {
            success = false;
          }
        }
      }
      if (!success) {
        System.exit(1);
      }
    };
  }

}
```

Do a test run: `mvn clean spring-boot:run.`

Start writing your individual Business Processes:
- Add BPMN Process Model classes by implementing the ```IDeployableBPMNProcess``` Interface. Store them in the package ```com.example.workflow.processes```.
- Add required dependencies by add them into  ```pom.xml```.
- Create Service classes that runs your business logic by providing the default constructur and store them in the package ```com.example.workflow.services```.
- If you use custom classes within your process models make sure these implements ```Serializable``` for marshalling support.
- Add own WorkItemHandler by implementing the ```IDeployableWorkItemHandler``` Interface. Store them in the package ```com.example.workflow.workitemhandler```.
- Check out many examples within the ```test``` directory.

To create your release as Spring Boot fat jar just run `mvn:install`.

To run the app / trigger the deployment just run `java -jar example-1.0.0-SNAPSHOT-exec.jar`.

You can overwrite properties directly on the command line e.g.: `java -jar example-1.0.0-SNAPSHOT-exec.jar --spring.application.autodeploy=true --spring.application.autodeploy.overwrite=true`.

Licence
------------------------------
This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

KIE4Developer makes use of following third party libraries:
- [kie-server-spring-boot-starter](https://github.com/kiegroup/droolsjbpm-integration/tree/master/kie-spring-boot/kie-spring-boot-starters/kie-server-spring-boot-starter-jbpm) [Apache 2.0 License]
- [kie-server-client](https://github.com/kiegroup/droolsjbpm-integration/tree/master/kie-server-parent/kie-server-remote/kie-server-client) [Apache 2.0 License]
- [jbpm-flow](https://github.com/kiegroup/jbpm/tree/master/jbpm-flow) [Apache 2.0 License]
- [jbpm-bpmn2](https://github.com/kiegroup/jbpm/tree/master/jbpm-bpmn2) [Apache 2.0 License]
- [tomcat-jdbc](https://github.com/apache/tomcat/tree/main/modules/jdbc-pool) [Apache 2.0 License]
- [spring-boot-starter-security](https://github.com/spring-projects/spring-boot/tree/main/spring-boot-project/spring-boot-starters/spring-boot-starter-security) [Apache 2.0 License]
- [swagger-ui](https://github.com/swagger-api/swagger-ui) [Apache 2.0 License]
- [cxf-rt-rs-service-description-swagger](https://github.com/apache/cxf/tree/master/rt/rs/description-swagger) [Apache 2.0 License]
- [batik-all](https://github.com/apache/xmlgraphics-batik) [Apache 2.0 License]
- [h2](https://github.com/h2database/h2database) [[Eclipse Public License 1.0](https://github.com/h2database/h2database/blob/master/LICENSE.txt#L337)]
- [spring-boot-starter-test](https://github.com/spring-projects/spring-boot/tree/main/spring-boot-project/spring-boot-starters/spring-boot-starter-test) [Apache 2.0 License]
- [mockito-core](https://github.com/mockito/mockito) [[MIT License](https://github.com/mockito/mockito/blob/main/LICENSE)]
- [junit](https://github.com/junit-team/junit4) [[Eclipse Public License 1.0](https://github.com/junit-team/junit4/blob/main/LICENSE-junit.txt)]
