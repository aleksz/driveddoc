package com.gmail.at.zhuikov.aleksandr.driveddoc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;

import com.gmail.at.zhuikov.aleksandr.driveddoc.MockitoTest;

import ee.sk.digidoc.SignedDoc;
import ee.sk.utils.ConfigManager;

public class DigiDocServiceTest extends MockitoTest {

	@InjectMocks DigiDocService service;
	
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
}
