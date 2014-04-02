package com.gmail.at.zhuikov.aleksandr.driveddoc.servlet;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.json.JsonFactory;
import com.google.drive.samples.dredit.CredentialManager;

@Singleton
public class DriveDDocServlet extends AuthorizationServlet {

	private static final long serialVersionUID = 1L;

	@Inject
	public DriveDDocServlet(JsonFactory jsonFactory, CredentialManager credentialManager) {
		super(jsonFactory, credentialManager);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		req.getRequestDispatcher("/public/editor.html").forward(req, resp);
	}
}
