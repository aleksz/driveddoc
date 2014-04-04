package com.gmail.at.zhuikov.aleksandr.driveddoc.service;

import java.util.Arrays;
import java.util.List;

import javax.inject.Singleton;

import com.google.api.client.extensions.appengine.auth.oauth2.AppEngineCredentialStore;

@Singleton
public class CredentialManager extends AppEngineCredentialStore {

	public static final List<String> SCOPES = Arrays.asList(
			// Required to access and manipulate files.
			"https://www.googleapis.com/auth/drive.file",
			// Required to identify the user in our data store.
			"https://www.googleapis.com/auth/userinfo.email",
			"https://www.googleapis.com/auth/userinfo.profile",
			"https://www.googleapis.com/auth/drive.install"
	// "https://www.googleapis.com/auth/drive.appdata",
	// "https://www.googleapis.com/auth/drive.apps.readonly"
			);
}
