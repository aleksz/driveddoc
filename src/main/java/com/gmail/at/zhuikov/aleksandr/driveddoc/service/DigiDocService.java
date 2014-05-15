package com.gmail.at.zhuikov.aleksandr.driveddoc.service;

import static ee.sk.digidoc.SignedDoc.bin2hex;
import static ee.sk.digidoc.SignedDoc.hex2bin;
import static ee.sk.digidoc.SignedDoc.readCertificate;
import static ee.sk.digidoc.factory.DigiDocServiceFactory.STAT_OUTSTANDING_TRANSACTION;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.inject.Singleton;

import org.apache.commons.compress.utils.IOUtils;

import com.gmail.at.zhuikov.aleksandr.driveddoc.model.DigidocOCSPSignatureContainer;
import com.gmail.at.zhuikov.aleksandr.driveddoc.model.IdSignSession;
import com.gmail.at.zhuikov.aleksandr.driveddoc.model.SignSession;
import com.gmail.at.zhuikov.aleksandr.driveddoc.model.ValidatedSignedDoc;
import com.gmail.at.zhuikov.aleksandr.jdigidoc.FlexibleBouncyCastleNotaryFactory;

import ee.sk.digidoc.DataFile;
import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.Signature;
import ee.sk.digidoc.SignedDoc;
import ee.sk.digidoc.factory.DigiDocFactory;
import ee.sk.digidoc.factory.DigiDocGenFactory;
import ee.sk.digidoc.factory.DigiDocServiceFactory;
import ee.sk.utils.ConfigManager;

@Singleton
public class DigiDocService {

	private static final Logger LOG = Logger.getLogger(DigiDocService.class.getName());
	
	public DigiDocService() {
		ConfigManager.init("jar://jdigidoc.cfg");
	}
	
	/**
	 * @throws IllegalArgumentException if content is not DDoc
	 * @param content
	 * @return
	 * @throws IOException 
	 */
	public ValidatedSignedDoc parseSignedDoc(String fileName,  String id,  InputStream content) throws IOException  {
		
		ArrayList<DigiDocException> warnings = new ArrayList<DigiDocException>();

		try {
			DigiDocFactory factory = ConfigManager.instance().getDigiDocFactory();
			SignedDoc doc = factory.readSignedDocFromStreamOfType(content, factory.isBdocExtension(fileName), warnings);
			
			if (doc == null) {
				throw new IllegalArgumentException("content is not a ddoc/bdoc: " + warnings);
			}
			
			return new ValidatedSignedDoc(doc, warnings);
			
		} catch (DigiDocException e) {
			throw new RuntimeException(e);
		}
	}
	
	public SignSession requestMobileIdSignature(
			SignedDoc doc, String personalId, String phoneNumber) {
		
		try {
			StringBuffer challenge = new StringBuffer();
			String sessionId = DigiDocServiceFactory.ddsSign(
					doc, 
					personalId,
					phoneNumber,
					"ENG", 
					"Testimine",//TODO: use correct service name 
					"", 
					"", 
					"", 
					"", 
					"EE",
					challenge);
			
			return new SignSession(sessionId, challenge.toString());
		} catch (DigiDocException e) {
			throw new RuntimeException(e);
		}
	}
	
	public boolean isWaitingForMobileIdPin(SignedDoc doc, String challenge) {
		return STAT_OUTSTANDING_TRANSACTION
				.equals(getMobileIdSignatureStatus(doc, challenge));
	}

	public String getMobileIdSignatureStatus(SignedDoc doc, String challenge) {
		try {
			return DigiDocServiceFactory.ddsGetStatus(doc, challenge);
		} catch (DigiDocException e) {
			throw new RuntimeException(e);
		}
	}
	
	public IdSignSession prepareSignature(SignedDoc doc, String cert) {
		try {
			Signature signature = doc.prepareSignature(readCertificate(hex2bin(cert)), null, null);
			String digest = bin2hex(signature.calculateSignedInfoDigest());
			return new IdSignSession(signature.getId(), digest, doc);
		} catch (DigiDocException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void finalizeSignature(SignedDoc doc, String signatureId, String signatureValue) {
		try {
			Signature signature = doc.findSignatureById(signatureId);
			signature.setSignatureValue(hex2bin(signatureValue));
			signature.getConfirmation();
		} catch (DigiDocException e) {
			throw new RuntimeException(e);
		}
	}

	public void finalizeSignature(SignedDoc signedDoc, String signatureId,
			String signatureValue, DigidocOCSPSignatureContainer signatureContainer) {
		FlexibleBouncyCastleNotaryFactory.ocspSignCert.set(signatureContainer);
		finalizeSignature(signedDoc, signatureId, signatureValue);
	}

	public SignedDoc createContainer(String fileName, String mimeType, InputStream content) {

		try {
			SignedDoc signedDoc = DigiDocGenFactory.createSignedDoc(
					SignedDoc.FORMAT_DIGIDOC_XML, null, null);
			
			DataFile dataFile = new DataFile(
					signedDoc.getNewDataFileId(), 
					DataFile.CONTENT_EMBEDDED_BASE64, 
					fileName, 
					mimeType, 
					signedDoc);
			
			byte[] contentBytes = IOUtils.toByteArray(content);
			dataFile.setBase64Body(contentBytes);
			dataFile.calcHashes(new ByteArrayInputStream(contentBytes));//hack to make DigiDoc save file content
			signedDoc.addDataFile(dataFile);
			return signedDoc;

		} catch (DigiDocException | IOException e) {
			throw new RuntimeException(e);
		}
	}
}
