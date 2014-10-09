package com.gmail.at.zhuikov.aleksandr.driveddoc.service;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import com.gmail.at.zhuikov.aleksandr.driveddoc.model.SignatureContainerDescription;
import com.gmail.at.zhuikov.aleksandr.driveddoc.repository.SignatureContainerDescriptionRepository;

import ee.sk.digidoc.Base64Util;
import ee.sk.digidoc.DataFile;
import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.Signature;
import ee.sk.digidoc.SignedDoc;
import ee.sk.utils.ConfigManager;
import ee.sk.utils.ConvertUtils;


@Singleton
public class MobileIdService {
	
	private static final Logger LOG = Logger.getLogger(DigiDocService.class.getName());

	public static final String STAT_OUTSTANDING_TRANSACTION = "OUTSTANDING_TRANSACTION";
	public static final String STAT_SIGNATURE = "SIGNATURE";
	public static final String STAT_ERROR = "ERROR";
	
	private static final String g_xmlHdr1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:d=\"http://www.sk.ee/DigiDocService/DigiDocService_2_3.wsdl\" xmlns:mss=\"http://www.sk.ee:8096/MSSP_GW/MSSP_GW.wsdl\"><SOAP-ENV:Body SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><d:MobileCreateSignature>";
	private static final String g_xmlEnd1 = "</d:MobileCreateSignature></SOAP-ENV:Body></SOAP-ENV:Envelope>";
	private static final String g_xmlHdr2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:d=\"http://www.sk.ee/DigiDocService/DigiDocService_2_3.wsdl\" xmlns:mss=\"http://www.sk.ee:8096/MSSP_GW/MSSP_GW.wsdl\"><SOAP-ENV:Body SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><d:GetMobileCreateSignatureStatus>";
	private static final String g_xmlEnd2 = "</d:GetMobileCreateSignatureStatus></SOAP-ENV:Body></SOAP-ENV:Envelope>";
	
	private final SignatureContainerDescriptionRepository signatureContainerDescriptionRepository;
	
	@Inject
	public MobileIdService(SignatureContainerDescriptionRepository signatureContainerDescriptionRepository) {
		this.signatureContainerDescriptionRepository = signatureContainerDescriptionRepository;
	}
	
	private static String findElemValue(String msg, String tag) {
		int nIdx1 = 0, nIdx2 = 0;
		if (msg != null && tag != null) {
			nIdx1 = msg.indexOf("<" + tag);
			if (nIdx1 != -1) {
				while (msg.charAt(nIdx1) != '>')
					nIdx1++;
				nIdx1++;
				nIdx2 = msg.indexOf("</" + tag, nIdx1);
				if (nIdx1 > 0 && nIdx2 > 0)
					return msg.substring(nIdx1, nIdx2);
			}
		}
		return null;
	}

	private static void addElem(StringBuffer xml, String tag, String value) {
		if (value != null && value.trim().length() > 0) {
			xml.append("<");
			xml.append(tag);
			xml.append(">");
			xml.append(value);
			xml.append("</");
			xml.append(tag);
			xml.append(">");
		}
	}

	private static String findAttrValue(String msg, String attr) {
		int nIdx1 = 0, nIdx2 = 0;
		if (msg != null && attr != null) {
			nIdx1 = msg.indexOf(attr);
			if (nIdx1 != -1) {
				while (msg.charAt(nIdx1) != '=')
					nIdx1++;
				nIdx1++;
				if (msg.charAt(nIdx1) == '\"')
					nIdx1++;
				nIdx2 = msg.indexOf("\"", nIdx1);
				if (nIdx1 > 0 && nIdx2 > 0)
					return msg.substring(nIdx1, nIdx2);
			}
		}
		return null;
	}
	
	public String startSigningSession(SignedDoc sdoc, 
            String sIdCode, String sPhoneNo,
            String sLang, String sServiceName,
            String sManifest, String sCity, 
            String sState, String sZip, 
            String sCountry, StringBuffer sbChallenge) throws DigiDocException {
		String sSessCode = null;
		
		if(sdoc == null)
	    	throw new DigiDocException(DigiDocException.ERR_DIGIDOC_SERVICE, "Missing SignedDoc object", null);
	    if(sIdCode == null || sIdCode.trim().length() < 11)
	    	throw new DigiDocException(DigiDocException.ERR_DIGIDOC_SERVICE, "Missing or invalid personal id-code", null);
	    if(sPhoneNo == null || sPhoneNo.trim().length() < 5) // min 5 kohaline mobiili nr ?
	    	throw new DigiDocException(DigiDocException.ERR_DIGIDOC_SERVICE, "Missing or invalid phone number", null);
	    if(sCountry == null || sCountry.trim().length() < 2)
	    	throw new DigiDocException(DigiDocException.ERR_DIGIDOC_SERVICE, "Missing or invalid country code", null);
	    ConfigManager cfg = ConfigManager.instance();
		String sUrl = cfg.getProperty("DDS_URL");
		//String sProxyHost = cfg.getProperty("DIGIDOC_PROXY_HOST");
		//String sProxyPort = cfg.getProperty("DIGIDOC_PROXY_PORT");
		// compose soap msg
		StringBuffer sbMsg = new StringBuffer(g_xmlHdr1);
		addElem(sbMsg, "IDCode", sIdCode);
		addElem(sbMsg, "SignersCountry", sCountry);
		addElem(sbMsg, "PhoneNo", sPhoneNo);
		addElem(sbMsg, "Language", sLang);
		addElem(sbMsg, "ServiceName", sServiceName);
		addElem(sbMsg, "Role", sManifest);
		addElem(sbMsg, "City", sCity);
		addElem(sbMsg, "StateOrProvince", sState);
		addElem(sbMsg, "PostalCode", sZip);
		addElem(sbMsg, "CountryName", sCountry);
	    sbMsg.append("<DataFiles>");
	    for(int i = 0; i < sdoc.countDataFiles(); i++) {
	        DataFile df = sdoc.getDataFile(i);
	        sbMsg.append("<DataFileDigest>");
	        addElem(sbMsg, "Id", df.getId());
	        addElem(sbMsg, "DigestType", "sha1");
	        String sHash = Base64Util.encode(df.getDigest());
	        addElem(sbMsg, "DigestValue", sHash);
	        sbMsg.append("</DataFileDigest>");
	    }
	    sbMsg.append("</DataFiles>");
	    addElem(sbMsg, "Format", sdoc.getFormat());
	    addElem(sbMsg, "Version", sdoc.getVersion());
	    String sId = sdoc.getNewSignatureId();
	    addElem(sbMsg, "SignatureID", sId);
	    addElem(sbMsg, "MessagingMode", "asynchClientServer");
	    addElem(sbMsg, "AsyncConfiguration", "0");
		sbMsg.append(g_xmlEnd1);
		// send soap message
		LOG.fine("Sending:\n---\n" + sbMsg.toString() + "\n---\n");
		String sResp = pullUrl(sUrl, sbMsg.toString());
		LOG.fine("Received:\n---\n" + sResp + "\n---\n");
		if(sResp != null && sResp.trim().length() > 0) {
			sSessCode = findElemValue(sResp, "Sesscode");
			String s = findElemValue(sResp, "ChallengeID");
			if(s != null)
				sbChallenge.append(s);
		}
		return sSessCode;
	}
	
	public String getStatus(SignedDoc sdoc, String sSesscode)
			throws DigiDocException {
		String sStatus = null;

		if (sdoc == null)
			throw new DigiDocException(DigiDocException.ERR_DIGIDOC_SERVICE,
					"Missing SignedDoc object", null);
		if (sSesscode == null || sSesscode.trim().length() == 0)
			throw new DigiDocException(DigiDocException.ERR_DIGIDOC_SERVICE,
					"Missing or invalid  session code", null);
		ConfigManager cfg = ConfigManager.instance();
		String sUrl = cfg.getProperty("DDS_URL");
		// compose soap msg
		StringBuffer sbMsg = new StringBuffer(g_xmlHdr2);
		addElem(sbMsg, "Sesscode", sSesscode);
		addElem(sbMsg, "WaitSignature", "false");
		sbMsg.append(g_xmlEnd2);
		// send soap message
		LOG.fine("Sending:\n---\n" + sbMsg.toString() + "\n---\n");
		String sResp = pullUrl(sUrl, sbMsg.toString());
		LOG.fine("Received:\n---\n" + sResp + "\n---\n");
		if (sResp != null && sResp.trim().length() > 0) {
			sStatus = findElemValue(sResp, "Status");
			if (sStatus != null && sStatus.equals(STAT_SIGNATURE)) {
				String s = findElemValue(sResp, "Signature");
				if (s != null) {
					String sSig = ConvertUtils.unescapeXmlSymbols(s);
					String sId = findAttrValue(sSig, "Id");
					LOG.fine("Signature: " + sId + "\n---\n" + sSig + "\n---\n");
					Signature sig = new Signature(sdoc);
					sig.setId(sId);
					try {
						sig.setOrigContent(sSig.getBytes("UTF-8"));
					} catch (Exception ex) {
						LOG.warning("Error adding signature: " + ex);
						DigiDocException.handleException(ex,
								DigiDocException.ERR_DIGIDOC_SERVICE);
					}
					sdoc.addSignature(sig);
				}
			}
		}
		return sStatus;
	}
	
	private String pullUrl(String url, String msg) {

		HttpClient httpclient = getHttpClient();
		
		HttpPost request = new HttpPost(url);
		request.setHeader("Content-Type", "text/xml; charset=utf-8");
		request.setHeader("User-Agent", SignedDoc.LIB_NAME + " / " + SignedDoc.LIB_VERSION);
		request.setHeader("SOAPAction", "");
		
		try {
			request.setEntity(new StringEntity(msg, "UTF-8"));
			return new String(IOUtils.toByteArray(httpclient.execute(request).getEntity().getContent()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected HttpClient getHttpClient() {
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
