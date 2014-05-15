package com.gmail.at.zhuikov.aleksandr.driveddoc.repository;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Singleton;

import com.gmail.at.zhuikov.aleksandr.driveddoc.model.SignatureContainerDescription;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreInputStream;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

@Singleton
public class SignatureContainerDescriptionRepository {
	
	private static final String KIND = "OCSPKeyRepository";
	
	public void store(SignatureContainerDescription description) {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	    Entity entity = new Entity(KIND, description.getUserId());
	    entity.setProperty("key", description.getKey().getKeyString());
	    entity.setProperty("password", description.getPassword());
	    datastore.put(entity);
	}
	
	public SignatureContainerDescription get(String userId) {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Key key = KeyFactory.createKey(KIND, userId);
		try {
			Entity entity = datastore.get(key);
			return new SignatureContainerDescription(
					new BlobKey((String) entity.getProperty("key")), 
					(String) entity.getProperty("password"), 
					userId);
		} catch (EntityNotFoundException e) {
			return null;
		}
	}
	
	public InputStream getContent(SignatureContainerDescription description) throws IOException {
		return new BlobstoreInputStream(description.getKey());
	}

	public void delete(String userId) {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Key key = KeyFactory.createKey(KIND, userId);
		datastore.delete(key);		
	}
}
