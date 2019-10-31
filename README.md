KIE Server with all capabilities
========================================

KIE Server SpringBoot sample application that uses Spring Security for securing access to KIE Server resources.
This is a complete (fully featured KIE Server - includes all capabilities) KIE Server that can be used to leverage 
business process management, rules management and planning solutions in single runtime.

How to configure it
------------------------------

Complete configuration is via application.properties of the projects.
Users can decide which KIE Server extensions should be activated via following properties:

```
kieserver.drools.enabled=true
kieserver.dmn.enabled=true
kieserver.jbpm.enabled=true
kieserver.jbpmui.enabled=true
kieserver.casemgmt.enabled=true
kieserver.optaplanner.enabled=true
```

How to run it
------------------------------

You can run the application by simply starting

```
mvn clean spring-boot:run

URL & Credentails:
http://localhost:8090/kie-server/services/rest/server
kieserver/kieserver1!
```

If you like to use the jbpm workbench please run this seperatly as Docker container (https://hub.docker.com/r/jboss/jbpm-workbench-showcase):

```
docker run -p 8080:8080 -p 8001:8001 -d -e JAVA_OPTS="-Djava.net.preferIPv4Stack=true" --name jbpm-workbench jboss/jbpm-workbench-showcase:7.6.0.Final

URL & Credentails (start-up takes some time...):
http://localhost:8080/jbpm-console
admin/admin
```