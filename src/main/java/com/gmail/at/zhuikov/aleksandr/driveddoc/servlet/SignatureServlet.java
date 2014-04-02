package com.gmail.at.zhuikov.aleksandr.driveddoc.servlet;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gmail.at.zhuikov.aleksandr.driveddoc.model.IdSignSession;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.DigiDocService;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.GDriveService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.drive.model.File;
import com.google.drive.samples.dredit.CredentialManager;
import com.google.drive.samples.dredit.DrEditServlet;
import com.google.gson.Gson;

import ee.sk.digidoc.SignedDoc;

@Singleton
public class SignatureServlet extends DrEditServlet {

	private static final long serialVersionUID = 1L;
	
	private DigiDocService digiDocService;
	private GDriveService gDriveService;
	
	@Inject
	public SignatureServlet(DigiDocService digiDocService, GDriveService gDriveService, JsonFactory jsonFactory, CredentialManager credentialManager) {
		super(jsonFactory, credentialManager);
		this.digiDocService = digiDocService;
		this.gDriveService = gDriveService;
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

			File file = gDriveService.getFile(signatureRequest.fileId, credential);
			InputStream content = gDriveService.downloadContent(file, credential);
			SignedDoc signedDoc = digiDocService.parseSignedDoc(content).getSignedDoc();
			req.getSession().setAttribute("ddoc", signedDoc);
			IdSignSession signSession = digiDocService.prepareSignature(signedDoc, signatureRequest.cert);
			sendJson(resp, signSession);

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
