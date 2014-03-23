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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gmail.at.zhuikov.aleksandr.driveddoc.model.container.ClientContainer;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.CachedContainerService;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.ContainerService;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.GDriveService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.IOUtils;
import com.google.api.services.drive.model.File;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import ee.sk.digidoc.DataFile;
import ee.sk.digidoc.DigiDocException;


@Singleton
public class FileServlet extends DrEditServlet {
	
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = Logger.getLogger(FileServlet.class.getName());
	
	private GDriveService gDriveService;
	private final ContainerService containerService;
	
	@Inject
	public FileServlet(GDriveService gDriveService, JsonFactory jsonFactory, CachedContainerService containerService) {
		super(jsonFactory);
		this.gDriveService = gDriveService;
		this.containerService = containerService;
	}
	
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
	  
    String fileId = req.getParameter("file_id");

    if (fileId == null) {
      sendError(resp, 400, "The `file_id` URI parameter must be specified.");
      return;
    }
    
    try {
    	
		String containerFileIndex = req.getParameter("index");
		
		if (containerFileIndex == null) {
			ClientContainer fileContent = containerService.getContainer(fileId, getCredential());
			
			if (fileContent == null) {
				sendError(resp, 400, "Could not read file");
			} else {
				sendJson(resp, fileContent);
			}
			
		} else {
			download(resp, getCredential(), fileId, new Integer(containerFileIndex));
		}
	  
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 401) {
        // The user has revoked our token or it is otherwise bad.
        // Delete the local copy so that their next page load will recover.
//        deleteCredential(req, resp);
        
      }
      sendGoogleJsonResponseError(resp, e);
    } catch (IllegalArgumentException e) {
  	  sendError(resp, 415, "Requested file is not DDoc");
    }
  }
  
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		Map<String, String> params = new Gson().fromJson(req.getReader(), new TypeToken<HashMap<String, String>>() {}.getType());

		String fileId = params.get("file_id");

		if (fileId == null) {
			sendError(resp, 400, "The `file_id` URI parameter must be specified.");
			return;
		}

		File file = gDriveService.getFile(fileId, getCredential());
		sendJson(resp, containerService.createNewDDocWithFile(file, gDriveService.downloadContent(file, getCredential()), getCredential()));
	}

	private void download(HttpServletResponse resp, Credential credential, String fileId,
			int containerFileIndex) throws IOException {
		
		DataFile dataFile = containerService.getFile(fileId, containerFileIndex, credential);
		
		resp.setContentType("application/x-download");
		resp.setHeader("Content-Disposition", "attachment; filename=" + dataFile.getFileName());
		
		try {
			IOUtils.copy(dataFile.getBodyAsStream(), resp.getOutputStream());
		} catch (DigiDocException e) {
			throw new RuntimeException(e);
		}
	}
}
