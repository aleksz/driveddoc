package com.google.drive.samples.dredit;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gmail.at.zhuikov.aleksandr.driveddoc.repository.CredentialRepository;
import com.gmail.at.zhuikov.aleksandr.driveddoc.repository.ServerNameRepository;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.OAuthSecretsService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.drive.samples.dredit.model.OAuthSecrets;

@Singleton
public class CredentialManager {

	private HttpTransport transport;
	private JsonFactory jsonFactory;
	private CredentialRepository credentialRepository;
	private OAuthSecrets oAuthSecrets;
	private ServerNameRepository serverNameRepository;

	public static final List<String> SCOPES = Arrays.asList(
			// Required to access and manipulate files.
			"https://www.googleapis.com/auth/drive.file",
			// Required to identify the user in our data store.
			"https://www.googleapis.com/auth/userinfo.email",
			"https://www.googleapis.com/auth/userinfo.profile",
			"https://www.googleapis.com/auth/drive.install"
	// "https://www.googleapis.com/auth/drive.appdata",
	// "https://www.googleapis.com/auth/drive.apps.readonly"
			);

	@Inject
	public CredentialManager(CredentialRepository credentialRepository,
			HttpTransport transport, JsonFactory jsonFactory,
			OAuthSecretsService oAuthSecretsService,
			ServerNameRepository serverNameRepository) {
		this.credentialRepository = credentialRepository;
		this.transport = transport;
		this.jsonFactory = jsonFactory;
		this.serverNameRepository = serverNameRepository;
		this.oAuthSecrets = oAuthSecretsService.get();
	}

	public Credential buildEmpty() {
		return new GoogleCredential.Builder()
				.setClientSecrets(
						oAuthSecrets.getClientId(), 
						oAuthSecrets.getClientSecrets())
				.setTransport(transport)
				.setJsonFactory(jsonFactory).build();
	}

	public Credential get(String userId) {
		return credentialRepository.get(userId);
	}

	public void save(String userId, Credential credential) {
		credentialRepository.save(userId, credential);
	}

	public void delete(String userId) {
		credentialRepository.delete(userId);
	}

	public String getAuthorizationUrl(String state) {
		GoogleAuthorizationCodeRequestUrl urlBuilder = new GoogleAuthorizationCodeRequestUrl(
				oAuthSecrets.getClientId(), 
				serverNameRepository.get(), 
				SCOPES)
				.setAccessType("offline")
				.setApprovalPrompt("auto")
				.setState(state);

		return urlBuilder.build();
	}

	public Credential retrieve(String code) {
		try {
			GoogleTokenResponse response = new GoogleAuthorizationCodeTokenRequest(
					transport, 
					jsonFactory,
					oAuthSecrets.getClientId(),
					oAuthSecrets.getClientSecrets(), 
					code,
					serverNameRepository.get()).execute();

			return buildEmpty().setFromTokenResponse(response);

		} catch (IOException e) {
			new RuntimeException(
					"An unknown problem occured while retrieving token");
		}

		return null;
	}
}
