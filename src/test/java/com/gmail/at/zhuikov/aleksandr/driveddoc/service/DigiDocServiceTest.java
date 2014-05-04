package com.gmail.at.zhuikov.aleksandr.driveddoc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;

import com.gmail.at.zhuikov.aleksandr.driveddoc.MockitoTest;
import com.gmail.at.zhuikov.aleksandr.driveddoc.RestrictedFileWritingRule;
import com.gmail.at.zhuikov.aleksandr.driveddoc.model.ValidatedSignedDoc;

import ee.sk.digidoc.SignedDoc;
import ee.sk.utils.ConfigManager;

public class DigiDocServiceTest extends MockitoTest {

	@InjectMocks DigiDocService service;
	
	@Rule public RestrictedFileWritingRule rule = new RestrictedFileWritingRule();
	
	@Test(expected = SecurityException.class)
	public void writingToFileIsNotAllowed() throws IOException {
		File.createTempFile("asf", "sdfg");
	}
	
	@Test
	public void wrapsOriginalFile() throws Exception {
		String content = "abc";
		SignedDoc container = service.createContainer("test.txt", "text/plain", new ByteArrayInputStream(content.getBytes()));
		assertNotNull(container);
		assertNotNull(container.getDataFile(0));
		assertEquals(content, container.getDataFile(0).getBodyAsString());
		assertTrue(container.getDataFile(0).getSize() > 0);
		container.writeToStream(System.out);
	}
	
	@Test
	public void createdContainerWritesDataFileBody() throws Exception {
		String content = "abc";
		SignedDoc container = service.createContainer("test.txt", "text/plain", new ByteArrayInputStream(content.getBytes()));
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		container.writeToStream(os);
		SignedDoc parsed = ConfigManager	.instance()	.getDigiDocFactory()
				.readDigiDocFromStream(	new ByteArrayInputStream(os.toByteArray()));
		assertEquals(content, parsed.getDataFile(0).getBodyAsString());
	}
	
	@Test
	public void parsesPreThreeDotEightDigiDoc() throws IOException {
		ValidatedSignedDoc doc = service.parseSignedDoc(getClass().getResourceAsStream("/pre_3_8.ddoc"), "id");
		assertNotNull(doc);
		assertNotNull(doc.getSignedDoc());
		assertTrue(doc.getWarnings().isEmpty());
		assertFalse(doc.getSignedDoc().getDataFiles().isEmpty());
	}
	
	@Test
	public void parsesPreThreeDotEightSignedDigiDoc() throws IOException {
		ValidatedSignedDoc doc = service.parseSignedDoc(getClass().getResourceAsStream("/pre_3_8_signed.ddoc"), "id");
		assertNotNull(doc);
		assertNotNull(doc.getSignedDoc());
		assertFalse(doc.getSignedDoc().getDataFiles().isEmpty());
	}
	
	@Test
	public void returnsWarningForPreThreeDotEightSignedDigiDoc() throws IOException {
		ValidatedSignedDoc doc = service.parseSignedDoc(getClass().getResourceAsStream("/pre_3_8_signed.ddoc"), "id");
		System.out.println(doc.getWarnings());
		assertFalse(doc.getWarnings().isEmpty());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void throwsExceptionWhenParsingNonDigiDocFile() throws IOException {
		 service.parseSignedDoc(getClass().getResourceAsStream("/504950.p12d"), "id");
	}
}
