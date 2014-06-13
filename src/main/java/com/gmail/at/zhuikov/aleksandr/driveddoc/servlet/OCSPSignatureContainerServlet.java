package com.gmail.at.zhuikov.aleksandr.driveddoc.servlet;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gmail.at.zhuikov.aleksandr.driveddoc.model.SignatureContainerDescription;
import com.gmail.at.zhuikov.aleksandr.driveddoc.repository.SignatureContainerDescriptionRepository;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.CredentialManager;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.SignatureContainerService;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.UserService;
import com.google.api.client.json.JsonFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreInputStream;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.drive.samples.dredit.DrEditServlet;

@Singleton
public class OCSPSignatureContainerServlet extends DrEditServlet {

	private static final long serialVersionUID = 1L;

	private final BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
	private final SignatureContainerDescriptionRepository signatureContainerDescriptionRepository;
	private final SignatureContainerService signatureContainerService = SignatureContainerService.getInstance(); 
	private final UserService userService;
	
	@Inject
	public OCSPSignatureContainerServlet(JsonFactory jsonFactory, CredentialManager credentialManager,
			SignatureContainerDescriptionRepository signatureContainerDescriptionRepository, UserService userService) {
		super(jsonFactory, credentialManager);
		this.signatureContainerDescriptionRepository = signatureContainerDescriptionRepository;
		this.userService = userService;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		sendJson(resp, signatureContainerDescriptionRepository.get(getUserId(req)));
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		String password = req.getParameter("password");
		Map<String, List<BlobKey>> uploads = blobstoreService.getUploads(req);
		
		if (!uploads.containsKey("file")) {
			sendError(resp, 400, "No certificate file");
			return;
		}
		
		BlobKey key = uploads.get("file").get(0);

		if (!signatureContainerService.isValid(new BlobstoreInputStream(key), password)) {
			signatureContainerDescriptionRepository.delete(getUserId(req));
			sendError(resp, 400, "Wrong password or file");
			return;
		}
		
		if (!checkMasterPermission(req)) {
			sendError(resp, 403, "Wrong permissions");
			return;
		}
		
		signatureContainerDescriptionRepository.store(new SignatureContainerDescription(
				key, 
				password, 
				isMaster(req) ? "master" : getUserId(req)));
	}
	
	private boolean checkMasterPermission(HttpServletRequest req) throws IOException, ServletException {
		return "aleksandr.zhuikov@gmail.com".equals(
				userService.getUserInfo(getUserId(req), getCredential()).getEmail()); //TODO: this is awful :)
	}
	
	private boolean isMaster(HttpServletRequest request) {
		return request.getParameter("master") != null;
	}
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		signatureContainerDescriptionRepository.delete(getUserId(req));
	}
}
