package com.gmail.at.zhuikov.aleksandr.driveddoc.model;

public class IdSignSession {

	private String id;
	private String digest;
	
	public IdSignSession(String id, String digest) {
		this.id = id;
		this.digest = digest;
	}
	
	public String getId() {
		return id;
	}
	
	public String getDigest() {
		return digest;
	}
}
