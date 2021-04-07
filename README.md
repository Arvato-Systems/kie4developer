KIE4Developer - Work with KIE the easy way
========================================

Library for creating and deploying JBPM workflows using Java.
- Create BPMN Processes (Process Definitions) as simple Java classes with fluent API or use existing .bpmn xml files
- Use simple Java classes for executing business logic in service tasks
- Unittest the BPMN Processes with JUnit
- Package a release with all dependencies as single self-executable jar artifact
- Deploy to a KIE Server with selectable deployment strategies e.g. overwrite or migration

How to configure it[](how-to-configure-it)
------------------------------

Complete configuration via [application.properties](src/main/resources/application.properties) of the projects.

You can either use the included KIE Server as deploy target and runtime (default) or connect to a remote KIE Server via following properties:
```
kieserver.protocol=http
kieserver.host=localhost
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

To create your Spring Boot fat jar just run `mvn:package`. Use `mvn:package deploy` for uploading your artifact to the repository server.

To start the application direct on the target-system just run `java -jar workflow-processes-example-7.23.0.Final-SNAPSHOT.jar`.

How to run it
------------------------------

Please read the chapter [How to configure it](#how-to-configure-it) first.

You can run the application by simply starting

```
mvn clean spring-boot:run
```

If you like to use the KIE Server REST API (hint: the main method contains a shutdown that you may want to remove):

```
# URL & Credentails (start-up takes some time...):
# Swagger UI: http://localhost:8180/kie-server/services/rest/api-docs?url=/kie-server/services/rest/swagger.json
# http://localhost:8180/kie-server/services/rest/server
# kieserver/kieserver1!
```

If you like to have a complete runtime setup with external KIE Server and Business Central please run this separately.
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

All general projects settings are taken from the maven [pom.xml](pom.xml) file.
Advanced configuration of your project is done within [application.properties](src/main/resources/application.properties) file.

Add BPMN Process Model classes by implementing the ```IDeployableBPMNProcess``` Interface. Store them in the package ```com.example.workflow.processes```.
Add required dependencies by add them into  ```pom.xml```.
Create Service classes that runs your business logic by providing the default constructur and store them in the package ```com.example.workflow.services```.
If you use custom classes within your process models make sure these implements ```Serializable``` for marshalling support.
Add own WorkItemHandler by implementing the ```IDeployableWorkItemHandler``` Interface. Store them in the package ```com.example.workflow.workitemhandler```.

Check out many examples within the ```test``` directory.
