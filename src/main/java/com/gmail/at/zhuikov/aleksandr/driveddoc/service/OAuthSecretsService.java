package com.gmail.at.zhuikov.aleksandr.driveddoc.service;

import javax.inject.Singleton;

import com.google.drive.samples.dredit.model.OAuthSecrets;

@Singleton
public class OAuthSecretsService {

	public OAuthSecrets get() {
		return new OAuthSecrets("610309933249.apps.googleusercontent.com", "YDq0zPizR0rJANUBlgbzlb_4");
	}
}
