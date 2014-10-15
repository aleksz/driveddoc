package com.gmail.at.zhuikov.aleksandr.digidocserviceadapter;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;

public class DigiDocServiceAccessor {
	
	private final SignatureContainerDescriptionRepository signatureContainerDescriptionRepository = new SignatureContainerDescriptionRepository();
	private static final String SERVICE_URL = "https://digidocservice.sk.ee";
	
	public String pullUrl(InputStream msg, long length) {

		HttpClient httpclient = getHttpClient();
		
		HttpPost request = new HttpPost(SERVICE_URL);
		request.setHeader("Content-Type", "text/xml; charset=utf-8");
		request.setHeader("User-Agent",  "JDigiDoc /3.8.0.3 ");
		request.setHeader("SOAPAction", "");
		
		try {
			request.setEntity(new InputStreamEntity(msg, length));
			return new String(IOUtils.toByteArray(httpclient.execute(request).getEntity().getContent()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private HttpClient getHttpClient() {
		SignatureContainerDescription description = signatureContainerDescriptionRepository.get("master");
		
		HttpClient client = new DefaultHttpClient();
		KeyStore keyStore = loadKeyStore(description);
		
		try {
			Scheme scheme = new Scheme("https",	new SSLSocketFactory(keyStore, description.getPassword()), 443);
			client.getConnectionManager().getSchemeRegistry().register(scheme);
		} catch (KeyManagementException | UnrecoverableKeyException
				| NoSuchAlgorithmException | KeyStoreException e) {
			throw new RuntimeException(e);
		}
	    return client;
	}

	protected KeyStore loadKeyStore(SignatureContainerDescription description) {
		
		try {
			KeyStore trustStore = KeyStore.getInstance("pkcs12");
			
			try(InputStream instream = signatureContainerDescriptionRepository.getContent(description)) {
				trustStore.load(instream, description.getPassword().toCharArray());
			} 
			return trustStore;
		} catch (CertificateException|NoSuchAlgorithmException|KeyStoreException|IOException e) {
			throw new RuntimeException(e);
		}
	}
}
