KIE4Humans - Work with KIE the easy way
========================================

This is a template project to easily create KIE Workflow Projects and deploy them to a external KIE Server (BICCW).
- Create BPMN Processes (Process Definitions) as simple Java classes with fluent API or use exising .bpmn xml file
- Use simple java classes for executing business logic in script tasks
- Unittest the BPMN Processes with JUnit
- Package a release and deploy to a Server with update strategies

How to configure it
------------------------------

Complete configuration via application.properties of the projects.

You can either use the included KIE Server as runtime and deploy target or 
connect to a remote KIE Server via following properties:
```
kieserver.protocol=http
kieserver.host=localhost
kieserver.port=8180
kieserver.user=kieserver
kieserver.pwd=kieserver1!
```

For runtime you need a remote KIE Workbench which can be connected via following properties:
```
kieworkbench.protocol=http
kieworkbench.host=localhost
kieworkbench.port=8080
kieworkbench.context=jbpm-console
kieworkbench.user=admin
kieworkbench.pwd=admin
```

How to build it
------------------------------
To create your Spring Boot fat jar just run `mvn:package`. Use `mvn:package deploy` for uploading your artifact to the repository server.<br/>

You can build the docker image with `docker build --build-arg VERSION_NO=1.0.0-SNAPSHOT -t workflow-rdu-processes .` from the command line. Use `docker save -o target/workflow-rdu-processes.tar workflow-rdu-processes` to build the image to a tarball, then use `docker load --input target/workflow-rdu-processes.tar`on the target system to load the image into Docker and `docker run -p 8180:8180 -d workflow-rdu-processes` to run the container.<br/>
To publish the image on the docker repository run `docker login -u <username> -p <password> docker.bfs-finance.de` on the command line and login. Then use `docker tag workflow-rdu-processes docker.bfs-finance.de/workflow-rdu-processes:latest` and `docker push docker.bfs-finance.de/workflow-rdu-processes:latest` to push the image to the repository. To publish a specific version use `docker tag workflow-rdu-processes docker.bfs-finance.de/workflow-rdu-processes:1.0.0-SNAPSHOT` and `docker push docker.bfs-finance.de/workflow-rdu-processes:1.0.0-SNAPSHOT` to push the image<br/>

Hint: for removing all docker container and images execute: `docker stop $(docker ps -aq)` ; `docker rm $(docker ps -a -q)` ; `docker rmi -f $(docker images -q)`.

Ensure that the default installed java version in the environment is JDK 8.<br/>
To start the Application direct on the target-System just run `java -jar workflow-rdu-processes-0.0.1-SNAPSHOT.jar`.

How to run it
------------------------------

Make sure you have a kie workbench instance up and running. E.g. use the existing [docker image](https://hub.docker.com/r/jboss/jbpm-workbench-showcase):

```
docker run -p 8080:8080 -p 8001:8001 -d --name jbpm-workbench jboss/jbpm-workbench-showcase:7.15.0.Final

URL & Credentails (start-up takes some time...):
http://localhost:8080/jbpm-console
admin/admin
```

You can run the application by simply starting

```
mvn clean spring-boot:run
```

If you like to use the kie server API:

```
URL & Credentails (start-up takes some time...):
Swagger UI: http://localhost:8180/kie-server/services/rest/api-docs?url=/kie-server/services/rest/swagger.json
http://localhost:8180/kie-server/services/rest/server
kieserver/kieserver1!
```

If you like to use a external kie server please run this separately. E.g. use the existing [docker image](https://hub.docker.com/r/jboss/kie-server-showcase):

```
docker run -p 8180:8080 -d --env "JAVA_OPTS=-Xms256m -Xmx1024m -Djava.net.preferIPv4Stack=true -Dorg.kie.server.bypass.auth.user=true" --name kie-server --link jbpm-workbench:kie-wb jboss/kie-server-showcase:7.15.0.Final

URL & Credentails (start-up takes some time...):
http://localhost:8180/kie-server/docs/
kieserver/kieserver1!
```

How to implement your own workflow solution
------------------------------
Configure your project settings

```
spring.application.groupid=com.example
spring.application.name=myproject
spring.application.version=1.0.0-SNAPSHOT
spring.application.project.name=My Project
spring.application.project.description=My first Workflow Project
```

Add BPMN Process Model classes by implementing the ```IDeployableBPMNProcess``` Interface.
Add required dependencies by implementing the ```IDeployableDependency``` Interface.
Use WorkItemHandler classes by implementing the ```IDeployableWorkItemHandler``` Interface.
Use Service classes by implementing the ```IDeployableService``` Interface.
