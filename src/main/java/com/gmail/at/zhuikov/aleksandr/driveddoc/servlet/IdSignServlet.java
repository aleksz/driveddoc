package com.gmail.at.zhuikov.aleksandr.driveddoc.servlet;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gmail.at.zhuikov.aleksandr.driveddoc.model.IdSignSession;
import com.gmail.at.zhuikov.aleksandr.driveddoc.repository.SignatureContainerDescriptionRepository;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.ContainerService;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.CredentialManager;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.JsonFactory;
import com.google.drive.samples.dredit.DrEditServlet;

import ee.sk.digidoc.SignedDoc;

@Singleton
public class IdSignServlet extends DrEditServlet {

	private static final long serialVersionUID = 1L;
	
	private final ContainerService containerService;
	SignatureContainerDescriptionRepository signatureContainerDescriptionRepository = 
			SignatureContainerDescriptionRepository.getInstance();

	@Inject
	public IdSignServlet(JsonFactory jsonFactory, CredentialManager credentialManager, ContainerService containerService) {
		super(jsonFactory, credentialManager);
		this.containerService = containerService;
	}
	
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		String fileId = req.getParameter("file_id");
		
		Credential credential = getCredential();
		
		if (fileId == null) {
			sendError(resp, 400, "The `file_id` URI parameter must be specified.");
			return;
		}
		
		String signatureValue = req.getParameter("signature");
		
		if (signatureValue == null) {
			sendError(resp, 400, "The `signature` URI parameter must be specified.");
			return;
		}
		
		try {

			IdSignSession signSession = (IdSignSession) req.getSession().getAttribute("ddoc");
			
			containerService.finalizeSignature(signSession, signatureValue, getUserId(req), fileId, credential);
			
			sendJson(resp, "ok");

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
