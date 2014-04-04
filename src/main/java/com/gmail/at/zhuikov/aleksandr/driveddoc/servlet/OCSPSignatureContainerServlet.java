package com.gmail.at.zhuikov.aleksandr.driveddoc.servlet;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gmail.at.zhuikov.aleksandr.driveddoc.model.SignatureContainerDescription;
import com.gmail.at.zhuikov.aleksandr.driveddoc.repository.SignatureContainerDescriptionRepository;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.CredentialManager;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.SignatureContainerService;
import com.google.api.client.json.JsonFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreInputStream;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.drive.samples.dredit.DrEditServlet;

@Singleton
public class OCSPSignatureContainerServlet extends DrEditServlet {

	@Inject
	public OCSPSignatureContainerServlet(JsonFactory jsonFactory, CredentialManager credentialManager) {
		super(jsonFactory, credentialManager);
	}

	private static final long serialVersionUID = 1L;

	BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
	SignatureContainerDescriptionRepository signatureContainerDescriptionRepository = 
			SignatureContainerDescriptionRepository.getInstance();
	SignatureContainerService signatureContainerService = SignatureContainerService.getInstance(); 
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		sendJson(resp, signatureContainerDescriptionRepository.get(getUserId(req)));
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		String password = req.getParameter("password");
		BlobKey key = blobstoreService.getUploads(req).get("file").get(0);

		if (!signatureContainerService.isValid(new BlobstoreInputStream(key), password)) {
			signatureContainerDescriptionRepository.delete(getUserId(req));
			sendError(resp, 400, "Wrong password or file");
			return;
		}
		
		signatureContainerDescriptionRepository.store(new SignatureContainerDescription(
				key, 
				password, 
				getUserId(req)));
	}
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		signatureContainerDescriptionRepository.delete(getUserId(req));
	}
}
