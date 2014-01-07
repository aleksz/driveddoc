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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gmail.at.zhuikov.aleksandr.driveddoc.model.ClientContainer;
import com.gmail.at.zhuikov.aleksandr.driveddoc.model.ClientSignature;
import com.gmail.at.zhuikov.aleksandr.driveddoc.model.FileInContainer;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
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
import ee.sk.digidoc.factory.DigiDocFactory;
import ee.sk.utils.ConfigManager;


@Singleton
public class FileServlet extends DrEditServlet {
	
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = Logger.getLogger(FileServlet.class.getName());
	
  /**
   * Given a {@code file_id} URI parameter, return a JSON representation
   * of the given file.
   */
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
	  
    Drive drive = getDriveService(getCredential(req, resp));
    String fileId = req.getParameter("file_id");

    if (fileId == null) {
      sendError(resp, 400, "The `file_id` URI parameter must be specified.");
      return;
    }
    
    try {
    	
		File file = drive.files().get(fileId).execute();
		String containerFileIndex = req.getParameter("index");
		
		if (containerFileIndex == null) {
			sendJson(resp, downloadFileContent(drive, file));
		} else {
			download(resp, drive, file, containerFileIndex);
		}
	  
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 401) {
        // The user has revoked our token or it is otherwise bad.
        // Delete the local copy so that their next page load will recover.
        deleteCredential(req, resp);
        
      }
      
      sendGoogleJsonResponseError(resp, e);
    }
  }

	private void download(HttpServletResponse resp, Drive drive, File file,
			String containerFileIndex) throws IOException {
		
		SignedDoc signedDoc = getSignedDoc(drive, file);
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
    Drive service = getDriveService(getCredential(req, resp));
    ClientFile clientFile = new ClientFile(req.getReader());
    File file = clientFile.toFile();

    if (!clientFile.content.equals("")) {
      file = service.files().insert(file,
          ByteArrayContent.fromString(clientFile.mimeType, clientFile.content))
          .execute();
    } else {
      file = service.files().insert(file).execute();
    }
    sendJson(resp, file.getId());
  }

  /**
   * Update a file given a JSON representation, and return the JSON
   * representation of the created file.
   */
  @Override
  public void doPut(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    boolean newRevision = req.getParameter("newRevision").equals(Boolean.TRUE);
    Drive service = getDriveService(getCredential(req, resp));
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
	private ClientContainer downloadFileContent(Drive drive, File file)
			throws IOException {

		ClientContainer container = new ClientContainer();
		container.title = file.getTitle();
		container.id = file.getId();
		
		SignedDoc signedDoc = getSignedDoc(drive, file);
		
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

	private SignedDoc getSignedDoc(Drive drive, File file) throws IOException {
		HttpResponse response = drive.getRequestFactory().buildGetRequest(
				new GenericUrl(file.getDownloadUrl())).execute();
	
		return getSignedDoc(response.getContent());
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

	private SignedDoc getSignedDoc(InputStream content) {
		try {
			LOG.finest("Loading signed doc");
			ConfigManager.init("jar://jdigidoc.cfg");
			DigiDocFactory factory = ConfigManager.instance().getDigiDocFactory();
			SignedDoc doc = factory.readDigiDocFromStream(content);
			LOG.finest("Loaded signed doc");
			return doc;
		} catch (DigiDocException e) {
			throw new RuntimeException(e);
		}
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
