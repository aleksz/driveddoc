package com.gmail.at.zhuikov.aleksandr.jdigidoc;

import ee.sk.digidoc.DataFile;
import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.SignedDoc;

public class DataFileWithoutCache extends DataFile {

	private static final long serialVersionUID = 1L;

	public DataFileWithoutCache(String id, String contentType, String fileName,
			String mimeType, SignedDoc sdoc) throws DigiDocException {
		super(id, contentType, fileName, mimeType, sdoc);
	}

	@Override
	public boolean schouldUseTempFile() {
		return false;//eat this !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! :P
	}
}