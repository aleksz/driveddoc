package com.gmail.at.zhuikov.aleksandr.driveddoc.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import com.gmail.at.zhuikov.aleksandr.driveddoc.model.container.ClientContainer;
import com.gmail.at.zhuikov.aleksandr.driveddoc.model.container.ClientSignature;
import com.gmail.at.zhuikov.aleksandr.driveddoc.model.container.FileInContainer;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.drive.model.File;

import ee.sk.digidoc.DataFile;
import ee.sk.digidoc.DigiDocException;
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
	
	public DataFile  getFile(String fileId, int index, Credential credential) throws IOException {
		File file = gDriveService.getFile(fileId, credential);
		SignedDoc signedDoc = digiDocService.parseSignedDoc(
				gDriveService.downloadContent(file, credential)).getSignedDoc();
	
		return signedDoc.getDataFile(new Integer(index));
	}
	
	public void saveFileToDrive(String containerFileId, int index, Credential credential) throws IOException {
		File containerFile = gDriveService.getFile(containerFileId, credential);
		
		SignedDoc signedDoc = digiDocService.parseSignedDoc(
				gDriveService.downloadContent(containerFile, credential)).getSignedDoc();
	
		DataFile dataFile = signedDoc.getDataFile(new Integer(index));
		
		File file = new File();
		file.setTitle(dataFile.getFileName());
		file.setParents(containerFile.getParents());
		
		try {
			gDriveService.insertFile(file, dataFile.getBodyAsStream(), credential);
		} catch (DigiDocException e) {
			throw new RuntimeException(e);
		}
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
			
			FileInContainer fileInContainer = new FileInContainer(dataFile.getFileName(), dataFile.getMimeType());
			container.files.add(fileInContainer);
		}
	}
	
	private ClientSignature getSignature(Signature signature,
			SignedDoc signedDoc) {

		Collection<String> errors = new ArrayList<>();

		for (Object errorObject : signature.verify(signedDoc, false, false)) {
			errors.add(errorObject.toString());
		}

		KeyInfo keyInfo = signature.getKeyInfo();

		return new ClientSignature(
				keyInfo.getSubjectFirstName() + " "	+ keyInfo.getSubjectLastName(),
				keyInfo.getSubjectPersonalCode(), 
				signature.getSignedProperties().getSigningTime(),
				errors);
	}
	
	public ClientContainer createNewDDocWithFile(File file, InputStream content, Credential credential) throws IOException {
		SignedDoc container = digiDocService.createContainer(file.getTitle(),
				file.getMimeType(),
				gDriveService.downloadContent(file, credential));

		File containerFile = new File();
		containerFile.setTitle(file.getTitle() + ".ddoc");
		containerFile.setMimeType("application/ddoc");
		containerFile.setParents(file.getParents());

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			container.writeToStream(os);
		} catch (DigiDocException e) {
			throw new RuntimeException(e);
		}

		containerFile = gDriveService.insertFile(containerFile, 	new ByteArrayInputStream(os.toByteArray()), credential);
		
		ClientContainer clientContainer = new ClientContainer();
		clientContainer.title = containerFile.getTitle();
		clientContainer.id = containerFile.getId();
		
		extractFiles(container, clientContainer);
		
		return clientContainer;
	}
}
