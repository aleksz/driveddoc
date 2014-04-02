package com.gmail.at.zhuikov.aleksandr.driveddoc.servlet;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.json.JsonFactory;
import com.google.drive.samples.dredit.CredentialManager;

public abstract class DriveUIAuthorizationServlet extends AuthorizationServlet {

	private static final long serialVersionUID = 1L;
	
	@Inject
	public DriveUIAuthorizationServlet(JsonFactory jsonFactory, CredentialManager credentialManager) {
		super(jsonFactory, credentialManager);
	}
	
	@Override
	protected void onAuthorization(HttpServletRequest req,
			HttpServletResponse resp,
			AuthorizationCodeRequestUrl authorizationUrl)
			throws ServletException, IOException {
		authorizationUrl.setState(req.getParameter("state"));
		super.onAuthorization(req, resp, authorizationUrl);
	}
}
