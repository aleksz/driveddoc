package com.google.drive.samples.dredit.model;

public class OAuthSecrets {

	private String clientId;
	private String clientSecrets;
	
	public OAuthSecrets(String clientId, String clientSecrets) {
		this.clientId = clientId;
		this.clientSecrets = clientSecrets;
	}
	
	public String getClientId() {
		return clientId;
	}
	
	public String getClientSecrets() {
		return clientSecrets;
	}
}
