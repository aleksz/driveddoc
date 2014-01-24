/*
 * Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.drive.samples.dredit;

import static java.util.Arrays.asList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sun.awt.image.ByteArrayImageSource;

import com.gmail.at.zhuikov.aleksandr.driveddoc.model.ClientContainer;
import com.gmail.at.zhuikov.aleksandr.driveddoc.model.ClientSignature;
import com.gmail.at.zhuikov.aleksandr.driveddoc.model.FileInContainer;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.DigiDocService;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.GDriveService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.IOUtils;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.App;
import com.google.api.services.drive.model.App.Icons;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;
import com.google.drive.samples.dredit.model.ClientFile;

import ee.sk.digidoc.DataFile;
import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.KeyInfo;
import ee.sk.digidoc.Signature;
import ee.sk.digidoc.SignedDoc;


@Singleton
public class FileServlet extends DrEditServlet {
	
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = Logger.getLogger(FileServlet.class.getName());
	
	private DigiDocService digiDocService;
	private GDriveService gDriveService;
	
	@Inject
	public FileServlet(GDriveService gDriveService, DigiDocService digiDocService, JsonFactory jsonFactory) {
		super(jsonFactory);
		this.gDriveService = gDriveService;
		this.digiDocService = digiDocService;
	}
	
  /**
   * Given a {@code file_id} URI parameter, return a JSON representation
   * of the given file.
   */
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
	  
    Credential credential = getCredential();
    String fileId = req.getParameter("file_id");

    if (fileId == null) {
      sendError(resp, 400, "The `file_id` URI parameter must be specified.");
      return;
    }
    
    try {
    	
		File file = gDriveService.getFile(fileId, credential);
		String containerFileIndex = req.getParameter("index");
		
		if (containerFileIndex == null) {
			ClientContainer fileContent = downloadFileContent(credential, file);
			
			if (fileContent == null) {
				sendError(resp, 400, "Could not read file");
			} else {
				sendJson(resp, fileContent);
			}
			
		} else {
			download(resp, credential, file, containerFileIndex);
		}
	  
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 401) {
        // The user has revoked our token or it is otherwise bad.
        // Delete the local copy so that their next page load will recover.
//        deleteCredential(req, resp);
        
      }
      
      sendGoogleJsonResponseError(resp, e);
    }
  }

	private void download(HttpServletResponse resp, Credential credential, File file,
			String containerFileIndex) throws IOException {
		
		SignedDoc signedDoc = digiDocService.parseSignedDoc(
				gDriveService.downloadContent(file, credential));
		DataFile dataFile = signedDoc.getDataFile(new Integer(containerFileIndex));
		
		resp.setContentType("application/x-download");
		resp.setHeader("Content-Disposition", "attachment; filename=" + dataFile.getFileName());
		
		try {
			IOUtils.copy(dataFile.getBodyAsStream(), resp.getOutputStream());
		} catch (DigiDocException e) {
			throw new RuntimeException(e);
		}
	}

  /**
   * Create a new file given a JSON representation, and return the JSON
   * representation of the created file.
   */
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
	  
	  String fileId = req.getParameter("fileId");

	  File file = gDriveService.getFile(fileId, getCredential());
	  
	  SignedDoc container = digiDocService.createContainer(
			  file.getTitle(),
			  file.getMimeType(), 
			  gDriveService.downloadContent(file, getCredential()));
	  
	  File containerFile = new File();
	  containerFile.setTitle(file.getTitle() + ".ddoc");
	  containerFile.setMimeType("application/ddoc");
	  containerFile.setParents(file.getParents());
		
	  ByteArrayOutputStream os = new ByteArrayOutputStream();
	  container.writeToStream(os);
	  
	  gDriveService.insertFile(file, new ByteArrayInputStream(os.toByteArray()), getCredential());

  }

  /**
   * Update a file given a JSON representation, and return the JSON
   * representation of the created file.
   */
  @Override
  public void doPut(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    boolean newRevision = req.getParameter("newRevision").equals(Boolean.TRUE);
    Drive service = getDriveService(getCredential());
    ClientFile clientFile = new ClientFile(req.getReader());
    File file = clientFile.toFile();
    // If there is content we update the given file
    if (clientFile.content != null) {
      file = service.files().update(clientFile.resource_id, file,
          ByteArrayContent.fromString(clientFile.mimeType, clientFile.content))
          .setNewRevision(newRevision).execute();
    } else { // If there is no content we patch the metadata only
      file = service.files()
          .patch(clientFile.resource_id, file)
          .setNewRevision(newRevision)
          .execute();
    }
    sendJson(resp, file.getId());
  }

  /**
   * Download the content of the given file.
   *
   * @param service Drive service to use for downloading.
   * @param file File metadata object whose content to download.
   * @return String representation of file content.  String is returned here
   *         because this app is setup for text/plain files.
   * @throws IOException Thrown if the request fails for whatever reason.
   */
	private ClientContainer downloadFileContent(Credential credential, File file)
			throws IOException {

		ClientContainer container = new ClientContainer();
		container.title = file.getTitle();
		container.id = file.getId();
		
		SignedDoc signedDoc = digiDocService.parseSignedDoc(
				gDriveService.downloadContent(file, credential));
		
		if (signedDoc == null) {
			return null;
		}
		
		addSignatures(container, signedDoc);

		for (Object dataFileObject : signedDoc.getDataFiles()) {
			DataFile dataFile = (DataFile) dataFileObject;
			
//			File tmpFile = insertFile(drive, dataFile);
			FileInContainer fileInContainer = new FileInContainer();
			fileInContainer.title = dataFile.getFileName();
//			fileInContainer.iconLink = tmpFile.getIconLink();
			
//			for (Map.Entry<String, String> link : tmpFile.getOpenWithLinks().entrySet()) {
//				App app = getApp(drive, link.getKey());
//				fileInContainer.apps.add(new ClientApp(
//						app.getName(), 
//						getIcon(app).getIconUrl(),
//						link.getValue()));
//			}
			
			container.files.add(fileInContainer);
		}

		return container;
	}

	private void addSignatures(ClientContainer container, SignedDoc signedDoc) {
		if (signedDoc.getSignatures() != null) {
			for (Object signatureObject : signedDoc.getSignatures()) {
				Signature signature = (Signature) signatureObject;
				ClientSignature clientSignature = getSignature(signature, signedDoc);
				LOG.fine("Adding signature " + clientSignature);
				container.signatures.add(clientSignature);
			}
		}
	}

	private ClientSignature getSignature(Signature signature, SignedDoc signedDoc) {
		
		Collection<String> errors = new ArrayList<>();
		
        for (Object errorObject : signature.verify(signedDoc, false, false)) {
        	errors.add(errorObject.toString());
        }
		
		KeyInfo keyInfo = signature.getKeyInfo();
		
		return new ClientSignature(
				keyInfo.getSubjectFirstName() + " " + keyInfo.getSubjectLastName(),
				keyInfo.getSubjectPersonalCode(),
				signature.getSignedProperties().getSigningTime(),
				errors);
	}
	
	private Icons getIcon(App app) {
		for (Icons icon : app.getIcons()) {
			if ("application".equals(icon.getCategory()) && 16 == icon.getSize()) {
				return icon;
			}
		}
		
		return null;
	}
	
	private App getApp(Drive drive, String id) throws IOException {
		return drive.apps().get(id).execute();
	}

	private File insertFile(Drive drive, DataFile file) throws IOException {
		try {
			LOG.finest("Saving temporary file " + file.getFileName());

			String mimeType = "file".equals(file.getMimeType()) ? null : file.getMimeType();
			
			File descriptor = new File();
			descriptor.setTitle(file.getFileName());
			descriptor.setMimeType(mimeType);
			descriptor.setParents(asList(new ParentReference().setId("appdata")));
			
			ByteArrayContent content = new ByteArrayContent(
					mimeType,
					file.getBodyAsData());
			
			File tmpFile = drive.files().insert(descriptor, content).execute();
			
			LOG.finest("Saved temporary file " + tmpFile.getTitle());
			
			return tmpFile;
			
		} catch (DigiDocException e) {
			throw new RuntimeException(e);
		}
	}
}
