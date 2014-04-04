package com.gmail.at.zhuikov.aleksandr.driveddoc.servlet;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gmail.at.zhuikov.aleksandr.driveddoc.model.DigidocOCSPSignatureContainer;
import com.gmail.at.zhuikov.aleksandr.driveddoc.model.SignatureContainerDescription;
import com.gmail.at.zhuikov.aleksandr.driveddoc.repository.SignatureContainerDescriptionRepository;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.CredentialManager;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.DigiDocService;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.GDriveService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.drive.model.File;
import com.google.appengine.api.blobstore.BlobstoreInputStream;
import com.google.drive.samples.dredit.DrEditServlet;

import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.SignedDoc;

@Singleton
public class IdSignServlet extends DrEditServlet {

	private static final long serialVersionUID = 1L;
	
	private DigiDocService digiDocService;
	private GDriveService gDriveService;
	SignatureContainerDescriptionRepository signatureContainerDescriptionRepository = 
			SignatureContainerDescriptionRepository.getInstance();

	
	@Inject
	public IdSignServlet(DigiDocService digiDocService, GDriveService gDriveService, JsonFactory jsonFactory, CredentialManager credentialManager) {
		super(jsonFactory, credentialManager);
		this.digiDocService = digiDocService;
		this.gDriveService = gDriveService;
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
		
		String signatureId = req.getParameter("signatureId");
		
		if (signatureId == null) {
			sendError(resp, 400, "The `signatureId` URI parameter must be specified.");
			return;
		}
		
		String signatureValue = req.getParameter("signature");
		
		if (signatureValue == null) {
			sendError(resp, 400, "The `signature` URI parameter must be specified.");
			return;
		}
		
		try {

			SignedDoc signedDoc = (SignedDoc) req.getSession().getAttribute("ddoc");
			
			digiDocService.finalizeSignature(
					signedDoc, 
					signatureId, 
					signatureValue, 
					getOCSPSignatureContainer(req));
			
			try {
				File file = gDriveService.getFile(fileId, credential);
				gDriveService.updateContent(file, signedDoc.toXML().getBytes(), credential);
			} catch (DigiDocException e) {
				throw new RuntimeException(e);
			}
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

	private DigidocOCSPSignatureContainer getOCSPSignatureContainer(
			HttpServletRequest req) throws IOException, ServletException {
		
		SignatureContainerDescription description = 
				signatureContainerDescriptionRepository.get(getUserId(req));
		
		return new DigidocOCSPSignatureContainer(
				new BlobstoreInputStream(description.getKey()), description);
	}
}
