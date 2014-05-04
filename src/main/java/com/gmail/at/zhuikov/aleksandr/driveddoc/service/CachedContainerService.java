package com.gmail.at.zhuikov.aleksandr.driveddoc.service;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gmail.at.zhuikov.aleksandr.driveddoc.model.container.ClientContainer;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.drive.model.File;
import com.google.appengine.api.memcache.AsyncMemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

@Singleton
public class CachedContainerService extends ContainerService {

	private AsyncMemcacheService cache = MemcacheServiceFactory.getAsyncMemcacheService();
	
	@Inject
	public CachedContainerService(GDriveService gDriveService,
			CachedDigiDocService digiDocService) {
		super(gDriveService, digiDocService);
	}

	@Override
	protected ClientContainer getContainer(Credential credential, File file)
			throws IOException {
		
		try {
			if (cache.contains(file.getEtag()).get()) {
				return (ClientContainer) cache.get(file.getEtag()).get();
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
		
		ClientContainer container = super.getContainer(credential, file);
		cache.put(file.getEtag(), container);
		return container;
	}
}
