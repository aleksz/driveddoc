package com.gmail.at.zhuikov.aleksandr.driveddoc.model;

import com.google.appengine.api.blobstore.BlobKey;

public class SignatureContainerDescription {

	private BlobKey key;
	private String password;
	private String userId;
	
	public SignatureContainerDescription(BlobKey key, String password, String userId) {
		this.key = key;
		this.password = password;
		this.userId = userId;
	}
	
	public BlobKey getKey() {
		return key;
	}
	
	public String getPassword() {
		return password;
	}
	
	public String getUserId() {
		return userId;
	}
}
