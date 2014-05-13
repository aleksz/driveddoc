package com.gmail.at.zhuikov.aleksandr.driveddoc.servlet;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gmail.at.zhuikov.aleksandr.driveddoc.service.CachedContainerService;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.ContainerService;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.CredentialManager;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.JsonFactory;
import com.google.drive.samples.dredit.DrEditServlet;

@Singleton
public class SignServlet extends DrEditServlet {

	private static final long serialVersionUID = 1L;
	
	private final ContainerService containerService;
	
	@Inject
	public SignServlet(JsonFactory jsonFactory, CredentialManager credentialManager, CachedContainerService cachedContainerService) {
		super(jsonFactory, credentialManager);
		containerService = cachedContainerService;
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		String fileId = req.getParameter("file_id");
		
		if (fileId == null) {
			sendError(resp, 400, "The `file_id` URI parameter must be specified.");
			return;
		}
		
		String personalId = req.getParameter("personalId");
		
		if (personalId == null) {
			sendError(resp, 400, "The `personalId` URI parameter must be specified.");
			return;
		}
		
		String phoneNumber = req.getParameter("phoneNumber");
		
		if (phoneNumber == null) {
			sendError(resp, 400, "The `phoneNumber` URI parameter must be specified.");
			return;
		}
		
		try {		
			
			sendJson(resp, containerService.startSigningWithMobileId(fileId, personalId, phoneNumber, getCredential()));

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
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		String fileId = req.getParameter("file_id");
		String sessionId = req.getParameter("sessionId");
		
		if (fileId == null) {
			sendError(resp, 400, "The `file_id` URI parameter must be specified.");
			return;
		}
		
		if (sessionId == null) {
			sendError(resp, 400, "The `sessionId` URI parameter must be specified.");
			return;
		}
		
		String status = containerService.checkMobileIdSignStatus(fileId, sessionId, getCredential());	
		
		sendJson(resp, status);
	}
}
