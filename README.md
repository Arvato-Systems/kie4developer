KIE4Developer - Work with KIE the easy way
========================================

Library for creating and deploying [JBPM](https://www.jbpm.org) workflows using [Java](https://java.com).

- Create BPMN Processes (Process Definitions) as simple Java classes with fluent API or use existing .bpmn xml-files
- Use simple Java classes for executing business logic in service tasks
- Unittest the BPMN Processes with JUnit on your local system
- Package a release with all dependencies as single self-executable jar artifact
- Deploy to a KIE Server with selectable deployment strategies e.g. overwrite or migration

How to configure it[](how-to-configure-it)
------------------------------

Complete configuration is done via [application.properties](src/main/resources/application.properties) file.

You can use this lib as runtime target (included KIE Server -> default) or as vehicle for deloyments on a remote KIE Server.

To use a remote server just configure the connection properties to a non-local server via following properties:
```
kieserver.protocol=http
kieserver.host=externalhost
kieserver.port=8180
kieserver.user=kieserver
kieserver.pwd=kieserver1!
```

When connect to a remote KIE Server you need a remote KIE Workbench (alias Business Central) as well which can be connected via following properties:
```
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

To create the kie4developer jar run `mvn:install`.

How to run it
------------------------------

Please read the chapter [How to configure it](#how-to-configure-it) first.

You can run the application by simply starting

```
mvn clean spring-boot:run
```

If you like to use the KIE Server REST API:

```
# URL & Credentails (start-up takes some time...):
# Swagger UI: http://localhost:8180/kie-server/services/rest/api-docs?url=/kie-server/services/rest/swagger.json
# http://localhost:8180/kie-server/services/rest/server
# kieserver/kieserver1!
```

If you like to work with external KIE Server and Business Central please run this separately.
E.g. use the existing docker images of [business-central-workbench-showcase](https://hub.docker.com/r/jboss/business-central-workbench-showcase) and [kie-server-showcase](https://hub.docker.com/r/jboss/kie-server-showcase):

```
# Run Business Central
docker run -p 8080:8080 -p 8001:8001 -d --name jbpm-workbench jboss/business-central-workbench-showcase:7.23.0.Final
# URL & Credentails (start-up takes some time...):
# http://localhost:8080/business-central
# admin/admin

# Run KIE-Server
docker run -p 8180:8080 -d --env "JAVA_OPTS=-Xms256m -Xmx1024m -Djava.net.preferIPv4Stack=true -Dorg.kie.server.bypass.auth.user=true" --name kie-server --link jbpm-workbench:kie-wb jboss/kie-server-showcase:7.23.0.Final
# URL & Credentails (start-up takes some time...):
# http://localhost:8180/kie-server/docs/
# kieserver/kieserver1!
```

How to implement your own workflow solution
------------------------------

Add the kie4developer lib as maven dependency to your project:
```
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

Add the plugin to create the executable Spring Boot fat jar
```
  <build>
    <plugins>
      <plugin>
        <!-- create a executable jar and a jar with dependencies to use in child-projects -->
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
```
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
