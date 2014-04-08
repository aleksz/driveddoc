package com.gmail.at.zhuikov.aleksandr.driveddoc.model;

import java.io.Serializable;

import ee.sk.digidoc.SignedDoc;

public class IdSignSession implements Serializable {

	private static final long serialVersionUID = 1L;

	private String id;
	private String digest;
	private SignedDoc signedDoc;
	
	public IdSignSession(String id, String digest, SignedDoc signedDoc) {
		this.id = id;
		this.digest = digest;
		this.signedDoc = signedDoc;
	}
	
	public String getId() {
		return id;
	}
	
	public String getDigest() {
		return digest;
	}
	
	public SignedDoc getSignedDoc() {
		return signedDoc;
	}
}
