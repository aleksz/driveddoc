package com.gmail.at.zhuikov.aleksandr.driveddoc.service;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gmail.at.zhuikov.aleksandr.driveddoc.model.User;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.appengine.api.memcache.AsyncMemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

@Singleton
public class CachedUserService extends UserService {

	private AsyncMemcacheService cache = MemcacheServiceFactory.getAsyncMemcacheService();
	
	@Inject
	public CachedUserService(HttpTransport transport, JsonFactory jsonFactory) {
		super(transport, jsonFactory);
	}

	@Override
	public User getUserInfo(String userId, Credential credential) throws IOException {
	
		try {
			if (cache.contains(userId).get()) {
				return (User) cache.get(userId).get();
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
		
		User  info =  super.getUserInfo(userId, credential);
		
		cache.put(userId, info);
		
		return info;
	}
}
