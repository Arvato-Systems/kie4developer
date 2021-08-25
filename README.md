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
  @Value("${spring.application.automigrate}")
  private Boolean automigrate;
  @Value("${spring.application.automigrate.oldContainerId}")
  private String oldContainerId;

  public static void main(String[] args) {
    ConfigurableApplicationContext app = SpringApplication.run(WorkflowApplication.class, args);
    app.close(); // shutdown after deployment to external server has taken place
  }

  @Bean
  CommandLineRunner deployAndMigrateOnStart(IDeploymentHelper clientDeploymentHelper) {
    return (String... strings) -> {
      LOGGER.info("KIE4Developer Settings: Autodeploy={}, Overwrite={}, Automigrate={}, MigrationContainerId={}", autodeploy, overwrite, automigrate, oldContainerId);
      boolean success = true;
      if (autodeploy.booleanValue()){
        if (oldContainerId != null && oldContainerId.length() > 0) {
          for (MigrationReportInstance migrationReportInstance : clientDeploymentHelper.deployWithMigration(oldContainerId)) {
            if (!migrationReportInstance.isSuccessful()) {
              success = false;
            }
          }
        } else {
          success = clientDeploymentHelper.deploy(overwrite);
        }
      } else if (automigrate.booleanValue() && oldContainerId != null && oldContainerId.length() > 0){
        for (MigrationReportInstance migrationReportInstance : clientDeploymentHelper.migrate(oldContainerId)) {
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
- [kie-server-spring-boot-starter](https://github.com/kiegroup/droolsjbpm-integration/tree/master/kie-spring-boot/kie-spring-boot-starters/kie-server-spring-boot-starter-jbpm) [[Apache 2.0 License](https://github.com/kiegroup/droolsjbpm-integration/blob/master/LICENSE-Apache-2.0.txt)]
- [kie-server-client](https://github.com/kiegroup/droolsjbpm-integration/tree/master/kie-server-parent/kie-server-remote/kie-server-client) [[Apache 2.0 License](https://github.com/kiegroup/droolsjbpm-integration/blob/master/LICENSE-Apache-2.0.txt)]
- [jbpm-flow](https://github.com/kiegroup/jbpm/tree/master/jbpm-flow) [[Apache 2.0 License](https://github.com/kiegroup/jbpm/blob/master/LICENSE-Apache-2.0.txt)]
- [jbpm-bpmn2](https://github.com/kiegroup/jbpm/tree/master/jbpm-bpmn2) [[Apache 2.0 License](https://github.com/kiegroup/jbpm/blob/master/LICENSE-Apache-2.0.txt)]
- [tomcat-jdbc](https://github.com/apache/tomcat/tree/main/modules/jdbc-pool) [[Apache 2.0 License](https://github.com/apache/tomcat/blob/main/modules/jdbc-pool/LICENSE)]
- [spring-boot-starter-security](https://github.com/spring-projects/spring-boot/tree/main/spring-boot-project/spring-boot-starters/spring-boot-starter-security) [[Apache 2.0 License](https://github.com/spring-projects/spring-boot/blob/main/LICENSE.txt)]
- [swagger-ui](https://github.com/swagger-api/swagger-ui) [[Apache 2.0 License](https://github.com/swagger-api/swagger-ui/blob/master/LICENSE)]
- [cxf-rt-rs-service-description-swagger](https://github.com/apache/cxf/tree/master/rt/rs/description-swagger) [[Apache 2.0 License](https://github.com/apache/cxf/blob/master/LICENSE)]
- [batik-all](https://github.com/apache/xmlgraphics-batik) [[Apache 2.0 License](https://github.com/apache/xmlgraphics-batik/blob/trunk/batik-shared-resources/src/main/resources/LICENSE)]
- [h2](https://github.com/h2database/h2database) [[Eclipse Public License 1.0](https://github.com/h2database/h2database/blob/master/LICENSE.txt#L337)]
- [spring-boot-starter-test](https://github.com/spring-projects/spring-boot/tree/main/spring-boot-project/spring-boot-starters/spring-boot-starter-test) [[Apache 2.0 License](https://github.com/spring-projects/spring-boot/blob/main/LICENSE.txt)]
- [mockito-core](https://github.com/mockito/mockito) [[MIT License](https://github.com/mockito/mockito/blob/main/LICENSE)]
- [junit](https://github.com/junit-team/junit4) [[Eclipse Public License 1.0](https://github.com/junit-team/junit4/blob/main/LICENSE-junit.txt)]

Please notice that third party libraries under Eclipse Public Licence 1.0 (such as junit and h2) remains licensed under Eclipse Public License 1.0; they are NOT licensed under Apache License 2.0.
* The source code for junit you find [here](https://github.com/junit-team/junit4).
* The source code for h2 you find [here](https://github.com/h2database/h2database).

The applicable licence text of the Eclipse Public Licence 1.0 for the third party libraries junit and h2 you find below:
<details>
  <summary>Licence text for Eclipse Public License 1.0</summary>

THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE PUBLIC LICENSE (“AGREEMENT”). ANY USE, REPRODUCTION OR DISTRIBUTION OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.

### 1. Definitions

“Contribution” means:
* **a)** in the case of the initial Contributor, the initial code and documentation distributed under this Agreement, and
* **b)** in the case of each subsequent Contributor:
    * **i)** changes to the Program, and
    * **ii)** additions to the Program;
      where such changes and/or additions to the Program originate from and are distributed by that particular Contributor. A Contribution 'originates' from a Contributor if it was added to the Program by such Contributor itself or anyone acting on such Contributor's behalf. Contributions do not include additions to the Program which: **(i)** are separate modules of software distributed in conjunction with the Program under their own license agreement, and **(ii)** are not derivative works of the Program.

“Contributor” means any person or entity that distributes the Program.

“Licensed Patents ” mean patent claims licensable by a Contributor which are necessarily infringed by the use or sale of its Contribution alone or when combined with the Program.

“Program” means the Contributions distributed in accordance with this Agreement.

“Recipient” means anyone who receives the Program under this Agreement, including all Contributors.

### 2. Grant of Rights

**a)** Subject to the terms of this Agreement, each Contributor hereby grants Recipient a non-exclusive, worldwide, royalty-free copyright license to reproduce, prepare derivative works of, publicly display, publicly perform, distribute and sublicense the Contribution of such Contributor, if any, and such derivative works, in source code and object code form.

**b)** Subject to the terms of this Agreement, each Contributor hereby grants Recipient a non-exclusive, worldwide, royalty-free patent license under Licensed Patents to make, use, sell, offer to sell, import and otherwise transfer the Contribution of such Contributor, if any, in source code and object code form. This patent license shall apply to the combination of the Contribution and the Program if, at the time the Contribution is added by the Contributor, such addition of the Contribution causes such combination to be covered by the Licensed Patents. The patent license shall not apply to any other combinations which include the Contribution. No hardware per se is licensed hereunder.

**c)** Recipient understands that although each Contributor grants the licenses to its Contributions set forth herein, no assurances are provided by any Contributor that the Program does not infringe the patent or other intellectual property rights of any other entity. Each Contributor disclaims any liability to Recipient for claims brought by any other entity based on infringement of intellectual property rights or otherwise. As a condition to exercising the rights and licenses granted hereunder, each Recipient hereby assumes sole responsibility to secure any other intellectual property rights needed, if any. For example, if a third party patent license is required to allow Recipient to distribute the Program, it is Recipient's responsibility to acquire that license before distributing the Program.

**d)** Each Contributor represents that to its knowledge it has sufficient copyright rights in its Contribution, if any, to grant the copyright license set forth in this Agreement.

### 3. Requirements

A Contributor may choose to distribute the Program in object code form under its own license agreement, provided that:
* **a)** it complies with the terms and conditions of this Agreement; and
* **b)** its license agreement:
    * **i)** effectively disclaims on behalf of all Contributors all warranties and conditions, express and implied, including warranties or conditions of title and non-infringement, and implied warranties or conditions of merchantability and fitness for a particular purpose;
    * **ii)** effectively excludes on behalf of all Contributors all liability for damages, including direct, indirect, special, incidental and consequential damages, such as lost profits;
    * **iii)** states that any provisions which differ from this Agreement are offered by that Contributor alone and not by any other party; and
    * **iv)** states that source code for the Program is available from such Contributor, and informs licensees how to obtain it in a reasonable manner on or through a medium customarily used for software exchange.

When the Program is made available in source code form:
* **a)** it must be made available under this Agreement; and
* **b)** a copy of this Agreement must be included with each copy of the Program.

Contributors may not remove or alter any copyright notices contained within the Program.

Each Contributor must identify itself as the originator of its Contribution, if any, in a manner that reasonably allows subsequent Recipients to identify the originator of the Contribution.

### 4. Commercial Distribution

Commercial distributors of software may accept certain responsibilities with respect to end users, business partners and the like. While this license is intended to facilitate the commercial use of the Program, the Contributor who includes the Program in a commercial product offering should do so in a manner which does not create potential liability for other Contributors. Therefore, if a Contributor includes the Program in a commercial product offering, such Contributor (“Commercial Contributor”) hereby agrees to defend and indemnify every other Contributor (“Indemnified Contributor”) against any losses, damages and costs (collectively “Losses”) arising from claims, lawsuits and other legal actions brought by a third party against the Indemnified Contributor to the extent caused by the acts or omissions of such Commercial Contributor in connection with its distribution of the Program in a commercial product offering. The obligations in this section do not apply to any claims or Losses relating to any actual or alleged intellectual property infringement. In order to qualify, an Indemnified Contributor must: **a)** promptly notify the Commercial Contributor in writing of such claim, and **b)** allow the Commercial Contributor to control, and cooperate with the Commercial Contributor in, the defense and any related settlement negotiations. The Indemnified Contributor may participate in any such claim at its own expense.

For example, a Contributor might include the Program in a commercial product offering, Product X. That Contributor is then a Commercial Contributor. If that Commercial Contributor then makes performance claims, or offers warranties related to Product X, those performance claims and warranties are such Commercial Contributor's responsibility alone. Under this section, the Commercial Contributor would have to defend claims against the other Contributors related to those performance claims and warranties, and if a court requires any other Contributor to pay any damages as a result, the Commercial Contributor must pay those damages.

### 5. No Warranty

EXCEPT AS EXPRESSLY SET FORTH IN THIS AGREEMENT, THE PROGRAM IS PROVIDED ON AN “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED INCLUDING, WITHOUT LIMITATION, ANY WARRANTIES OR CONDITIONS OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Each Recipient is solely responsible for determining the appropriateness of using and distributing the Program and assumes all risks associated with its exercise of rights under this Agreement , including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and unavailability or interruption of operations.

### 6. Disclaimer of Liability

EXCEPT AS EXPRESSLY SET FORTH IN THIS AGREEMENT, NEITHER RECIPIENT NOR ANY CONTRIBUTORS SHALL HAVE ANY LIABILITY FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING WITHOUT LIMITATION LOST PROFITS), HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OR DISTRIBUTION OF THE PROGRAM OR THE EXERCISE OF ANY RIGHTS GRANTED HEREUNDER, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

### 7. General

If any provision of this Agreement is invalid or unenforceable under applicable law, it shall not affect the validity or enforceability of the remainder of the terms of this Agreement, and without further action by the parties hereto, such provision shall be reformed to the minimum extent necessary to make such provision valid and enforceable.

If Recipient institutes patent litigation against any entity (including a cross-claim or counterclaim in a lawsuit) alleging that the Program itself (excluding combinations of the Program with other software or hardware) infringes such Recipient's patent(s), then such Recipient's rights granted under Section 2(b) shall terminate as of the date such litigation is filed.

All Recipient's rights under this Agreement shall terminate if it fails to comply with any of the material terms or conditions of this Agreement and does not cure such failure in a reasonable period of time after becoming aware of such noncompliance. If all Recipient's rights under this Agreement terminate, Recipient agrees to cease use and distribution of the Program as soon as reasonably practicable. However, Recipient's obligations under this Agreement and any licenses granted by Recipient relating to the Program shall continue and survive.

Everyone is permitted to copy and distribute copies of this Agreement, but in order to avoid inconsistency the Agreement is copyrighted and may only be modified in the following manner. The Agreement Steward reserves the right to publish new versions (including revisions) of this Agreement from time to time. No one other than the Agreement Steward has the right to modify this Agreement. The Eclipse Foundation is the initial Agreement Steward. The Eclipse Foundation may assign the responsibility to serve as the Agreement Steward to a suitable separate entity. Each new version of the Agreement will be given a distinguishing version number. The Program (including Contributions) may always be distributed subject to the version of the Agreement under which it was received. In addition, after a new version of the Agreement is published, Contributor may elect to distribute the Program (including its Contributions) under the new version. Except as expressly stated in Sections 2(a) and 2(b) above, Recipient receives no rights or licenses to the intellectual property of any Contributor under this Agreement, whether expressly, by implication, estoppel or otherwise. All rights in the Program not expressly granted under this Agreement are reserved.

This Agreement is governed by the laws of the State of New York and the intellectual property laws of the United States of America. No party to this Agreement will bring a legal action under this Agreement more than one year after the cause of action arose. Each party waives its rights to a jury trial in any resulting litigation.

</details>
