package com.gmail.at.zhuikov.aleksandr.driveddoc.model;

public class SignSession {

	private String sessionId;
	private String challenge;
	
	public SignSession(String sessionId, String challenge) {
		this.sessionId = sessionId;
		this.challenge = challenge;
	}
	
	public String getSessionId() {
		return sessionId;
	}
	
	public String getChallenge() {
		return challenge;
	}
}
