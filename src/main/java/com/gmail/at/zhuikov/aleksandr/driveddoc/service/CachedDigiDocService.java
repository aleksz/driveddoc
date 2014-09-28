package com.gmail.at.zhuikov.aleksandr.driveddoc.service;

import static java.util.logging.Level.WARNING;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.util.logging.Logger;

import javax.inject.Singleton;

import com.gmail.at.zhuikov.aleksandr.driveddoc.model.ValidatedSignedDoc;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsInputChannel;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.appengine.tools.cloudstorage.RetryHelperException;
import com.google.appengine.tools.cloudstorage.RetryParams;

@Singleton
public class CachedDigiDocService extends DigiDocService {
	
	private static final String BUCKET = "driveddoc.appspot.com";
	private static final Logger LOG = Logger.getLogger(CachedDigiDocService.class.getName());

	private final GcsService gcsService =  GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());
	
	@Override
	public ValidatedSignedDoc parseSignedDoc(String fileName, String id, InputStream content) throws IOException  {
		
		String cacheKey = URLEncoder.encode(id.replaceAll("\"", ""), "UTF-8");
		
		GcsFilename gcsFileName = new GcsFilename(BUCKET, cacheKey);

		if (gcsService.getMetadata(gcsFileName) != null) {
			LOG.fine("Cache hit for " + cacheKey);
			return readFromCache(gcsFileName);
		}
		
		LOG.fine("Cache miss for " + cacheKey);
		
		ValidatedSignedDoc signedDoc = super.parseSignedDoc(fileName, id, content);
		cache(gcsFileName, signedDoc);
		
		return signedDoc;
	}

	private void cache(final GcsFilename gcsFileName, final ValidatedSignedDoc signedDoc) 	throws IOException {
		
		try {
			GcsOutputChannel outputChannel = gcsService.createOrReplace(gcsFileName, GcsFileOptions.getDefaultInstance());
			try (ObjectOutputStream oout =   new ObjectOutputStream(Channels.newOutputStream(outputChannel))) {
				oout.writeObject(signedDoc);
			}
			LOG.fine("Cached " + gcsFileName.getObjectName());
		} catch (IOException|RetryHelperException e) {
			LOG.log(WARNING, "Failed to cache " + gcsFileName.getObjectName(), e);
		}
	}

	private ValidatedSignedDoc readFromCache(GcsFilename gcsFileName) throws IOException {
		GcsInputChannel readChannel = gcsService.openPrefetchingReadChannel(gcsFileName, 0, 1024 * 1024);
		try (ObjectInputStream oin = new ObjectInputStream(Channels.newInputStream(readChannel))) {
			return (ValidatedSignedDoc) oin.readObject();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
