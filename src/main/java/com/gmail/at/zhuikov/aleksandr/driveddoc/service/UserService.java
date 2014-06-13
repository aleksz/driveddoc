package com.gmail.at.zhuikov.aleksandr.driveddoc.service;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gmail.at.zhuikov.aleksandr.driveddoc.model.User;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;

@Singleton
public class UserService {

	private HttpTransport transport;
	private JsonFactory jsonFactory;

	@Inject
	public UserService(HttpTransport transport, JsonFactory jsonFactory) {
		this.transport = transport;
		this.jsonFactory = jsonFactory;
	}

	public User getUserInfo(String userId, Credential credential) throws IOException {
		Oauth2 service = getOauth2Service(credential);
		Userinfo userInfo = service.userinfo().get().execute();
		return new User(userInfo.getEmail(), userInfo.getLink());
	}

	/**
	 * Build and return an Oauth2 service object based on given request
	 * parameters.
	 * 
	 * @param credential
	 *            User credentials.
	 * @return Drive service object that is ready to make requests, or null if
	 *         there was a problem.
	 */
	protected Oauth2 getOauth2Service(Credential credential) {
		return new Oauth2.Builder(transport, jsonFactory, credential)
				.setApplicationName("Drive DigiDoc").build();
	}
}
