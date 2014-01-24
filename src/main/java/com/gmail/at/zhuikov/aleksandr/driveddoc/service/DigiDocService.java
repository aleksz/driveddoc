package com.gmail.at.zhuikov.aleksandr.driveddoc.service;

import static ee.sk.digidoc.SignedDoc.bin2hex;
import static ee.sk.digidoc.SignedDoc.hex2bin;
import static ee.sk.digidoc.SignedDoc.readCertificate;
import static ee.sk.digidoc.factory.DigiDocServiceFactory.STAT_OUTSTANDING_TRANSACTION;

import java.io.InputStream;
import java.util.logging.Logger;

import javax.inject.Singleton;

import com.gmail.at.zhuikov.aleksandr.driveddoc.model.DigidocOCSPSignatureContainer;
import com.gmail.at.zhuikov.aleksandr.driveddoc.model.IdSignSession;
import com.gmail.at.zhuikov.aleksandr.driveddoc.model.SignSession;

import ee.sk.digidoc.DataFile;
import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.Signature;
import ee.sk.digidoc.SignedDoc;
import ee.sk.digidoc.factory.DigiDocFactory;
import ee.sk.digidoc.factory.DigiDocServiceFactory;
import ee.sk.utils.ConfigManager;

@Singleton
public class DigiDocService {

	private static final Logger LOG = Logger.getLogger(DigiDocService.class.getName());
	
	public DigiDocService() {
		ConfigManager.init("jar://jdigidoc.cfg");
	}
	
	public SignedDoc parseSignedDoc(InputStream content) {
		try {
			DigiDocFactory factory = ConfigManager.instance().getDigiDocFactory();
			SignedDoc doc = factory.readDigiDocFromStream(content);
			return doc;
		} catch (DigiDocException e) {
			LOG.warning("Could not parse file: " + e.getMessage());
			return null;
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
			return new IdSignSession(signature.getId(), digest);
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
		SignedDoc signedDoc = new SignedDoc();
		
		try {
			DataFile dataFile = new DataFile(
					signedDoc.getNewDataFileId(), 
					DataFile.CONTENT_EMBEDDED_BASE64, 
					fileName, 
					mimeType, 
					signedDoc);
			
			dataFile.setBodyFromStream(content);
			signedDoc.addDataFile(dataFile);

		} catch (DigiDocException e) {
			throw new RuntimeException(e);
		}
	
		return signedDoc;
	}
}
