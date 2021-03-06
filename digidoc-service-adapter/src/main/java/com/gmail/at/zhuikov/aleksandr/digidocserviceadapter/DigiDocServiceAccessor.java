package com.gmail.at.zhuikov.aleksandr.digidocserviceadapter;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.logging.Logger;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.SyncBasicHttpContext;

public class DigiDocServiceAccessor {
	
	private final SignatureContainerDescriptionRepository signatureContainerDescriptionRepository = new SignatureContainerDescriptionRepository();
	private static final String SERVICE_URL = "https://digidocservice.sk.ee";
	private static final Logger LOG = Logger.getLogger(DigiDocServiceAccessor.class.getName());
	private final HttpClient httpClient;
	private final HttpContext httpContext;
	
	public DigiDocServiceAccessor() {
		httpClient = getHttpClient();
		httpContext = new SyncBasicHttpContext(null);
	}
	
	public String pullUrl(InputStream msg, long length) {

		HttpPost request = new HttpPost(SERVICE_URL);
		request.setHeader("Content-Type", "text/xml; charset=utf-8");
		request.setHeader("User-Agent",  "JDigiDoc /3.8.0.3 ");
		request.setHeader("SOAPAction", "");
		
		try {
			String msgString = new String(IOUtils.toByteArray(msg));
			LOG.info(msgString);
			request.setEntity(new StringEntity(msgString));
			String result = new String(IOUtils.toByteArray(httpClient.execute(request, httpContext).getEntity().getContent()));
			LOG.info(result);
			return result;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private HttpClient getHttpClient() {
		SignatureContainerDescription description = signatureContainerDescriptionRepository.get("master");
		
		try {
			KeyStore keyStore = loadKeyStore(description);
			HttpParams params = new BasicHttpParams();
			SchemeRegistry schemeRegistry = new SchemeRegistry();
			schemeRegistry.register(new Scheme("https",	new SSLSocketFactory(keyStore, description.getPassword()), 443));
			return new DefaultHttpClient(new ThreadSafeClientConnManager(params, schemeRegistry), params);
		} catch (KeyManagementException | UnrecoverableKeyException
				| NoSuchAlgorithmException | KeyStoreException e) {
			throw new RuntimeException(e);
		}
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
