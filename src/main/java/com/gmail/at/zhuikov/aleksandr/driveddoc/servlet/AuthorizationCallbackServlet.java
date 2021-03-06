package com.gmail.at.zhuikov.aleksandr.driveddoc.servlet;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gmail.at.zhuikov.aleksandr.driveddoc.service.CredentialManager;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialStore;
import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.extensions.servlet.auth.oauth2.AbstractAuthorizationCodeCallbackServlet;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.json.JsonFactory;

@Singleton
public class AuthorizationCallbackServlet extends
		AbstractAuthorizationCodeCallbackServlet {

	private static final long serialVersionUID = 1L;
	
	protected final JsonFactory jsonFactory;
	protected final CredentialStore credentialStore; 
	
	@Inject
	public AuthorizationCallbackServlet(JsonFactory jsonFactory, CredentialManager credentialManager) {
		this.jsonFactory = jsonFactory;
		this.credentialStore = credentialManager;
	}
	@Override
	protected void onSuccess(HttpServletRequest req, HttpServletResponse resp,
			Credential credential) throws ServletException, IOException {
		resp.sendRedirect("/?" + req.getQueryString());
	}

	@Override
	protected void onError(HttpServletRequest req, HttpServletResponse resp,
			AuthorizationCodeResponseUrl errorResponse)
			throws ServletException, IOException {
		// handle error
	}

	@Override
	protected String getRedirectUri(HttpServletRequest req)
			throws ServletException, IOException {
		GenericUrl url = new GenericUrl(req.getRequestURL().toString());
		url.setRawPath("/api/oauth2callback"); 
		return url.build();
	}

	@Override
	protected AuthorizationCodeFlow initializeFlow() throws IOException {
		return new GoogleAuthorizationCodeFlow.Builder(new UrlFetchTransport(),
				jsonFactory, "610309933249.apps.googleusercontent.com",
				"YDq0zPizR0rJANUBlgbzlb_4",
				CredentialManager.SCOPES)
				.setCredentialStore(credentialStore)
				.setAccessType("offline")
				.build();
	}

	@Override
	protected String getUserId(HttpServletRequest req) throws ServletException,
			IOException {
		return (String) req.getSession().getAttribute("me");
	}
}