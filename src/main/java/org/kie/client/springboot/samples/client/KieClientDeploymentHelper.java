package org.kie.client.springboot.samples.client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import org.apache.tomcat.util.codec.binary.Base64;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.compiler.kie.builder.impl.KieBuilderImpl;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.io.Resource;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieServiceResponse.ResponseType;
import org.kie.server.api.model.ServiceResponse;
import org.kie.client.springboot.samples.common.interfaces.IDeployableBPMNProcess;
import org.kie.client.springboot.samples.common.interfaces.IDeploymentHelper;
import org.kie.client.springboot.samples.common.interfaces.IRelease;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class KieClientDeploymentHelper implements IDeploymentHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(KieClientDeploymentHelper.class);
	private IDeployableBPMNProcess processToDeploy;
  @Autowired
  private KieClient kieClient;
  @Autowired
  private IRelease release;

	@Value("${kieworkbench.protocol}")
	private String workbenchProtocol;
	@Value("${kieworkbench.host}")
	private String workbenchHost;
	@Value("${kieworkbench.port}")
	private int workbenchPort;
	@Value("${kieworkbench.context}")
	private String workbenchContext;
	@Value("${kieworkbench.context.maven}")
	private String workbenchMavenContext;
	@Value("${kieworkbench.user}")
	private String workbenchUser;
	@Value("${kieworkbench.pwd}")
	private String workbenchPassword;


	@Override
	public IRelease getRelease() {
		return release;
	}

	@Override
  public void setProcessToDeploy(IDeployableBPMNProcess processToDeploy) {
    this.processToDeploy = processToDeploy;
  }

  @Override
  public boolean deploy() {
		deployJarIfExist();
		deployWorkItemHandlerIfExist();

		// send deployment command to server
		String containerId = getRelease().getContainerId();
		org.kie.server.api.model.ReleaseId releaseId = getRelease().getReleaseIdForServerAPI();

		KieContainerResource resource = new KieContainerResource(containerId, releaseId);
		// set runtime strategy to PER_PROCESS_INSTANCE
//		KieServerConfigItem config = new KieServerConfigItem();
//		config.setName(KieServerConstants.PCFG_RUNTIME_STRATEGY);
//		config.setType(KieServerConstants.CAPABILITY_BPM);
//		config.setValue("PER_PROCESS_INSTANCE");
//		resource.addConfigItem(config);

		ServiceResponse<KieContainerResource> createResponse = kieClient.getKieServicesClient().createContainer(containerId, resource);
		if (createResponse.getType() == ResponseType.FAILURE) {
			LOGGER.error("Error creating " + containerId + ". Message: " + createResponse.getMsg());
			return false;
		}else{
			LOGGER.info("Container " +  containerId +  " for release " + releaseId + " successful created.");
		}
		return true;
  }

	/**
	 * Deploy a kjar into the Server Container
	 * @see {https://developers.redhat.com/blog/2018/03/14/what-is-a-kjar/}
	 */
	private void deployJarIfExist() {
		File jarFile = processToDeploy.getJarFile();
		File pomFile = processToDeploy.getPomFile();

		if (!processToDeploy.isDistributedAsJar()) {
			// in this case we have to build the kjar by our own first
			org.kie.server.api.model.ReleaseId releaseId = getRelease().getReleaseIdForServerAPI();
			KieServices ks = KieServices.Factory.get();
			KieFileSystem kfs = ks.newKieFileSystem();

			Resource res = processToDeploy.getBPMNModel();
			kfs.write(res);
			kfs.generateAndWritePomXML(releaseId);
			KieBuilder builder = ks.newKieBuilder(kfs).buildAll();
			if (builder.getResults().hasMessages(Message.Level.ERROR)) {
				LOGGER.error("Process compilation error: " + builder.getResults().getMessages().toString());
				throw new RuntimeException("Process compilation error: " + builder.getResults().getMessages().toString());
			}
			InternalKieModule kieModule = (InternalKieModule) ks.getRepository().getKieModule(releaseId);
			String pomXml = KieBuilderImpl.generatePomXml(releaseId);

			try {
				pomFile = File.createTempFile("pom",".xml");
				Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pomFile), "utf-8"));
				writer.write(pomXml);
				writer.close();

				jarFile = File.createTempFile(getRelease().getArtifactId() + "-" + getRelease().getVersion(),".jar");
				OutputStream os	= new FileOutputStream(jarFile);
				os.write(kieModule.getBytes());
				os.close();
			} catch (IOException e) {
				throw new RuntimeException("Kjar write error", e);
			}
			LOGGER.info("Kjar created successfull: "+ jarFile.getAbsolutePath());
		}

		// There exist multiple ways to make the kjar artifact available for the kie-server. In most of the cases the artifact gets fetched from the related workbench setup.
		// Upload jar file to the Workbench maven repository by via Workbench GUI: Login into jbpm.console and goto 'Authoring'-->Artifact Repository-->Upload
		// Upload jar file to the Workbench maven repository by maven deploy goal: https://access.redhat.com/documentation/en-us/red_hat_jboss_bpm_suite/6.1/html/development_guide/uploading_artifacts_to_maven_repository / mvn deploy:deploy-file -DgroupId=org.kie.example -DartifactId=project1 -Dversion=1.0.4 -Dpackaging=jar -Dfile=/NotBackedUp/repository1/project1/target/project1-1.0.4.jar -DrepositoryId=guvnor-m2-repo -Durl=http://localhost:8080/jbpm-console/maven2/
		// Upload jar file to the Workbench maven repository by http request: curl -v -X POST -F data=@"/path/to/Project1-1.0.jar" -H 'Content-Type: multipart/form-data' -u User:Password http://localhost:8080/business-central/maven2/
		// Upload jar file to the Workbench maven repository by using Workbench's REST API: https://docs.jboss.org/jbpm/release/7.6.0.Final/jbpm-docs/html_single/#_maven_calls / [POST] http://localhost:8080/business-central/rest/deployment/groupID:ArtifactID:Version/deploy
		// Copy the jar into the workbench's maven repo folder: docker cp ...
		// Copy the jar into the kie-server's maven repo folder: docker cp ...
		// extend the maven settings.xml on the kie-server to fetch artifacts from other repositories

		//Maven coordinates
		String groupId = getRelease().getGroupId();
		String artifactId = getRelease().getArtifactId();
		String version = getRelease().getVersion();

		String url = workbenchProtocol + "://" + workbenchHost + ":" + workbenchPort + "/" + workbenchContext + "/" + workbenchMavenContext + "/" + groupId.replace('.', '/') + "/" + artifactId +"/" + version + "/" + artifactId + "-" + version + ".jar";

		try {
			ResponseEntity<String> response = uploadFile(jarFile, url);
			if (response.getStatusCode().is2xxSuccessful()){
				LOGGER.info("Jar file " + jarFile.getName() + " successful uploaded into workbench");
			}
		} catch (Exception e) {
			LOGGER.error("Error while jar file upload", e);
			throw e;
		}
	}

	/**
	 * Deploy a workitemhandler into the Server Container
	 * @see {https://developers.redhat.com/blog/2018/03/14/what-is-a-kjar/}
	 */
	private void deployWorkItemHandlerIfExist() {
		if (processToDeploy.hasWorkItemHandler()){
			File jarFile = processToDeploy.getWorkItemHandlerJarFile();

			//Maven coordinates //TODO: maybe it is requred to sync this with the content of the workitemhandler jar pom.xml file
			String groupId = getRelease().getGroupId();
			String artifactId = getRelease().getArtifactId() + "-workitemhandler";
			String version = getRelease().getVersion();
			String url = workbenchProtocol + "://" + workbenchHost + ":" + workbenchPort + "/" + workbenchContext + "/" + workbenchMavenContext + "/" + groupId.replace('.', '/') + "/" + artifactId +"/" + version + "/" + artifactId + "-" + version + ".jar";

			try {
				ResponseEntity<String> response = uploadFile(jarFile, url);
				if (response.getStatusCode().is2xxSuccessful()){
					LOGGER.info("Jar file " + jarFile.getName() + " successful uploaded into workbench");
				}
			} catch (Exception e) {
				LOGGER.error("Error while jar file upload", e);
				throw e;
			}
		}
	}

	@Override
  public boolean undeploy() {
    String containerId = getRelease().getContainerId();
		KieContainerResource container = kieClient.getKieServicesClient().getContainerInfo(containerId).getResult();
		if (container != null) {
			ServiceResponse<Void> responseDispose = kieClient.getKieServicesClient().disposeContainer(containerId);
			if (responseDispose.getType() == ResponseType.FAILURE) {
				LOGGER.error("Error disposing " + containerId + ". Message: " + responseDispose.getMsg());
				return false;
			} else {
				LOGGER.info("Container disposed: " + containerId + ". ");
				undeployJarIfExist();
			}
			return true;
		}
		return false;
  }

	/**
	 * Undeploy a kjar from the Server
	 */
	private void undeployJarIfExist() {
		if (processToDeploy.isDistributedAsJar()){
			// removing jars from the server repo isn't possible
		}
	}

	/**
	 * Upload a single file into the kie-workbench by using Basic Auth
	 * @param file the file to upload
	 * @param url the target url
	 * @return the http response
	 */
	private ResponseEntity<String> uploadFile(File file, String url){
		HttpHeaders headers = new HttpHeaders() {{
			String auth = workbenchUser + ":" + workbenchPassword;
			byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
			String authHeader = "Basic " + new String(encodedAuth);
			set("Authorization", authHeader);
			setContentType(MediaType.MULTIPART_FORM_DATA);
		}};

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("file", new FileSystemResource(file));

		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
		//Perform the HTTP POST
		RestTemplate restTemplate = new RestTemplate();
		return restTemplate.postForEntity(url, requestEntity, String.class);
	}

}
