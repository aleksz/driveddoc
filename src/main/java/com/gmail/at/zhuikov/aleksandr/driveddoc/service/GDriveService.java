package com.gmail.at.zhuikov.aleksandr.driveddoc.service;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

@Singleton
public class GDriveService {
	
	private static final int DOWNLOAD_TIMEOUT = 30 * 1000;

	private HttpTransport transport;
	private JsonFactory jsonFactory;
	
	@Inject
	public GDriveService(HttpTransport transport, JsonFactory jsonFactory) {
		this.transport = transport;
		this.jsonFactory = jsonFactory;
	}
	
	/**
	 * Build and return a Drive service object based on given request
	 * parameters.
	 * 
	 * @param credential
	 *            User credentials.
	 * @return Drive service object that is ready to make requests, or null if
	 *         there was a problem.
	 */
	private Drive getDriveService(Credential credential) {
		return new Drive.Builder(transport, jsonFactory, credential)
				.setApplicationName("Drive DigiDoc").build();
	}
	
	public File getFile(String id, Credential credential) throws IOException {
		return getDriveService(credential).files().get(id).execute();
	}
	
	public InputStream downloadContent(File file, Credential credential) throws IOException {
		return downloadContent(getDriveService(credential), file);
	}
	
	private InputStream downloadContent(Drive drive, File file) throws IOException {
		return drive.getRequestFactory()
				.buildGetRequest(	new GenericUrl(file.getDownloadUrl()))
				.setReadTimeout(DOWNLOAD_TIMEOUT)
				.execute()
				.getContent();
	}
	
	public void updateContent(File file, byte[] content, Credential credential) throws IOException {
		getDriveService(credential).files().update(
				file.getId(), 
				file, 
				new ByteArrayContent(file.getMimeType(), content)).execute();
	}
	
	public File insertFile(File file,  InputStream content, Credential credential) throws IOException {
		return getDriveService(credential)
			.files()
			.insert(file, new InputStreamContent(file.getMimeType(), content))
			.execute();
	}
}
