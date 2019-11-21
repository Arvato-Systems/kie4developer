KIE Client with all capabilities
========================================

KIE Client to control a KIE Server sample application.
This is a complete KIE Client example that can be used to leverage
business process management, rules management and planning solutions on a single runtime.

How to configure it
------------------------------

Complete configuration is via application.properties of the projects.
Remote KIE Server can be connected via following properties:

```
kieserver.location=http://localhost:8180/kie-server/services/rest/server
kieserver.user=admin
kieserver.pwd=admin
```

Remote KIE Workbench can be connected via following properties:

```
kieworkbench.protocol=http
kieworkbench.host=localhost
kieworkbench.port=8080
kieworkbench.context=jbpm-console
kieworkbench.context.maven=maven2
kieworkbench.user=admin
kieworkbench.pwd=admin
```

How to run it
------------------------------

You can run the application by simply starting

```
mvn clean spring-boot:run
```

If you like to use the jbpm workbench please run this seperatly as Docker container (https://hub.docker.com/r/jboss/jbpm-workbench-showcase):

```
docker run -p 8080:8080 -p 8001:8001 -d --name jbpm-workbench jboss/jbpm-workbench-showcase:7.15.0.Final

URL & Credentails (start-up takes some time...):
http://localhost:8080/jbpm-console
admin/admin
```

If you like to use the jbpm server please run this seperatly as Docker container (https://hub.docker.com/r/jboss/kie-server-showcase):

```
docker run -p 8180:8080 -d --name kie-server --link jbpm-workbench:kie-wb jboss/kie-server-showcase:7.15.0.Final

URL & Credentails (start-up takes some time...):
http://localhost:8180/kie-server/docs/
kieserver/kieserver1!
```
