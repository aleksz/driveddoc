package com.gmail.at.zhuikov.aleksandr.driveddoc.repository;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gmail.at.zhuikov.aleksandr.driveddoc.service.OAuthSecretsService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.appengine.auth.oauth2.AppEngineCredentialStore;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.drive.samples.dredit.model.OAuthSecrets;

@Singleton
public class CredentialRepository {

	private AppEngineCredentialStore credentialStore = new AppEngineCredentialStore();
	private HttpTransport transport;
	private JsonFactory jsonFactory;
	private OAuthSecrets oAuthSecrets;

	@Inject
	public CredentialRepository(HttpTransport httpTransport,
			JsonFactory jsonFactory, OAuthSecretsService oAuthSecretsService) {
		transport = httpTransport;
		this.jsonFactory = jsonFactory;
		this.oAuthSecrets = oAuthSecretsService.get();
	}
	
	public Credential buildEmpty() {
		return new GoogleCredential.Builder()
				.setClientSecrets(oAuthSecrets.getClientId(), oAuthSecrets.getClientSecrets())
				.setTransport(transport)
				.setJsonFactory(jsonFactory)
				.build();
	}

	public Credential get(String userId) {
	    Credential credential = buildEmpty();
	    if (credentialStore.load(userId, credential)) {
	      return credential;
	    }
	    return null;
	}
	
	public void save(String userId, Credential credential) {
		credentialStore.store(userId, credential);
	}

	public void delete(String userId) {
		credentialStore.delete(userId, get(userId));
	}
}
