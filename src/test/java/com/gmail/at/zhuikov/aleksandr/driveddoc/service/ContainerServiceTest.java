package com.gmail.at.zhuikov.aleksandr.driveddoc.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.gmail.at.zhuikov.aleksandr.driveddoc.MockitoTest;
import com.gmail.at.zhuikov.aleksandr.driveddoc.RestrictedFileWritingRule;
import com.gmail.at.zhuikov.aleksandr.driveddoc.model.IdSignSession;
import com.gmail.at.zhuikov.aleksandr.driveddoc.model.SignatureContainerDescription;
import com.gmail.at.zhuikov.aleksandr.driveddoc.repository.SignatureContainerDescriptionRepository;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.drive.model.File;
import com.google.appengine.api.blobstore.BlobKey;

import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.SignedDoc;
import ee.sk.utils.ConfigManager;

public class ContainerServiceTest extends MockitoTest {

	@Mock Credential credential;
	@Mock SignatureContainerDescriptionRepository signatureContainerDescriptionRepository;
	@Mock CachedDigiDocService digiDocService;
	@Mock GDriveService gDriveService;
	@InjectMocks ContainerService service;
	
	{
		ConfigManager.init("jar://jdigidoc.cfg");
	}
	
	@Rule public RestrictedFileWritingRule rule = new RestrictedFileWritingRule();
	
	@Test
	public void finalizeBDocSignature() throws Exception {
		SignedDoc doc = parseSignedDoc(getClass().getResourceAsStream("/test.bdoc"));
		
		IdSignSession signSession = new IdSignSession("id", "digest", doc);
		File file = new File();
		
		SignatureContainerDescription signatureContainerDescription = new SignatureContainerDescription(new BlobKey("1"), "pass", "user");
		when(signatureContainerDescriptionRepository.get("user")).thenReturn(new SignatureContainerDescription(new BlobKey("1"), "pass", "user"));
		when(signatureContainerDescriptionRepository.getContent(signatureContainerDescription)).thenReturn(new ByteArrayInputStream("abc".getBytes()));
		when(gDriveService.getFile("file", credential)).thenReturn(file);
		
		service.finalizeSignature(signSession, "signature", "user", "file", credential);
		
		 ArgumentCaptor<byte[]> argument = ArgumentCaptor.forClass(byte[].class);
		verify(gDriveService).updateContent(eq(file), argument.capture(), eq(credential));
		SignedDoc parsedSignedDoc = parseSignedDoc(new ByteArrayInputStream(argument.getValue()));
		assertEquals(1, parsedSignedDoc.getDataFiles().size());
		assertEquals(3547, parsedSignedDoc.getDataFile(0).getSize());
	}

	protected SignedDoc parseSignedDoc(InputStream input) throws DigiDocException {
		return ConfigManager.instance().getDigiDocFactory().readSignedDocFromStreamOfType(
				input, true, new ArrayList<>());
	}
}
