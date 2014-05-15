package com.gmail.at.zhuikov.aleksandr.driveddoc.servlet;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gmail.at.zhuikov.aleksandr.driveddoc.model.IdSignSession;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.ContainerService;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.CredentialManager;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.JsonFactory;
import com.google.drive.samples.dredit.DrEditServlet;
import com.google.gson.Gson;

@Singleton
public class SignatureServlet extends DrEditServlet {

	private static final long serialVersionUID = 1L;
	
	private final ContainerService containerService;
	
	@Inject
	public SignatureServlet(ContainerService containerService, JsonFactory jsonFactory, CredentialManager credentialManager) {
		super(jsonFactory, credentialManager);
		this.containerService = containerService;
	}
	
	public static class SignatureRequest {
		public String fileId;
		public String cert;
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		SignatureRequest signatureRequest = new Gson().fromJson(req.getReader(), SignatureRequest.class);
		
		Credential credential = getCredential();
		
		if (signatureRequest.fileId == null) {
			sendError(resp, 400, "The `file_id` URI parameter must be specified.");
			return;
		}
		
		if (signatureRequest.cert == null) {
			sendError(resp, 400, "The `cert` URI parameter must be specified.");
			return;
		}
		
		try {
			
			IdSignSession signSession = containerService.startSigning(signatureRequest.fileId, signatureRequest.cert, credential);			
			req.getSession().setAttribute("ddoc", signSession);
			sendJson(resp, signSession.getDigest());

		} catch (GoogleJsonResponseException e) {
			if (e.getStatusCode() == 401) {
				// The user has revoked our token or it is otherwise bad.
				// Delete the local copy so that their next page load will
				// recover.
				credentialManager.delete(getUserId(req), getCredential());
			}

			sendGoogleJsonResponseError(resp, e);
		}
	}
}
