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
import com.google.api.client.json.JsonFactory;
import com.google.gson.Gson;

@Singleton
public class DriveFileServlet extends AuthorizationServlet {

	private static final long serialVersionUID = 1L;
	
	private final ContainerService containerService;

	@Inject
	public DriveFileServlet(JsonFactory jsonFactory, CachedContainerService containerService, CredentialManager credentialManager) {
		super(jsonFactory, credentialManager);
		this.containerService = containerService;
	}

	public static class SaveFileToDriveRequest {
		public String containerFileId;
		public int fileIndex;
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		SaveFileToDriveRequest saveRequest = new Gson().fromJson(
				req.getReader(),
				SaveFileToDriveRequest.class);
		
		containerService.saveFileToDrive(
				saveRequest.containerFileId,
				saveRequest.fileIndex,
				getCredential());
	}
}
