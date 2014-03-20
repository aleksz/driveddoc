package com.gmail.at.zhuikov.aleksandr.driveddoc.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import javax.inject.Singleton;

import com.gmail.at.zhuikov.aleksandr.driveddoc.model.ClientContainer;
import com.gmail.at.zhuikov.aleksandr.driveddoc.model.ClientSignature;
import com.gmail.at.zhuikov.aleksandr.driveddoc.model.FileInContainer;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.drive.model.File;

import ee.sk.digidoc.DataFile;
import ee.sk.digidoc.KeyInfo;
import ee.sk.digidoc.Signature;
import ee.sk.digidoc.SignedDoc;

public class ContainerService {
	
	private static final Logger LOG = Logger.getLogger(ContainerService.class.getName());

	private final GDriveService gDriveService;
	private final DigiDocService digiDocService;
	
	public ContainerService(GDriveService gDriveService, DigiDocService digiDocService) {
		this.gDriveService = gDriveService;
		this.digiDocService = digiDocService;
	}
	
	public ClientContainer getContainer(String fileId, Credential credential) throws IOException {
		File file = gDriveService.getFile(fileId, credential);
		return getContainer(credential, file);
	}

	protected ClientContainer getContainer(Credential credential, File file)
			throws IOException {
		InputStream content = gDriveService.downloadContent(file, credential);
		return readExistingDDoc(file, digiDocService.parseSignedDoc(content).getSignedDoc());
	}
	
	private ClientContainer readExistingDDoc(File file, SignedDoc signedDoc) {
		ClientContainer container = new ClientContainer();
		container.title = file.getTitle();
		container.id = file.getId();
		
		if (signedDoc == null) {
			return null;
		}
		
		addSignatures(container, signedDoc);

		extractFiles(signedDoc, container);
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
	
	private void extractFiles(SignedDoc signedDoc, ClientContainer container) {
		for (Object dataFileObject : signedDoc.getDataFiles()) {
			DataFile dataFile = (DataFile) dataFileObject;
			
			FileInContainer fileInContainer = new FileInContainer();
			fileInContainer.title = dataFile.getFileName();
			container.files.add(fileInContainer);
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
}
