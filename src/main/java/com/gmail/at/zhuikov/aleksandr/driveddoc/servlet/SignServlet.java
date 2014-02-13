package com.gmail.at.zhuikov.aleksandr.driveddoc.servlet;

import static ee.sk.digidoc.factory.DigiDocServiceFactory.STAT_SIGNATURE;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gmail.at.zhuikov.aleksandr.driveddoc.model.SignSession;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.DigiDocService;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.GDriveService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.drive.model.File;
import com.google.drive.samples.dredit.DrEditServlet;

import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.SignedDoc;

@Singleton
public class SignServlet extends DrEditServlet {

	private static final long serialVersionUID = 1L;
	
	private DigiDocService digiDocService;
	private GDriveService gDriveService;
	
	@Inject
	public SignServlet(GDriveService gDriveService, DigiDocService digiDocService, JsonFactory jsonFactory) {
		super(jsonFactory);
		this.gDriveService = gDriveService;
		this.digiDocService = digiDocService;
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

			File file = gDriveService.getFile(fileId, credential);
			InputStream content = gDriveService.downloadContent(file, credential);
			SignedDoc signedDoc = digiDocService.parseSignedDoc(content).getSignedDoc();
			SignSession signSession = digiDocService.requestMobileIdSignature(
					signedDoc, personalId, phoneNumber);
			
			sendJson(resp, signSession);

		} catch (GoogleJsonResponseException e) {
			if (e.getStatusCode() == 401) {
				// The user has revoked our token or it is otherwise bad.
				// Delete the local copy so that their next page load will
				// recover.
//				deleteCredential(req, resp);

			}

			sendGoogleJsonResponseError(resp, e);
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		String fileId = req.getParameter("file_id");
		String sessionId = req.getParameter("sessionId");
		
		Credential credential = getCredential();
		
		if (fileId == null) {
			sendError(resp, 400, "The `file_id` URI parameter must be specified.");
			return;
		}
		
		if (sessionId == null) {
			sendError(resp, 400, "The `sessionId` URI parameter must be specified.");
			return;
		}
		
		File file = gDriveService.getFile(fileId, credential);
		InputStream content = gDriveService.downloadContent(file, credential);
		SignedDoc signedDoc = digiDocService.parseSignedDoc(content).getSignedDoc();
		String status = digiDocService.getMobileIdSignatureStatus(signedDoc, sessionId);
		
		if (STAT_SIGNATURE.equals(status)) {
			try {
				gDriveService.updateContent(file, signedDoc.toXML().getBytes(), credential);
			} catch (DigiDocException e) {
				throw new RuntimeException(e);
			}
		}
		
		sendJson(resp, status);
	}
}
