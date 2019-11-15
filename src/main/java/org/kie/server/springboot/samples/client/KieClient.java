package org.kie.server.springboot.samples.client;

import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.client.DocumentServicesClient;
import org.kie.server.client.JobServicesClient;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.ProcessServicesClient;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.client.UIServicesClient;
import org.kie.server.client.UserTaskServicesClient;
import org.kie.server.client.admin.ProcessAdminServicesClient;
import org.kie.server.client.admin.UserTaskAdminServicesClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KieClient {

	// kie server connection parameter
	@Value("${kieserver.location}")
	private String kieServerUrl;
	@Value("${kieserver.user}")
	private String kieServerUser;
	@Value("${kieserver.pwd}")
	private String kieServerPwd;
	private Integer connectionTimeout = 60000; //60s

	// kie server client
	private KieServicesClient kieServicesClient;
	// normal clients
	private ProcessServicesClient processClient;
	private UserTaskServicesClient taskClient;
	private QueryServicesClient queryClient;
	private JobServicesClient jobServicesClient;
	private DocumentServicesClient documentClient;
	private UIServicesClient uiServicesClient;
	// admin clients
	private ProcessAdminServicesClient processAdminClient;
	private UserTaskAdminServicesClient userTaskAdminClient;
    
    
	/**
	 * Connect to the specified Kie Server
	 */
	private void connectToServer() {
		if (kieServicesClient == null) {
			KieServicesConfiguration configuration = KieServicesFactory.newRestConfiguration(kieServerUrl, kieServerUser, kieServerPwd);
			configuration.setTimeout(connectionTimeout); // increate timeout... default is 5s
			configuration.setMarshallingFormat(MarshallingFormat.JSON);
			kieServicesClient = KieServicesFactory.newKieServicesClient(configuration);
			processClient = kieServicesClient.getServicesClient(ProcessServicesClient.class);
			taskClient = kieServicesClient.getServicesClient(UserTaskServicesClient.class);
			queryClient = kieServicesClient.getServicesClient(QueryServicesClient.class);
			jobServicesClient = kieServicesClient.getServicesClient(JobServicesClient.class);
			documentClient = kieServicesClient.getServicesClient(DocumentServicesClient.class);
			uiServicesClient = kieServicesClient.getServicesClient(UIServicesClient.class);
			processAdminClient = kieServicesClient.getServicesClient(ProcessAdminServicesClient.class);
			userTaskAdminClient = kieServicesClient.getServicesClient(UserTaskAdminServicesClient.class);
		}
	}

	public KieServicesClient getKieServicesClient() {
		connectToServer();
		return kieServicesClient;
	}

	public QueryServicesClient getQueryClient() {
		connectToServer();
		return queryClient;
	}

	public JobServicesClient getJobServicesClient() {
		connectToServer();
		return jobServicesClient;
	}

	public DocumentServicesClient getDocumentClient() {
		connectToServer();
		return documentClient;
	}

	public UIServicesClient getUiServicesClient() {
		connectToServer();
		return uiServicesClient;
	}

	public ProcessAdminServicesClient getProcessAdminClient() {
		connectToServer();
		return processAdminClient;
	}

	public UserTaskAdminServicesClient getUserTaskAdminClient() {
		connectToServer();
		return userTaskAdminClient;
	}

	public ProcessServicesClient getProcessClient() {
		connectToServer();
		return processClient;
	}
	
	public UserTaskServicesClient getTaskClient() {
		connectToServer();
		return taskClient;
	}
	
}
