package com.gmail.at.zhuikov.aleksandr.jdigidoc;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.ocsp.ResponderID;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.ocsp.BasicOCSPResp;
import org.bouncycastle.ocsp.CertificateID;
import org.bouncycastle.ocsp.OCSPReq;
import org.bouncycastle.ocsp.OCSPReqGenerator;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.ocsp.OCSPRespStatus;
import org.bouncycastle.ocsp.RevokedStatus;
import org.bouncycastle.ocsp.SingleResp;
import org.bouncycastle.ocsp.UnknownStatus;

import com.gmail.at.zhuikov.aleksandr.driveddoc.model.DigidocOCSPSignatureContainer;

import ee.sk.digidoc.Base64Util;
import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.Notary;
import ee.sk.digidoc.Signature;
import ee.sk.digidoc.SignedDoc;
import ee.sk.digidoc.factory.BouncyCastleNotaryFactory;
import ee.sk.digidoc.factory.CRLFactory;
import ee.sk.digidoc.factory.DigiDocFactory;
import ee.sk.digidoc.factory.NotaryFactory;
import ee.sk.digidoc.factory.TrustServiceFactory;
import ee.sk.utils.ConfigManager;
import ee.sk.utils.ConvertUtils;

public class FlexibleBouncyCastleNotaryFactory implements NotaryFactory {

	/** NONCE extendion oid */
    public static final String nonceOid = "1.3.6.1.5.5.7.48.1.2";
    private boolean m_bSignRequests;
    private Logger m_logger = Logger.getLogger(BouncyCastleNotaryFactory.class);
    
    public static ThreadLocal<DigidocOCSPSignatureContainer> ocspSignCert = new ThreadLocal<>();
    
    /**
     * Returns the OCSP responders certificate
     * @param responderCN responder-id's CN
     * @param specificCertNr specific cert number that we search.
     * If this parameter is null then the newest cert is seleced (if many exist)
     * @returns OCSP responders certificate
     */
    public X509Certificate getNotaryCert(String responderCN, String specificCertNr)
    	
    {
    	try {
    	  TrustServiceFactory tslFac = ConfigManager.instance().getTslFactory();
          boolean bUseLocal = ConfigManager.instance().getBooleanProperty("DIGIDOC_USE_LOCAL_TSL", false);
    	  return tslFac.findOcspByCN(responderCN, bUseLocal);
    	} catch(Exception ex) {
    		m_logger.error("Error searching responder cert for: " + responderCN + " - " + ex);
    	}
    	return null;
    }
    
    /**
     * Returns the OCSP responders certificate
     * @param responderCN responder-id's CN
     * @param specificCertNr specific cert number that we search.
     * If this parameter is null then the newest cert is seleced (if many exist)
     * @returns OCSP responders certificate
     */
    public X509Certificate[] getNotaryCerts(String responderCN, String specificCertNr)
    	
    {
    	try {
    	  TrustServiceFactory tslFac = ConfigManager.instance().getTslFactory();
          boolean bUseLocal = ConfigManager.instance().getBooleanProperty("DIGIDOC_USE_LOCAL_TSL", false);
    	  return tslFac.findOcspsByCNAndNr(responderCN, bUseLocal, specificCertNr);
    	} catch(Exception ex) {
    		m_logger.error("Error searching responder cert for: " + responderCN + " - " + ex);
    	}
    	return null;
    }
    
    /**
     * Returns the OCSP responders CA certificate
     * @param responderCN responder-id's CN
     * @returns OCSP responders CA certificate
     */
    public X509Certificate getCACert(String responderCN)
    {
    	try {
      	  TrustServiceFactory tslFac = ConfigManager.instance().getTslFactory();
            boolean bUseLocal = ConfigManager.instance().getBooleanProperty("DIGIDOC_USE_LOCAL_TSL", false);
      	  X509Certificate cert = tslFac.findOcspByCN(responderCN, bUseLocal);
      	  if(cert != null)
      	    return tslFac.findCaForCert(cert, bUseLocal);
      	} catch(Exception ex) {
      		m_logger.error("Error searching responder ca cert for: " + responderCN + " - " + ex);
      	}
      	return null;
    }

    
	/**
     * Get confirmation from AS Sertifitseerimiskeskus
     * by creating an OCSP request and parsing the returned
     * OCSP response
     * @param nonce signature nonce
     * @param signersCert signature owners cert
     * @param notId new id for Notary object
     * @param httpFrom HTTP_FROM header value (optional)
     * @returns Notary object
     */
    public Notary getConfirmation(byte[] nonce, 
        X509Certificate signersCert, String notId, String httpFrom) 
        throws DigiDocException
    {        
    	boolean bUseLocal = ConfigManager.instance().getBooleanProperty("DIGIDOC_USE_LOCAL_TSL", false);
        TrustServiceFactory tslFac = ConfigManager.instance().getTslFactory();
        X509Certificate caCert = tslFac.findCaForCert(signersCert, bUseLocal);
        X509Certificate ocspCert = tslFac.findOcspByCN(ConvertUtils.getCommonName(ConvertUtils.convX509Name(signersCert.getIssuerX500Principal())), bUseLocal);
        return getConfirmation(nonce, signersCert, caCert, ocspCert, notId, httpFrom);
    }
    
    /**
     * Get confirmation from AS Sertifitseerimiskeskus
     * by creating an OCSP request and parsing the returned
     * OCSP response
     * @param nonce signature nonce
     * @param signersCert signature owners cert
     * @param caCert CA cert for this signer
     * @param notaryCert notarys own cert
     * @param notId new id for Notary object
     * @param httpFrom HTTP_FROM header value (optional)
     * @returns Notary object
     */
    public Notary getConfirmation(byte[] nonce, 
        X509Certificate signersCert, X509Certificate caCert,
        X509Certificate notaryCert, String notId, String ocspUrl, 
        String httpFrom, String format, String formatVer) 
        throws DigiDocException 
    {
        Notary not = null;
        try {        
        	if(m_logger.isDebugEnabled())
                m_logger.debug("getConfirmation, nonce " + Base64Util.encode(nonce, 0) +
                " cert: " + ((signersCert != null) ? signersCert.getSerialNumber().toString() : "NULL") + 
                " CA: " + ((caCert != null) ? caCert.getSerialNumber().toString() : "NULL") +
                " responder: " + ((notaryCert != null) ? notaryCert.getSerialNumber().toString() : "NULL") +
                " notId: " + notId + " signRequest: " + m_bSignRequests +
                " url: " + ocspUrl);
            if(m_logger.isDebugEnabled()) {
            	m_logger.debug("Check cert: " + ((signersCert != null) ? signersCert.getSubjectDN().getName() : "NULL"));            	
            	m_logger.debug("Check CA cert: " + ((caCert != null) ? caCert.getSubjectDN().getName() : "NULL"));
        	}
            // create the request - sign the request if necessary
            OCSPReq req = createOCSPRequest(nonce, signersCert, caCert, m_bSignRequests);
            //debugWriteFile("req.der", req.getEncoded());
            if(m_logger.isDebugEnabled())
                m_logger.debug("REQUEST:\n" + Base64Util.encode(req.getEncoded(), 0));
            // send it
            OCSPResp resp = sendRequestToUrl(req, ocspUrl, httpFrom, format, formatVer);
            //debugWriteFile("resp.der", resp.getEncoded());
            if(m_logger.isDebugEnabled())
                m_logger.debug("RESPONSE:\n" + Base64Util.encode(resp.getEncoded(), 0));
            // check response status
            verifyRespStatus(resp);
            // check the result
            not = parseAndVerifyResponse(null, notId, signersCert, resp, nonce, notaryCert, caCert);
            if(m_logger.isDebugEnabled())
                m_logger.debug("Confirmation OK!");
        } catch(DigiDocException ex) {
            throw ex;
        } catch(Exception ex) {
            DigiDocException.handleException(ex, DigiDocException.ERR_OCSP_GET_CONF);        
        }
        return not;
    }

    
    /**
     * Get confirmation from AS Sertifitseerimiskeskus
     * by creating an OCSP request and parsing the returned
     * OCSP response
     * @param nonce signature nonce
     * @param signersCert signature owners cert
     * @param caCert CA cert for this signer
     * @param notaryCert notarys own cert
     * @param notId new id for Notary object
     * @returns Notary object
     */
    public Notary getConfirmation(byte[] nonce, 
        X509Certificate signersCert, X509Certificate caCert,
        X509Certificate notaryCert, String notId, String httpFrom) // TODO: remove param notaryCert
        throws DigiDocException 
    {
        return getConfirmation(nonce, 
                signersCert, caCert,
                notaryCert, notId, ConfigManager.instance().
            	getProperty("DIGIDOC_OCSP_RESPONDER_URL"), httpFrom, null, null);
    }


    
    /**
     * Get confirmation from AS Sertifitseerimiskeskus
     * by creating an OCSP request and parsing the returned
     * OCSP response
     * @param sig Signature object. 
     * @param signersCert signature owners cert
     * @param caCert CA cert for this signer
     * @returns Notary object
     */
    public Notary getConfirmation(Signature sig, 
        X509Certificate signersCert, X509Certificate caCert) 
        throws DigiDocException 
    {
    	
        Notary not = null;
        try {
        	String notId = sig.getId().replace('S', 'N');
            // calculate the nonce
        	// test if it works with sha256
            byte[] nonce = SignedDoc.digestOfType(sig.getSignatureValue().getValue(),
            		sig.getSignedDoc().getFormat().equals(SignedDoc.FORMAT_BDOC) ? SignedDoc.SHA256_DIGEST_TYPE : SignedDoc.SHA1_DIGEST_TYPE);
            X509Certificate notaryCert = null;
            if(sig.getUnsignedProperties() != null)
            	notaryCert = sig.getUnsignedProperties().getRespondersCertificate();
            // check the result
            // TODO: select correct ocsp url
            not = getConfirmation(nonce, signersCert, caCert, notaryCert, notId, 
            		ConfigManager.instance().getProperty("DIGIDOC_OCSP_RESPONDER_URL"), 
            		sig.getHttpFrom(), sig.getSignedDoc().getFormat(), sig.getSignedDoc().getVersion());
            // add cert to signature
            if(notaryCert == null && sig != null && sig.getUnsignedProperties() != null) {
            	OCSPResp resp = new OCSPResp(not.getOcspResponseData()); 
            	if(resp != null && resp.getResponseObject() != null) {
            	String respId = responderIDtoString((BasicOCSPResp)resp.getResponseObject());
            	TrustServiceFactory tslFac = ConfigManager.instance().getTslFactory();
            	notaryCert = tslFac.findOcspByCN(SignedDoc.getCommonName(respId), true); // must use local store here since ocsp certs are not in tsl
            	if(notaryCert != null && !sig.getSignedDoc().getFormat().equals(SignedDoc.FORMAT_XADES))
            	  sig.getUnsignedProperties().setRespondersCertificate(notaryCert);
            	  ee.sk.digidoc.CertID cid = new ee.sk.digidoc.CertID(sig, notaryCert, ee.sk.digidoc.CertID.CERTID_TYPE_RESPONDER);
                  sig.addCertID(cid);
                  cid.setUri("#" + sig.getId() + "-RESPONDER_CERT");
            	}
            }
        } catch(DigiDocException ex) {
            throw ex;
        } catch(Exception ex) {
            DigiDocException.handleException(ex, DigiDocException.ERR_OCSP_GET_CONF);        
        }
        return not;
    }
    
    /**
     * Get confirmation from AS Sertifitseerimiskeskus
     * by creating an OCSP request and parsing the returned
     * OCSP response
     * @param sig Signature object. 
     * @param signersCert signature owners cert
     * @param caCert CA cert for this signer
     * @param notaryCert OCSP responders cert
     * @param ocspUrl OCSP responders url
     * @returns Notary object
     */
    public Notary getConfirmation(Signature sig, 
        X509Certificate signersCert, X509Certificate caCert, 
        X509Certificate notaryCert, String ocspUrl) 
        throws DigiDocException 
    {
    	
        Notary not = null;
        try {
        	String notId = sig.getId().replace('S', 'N');
            // calculate the nonce
        	// TODO: sha256?
            //byte[] nonce = SignedDoc.digest(sig.getSignatureValue().getValue());
        	byte[] nonce = SignedDoc.digestOfType(sig.getSignatureValue().getValue(),
            		sig.getSignedDoc().getFormat().equals(SignedDoc.FORMAT_BDOC) ? SignedDoc.SHA256_DIGEST_TYPE : SignedDoc.SHA1_DIGEST_TYPE);
            if(notaryCert == null && sig.getUnsignedProperties() != null)
            	notaryCert = sig.getUnsignedProperties().getRespondersCertificate();
            // check the result
            not = getConfirmation(nonce, signersCert, caCert, notaryCert, notId, ocspUrl, 
            		sig.getHttpFrom(), sig.getSignedDoc().getFormat(), sig.getSignedDoc().getVersion());
            if(sig != null && not != null && sig.getUnsignedProperties() != null)
            	sig.getUnsignedProperties().setNotary(not);
            // add cert to signature
            if(notaryCert == null && sig != null && sig.getUnsignedProperties() != null && sig.getUnsignedProperties().getNotary() != null) {
            	OCSPResp resp = new OCSPResp(sig.getUnsignedProperties().getNotary().getOcspResponseData()); 
            	if(resp != null && resp.getResponseObject() != null && notaryCert == null) {
            	  String respId = responderIDtoString((BasicOCSPResp)resp.getResponseObject());
            	  boolean bUseLocal = ConfigManager.instance().getBooleanProperty("DIGIDOC_USE_LOCAL_TSL", false);
                  TrustServiceFactory tslFac = ConfigManager.instance().getTslFactory();
                  notaryCert = tslFac.findOcspByCN(ConvertUtils.getCommonName(respId), bUseLocal);
                  if(notaryCert != null) {
            	  sig.getUnsignedProperties().setRespondersCertificate(notaryCert);
            	  ee.sk.digidoc.CertID cid = new ee.sk.digidoc.CertID(sig, notaryCert, ee.sk.digidoc.CertID.CERTID_TYPE_RESPONDER);
                  sig.addCertID(cid);
                  cid.setUri("#" + sig.getId() + "-RESPONDER_CERT");
                  }
            	}
            }
        } catch(DigiDocException ex) {
            throw ex;
        } catch(Exception ex) {
            DigiDocException.handleException(ex, DigiDocException.ERR_OCSP_GET_CONF);        
        }
        return not;
    }

    /**
     * Get confirmation from AS Sertifitseerimiskeskus
     * by creating an OCSP request and parsing the returned
     * OCSP response. CA and reponders certs are read 
     * using paths in the config file or maybe from
     * a keystore etc.
     * @param sig Signature object
     * @param signersCert signature owners cert
     * @returns Notary object
     */
    public Notary getConfirmation(Signature sig, X509Certificate signersCert) 
        throws DigiDocException 
    {
    	String notId = sig.getId().replace('S', 'N');
    	//byte[] nonce = SignedDoc.digest(sig.getSignatureValue().getValue()); // sha256?
    	byte[] nonce = SignedDoc.digestOfType(sig.getSignatureValue().getValue(),
        		sig.getSignedDoc().getFormat().equals(SignedDoc.FORMAT_BDOC) ? SignedDoc.SHA256_DIGEST_TYPE : SignedDoc.SHA1_DIGEST_TYPE);
    	boolean bUseLocal = ConfigManager.instance().getBooleanProperty("DIGIDOC_USE_LOCAL_TSL", false);
        TrustServiceFactory tslFac = ConfigManager.instance().getTslFactory();
        X509Certificate caCert = tslFac.findCaForCert(signersCert, bUseLocal);
        X509Certificate ocspCert = tslFac.findOcspByCN(ConvertUtils.getCommonName(ConvertUtils.convX509Name(signersCert.getIssuerX500Principal())), bUseLocal);
        return getConfirmation(nonce, signersCert, caCert, ocspCert, notId, sig.getHttpFrom());
    }
    
    /*private String ocspFileName(X509Certificate cert)
    {
    	StringBuffer sb = new StringBuffer(cert.getSerialNumber().toString());
    	sb.append("_");
    	Date dtNow = new Date();
    	SimpleDateFormat df = new SimpleDateFormat("HHmmss");
    	sb.append(df.format(dtNow));    	
    	return sb.toString();	
    }*/
    
    
    private String composeHttpFrom()
	{
		// set HTTP_FROM to some value
		String sFrom = null;
		try {
			NetworkInterface ni = null;
			Enumeration eNi = NetworkInterface.getNetworkInterfaces();
			if(eNi != null && eNi.hasMoreElements())
				ni = (NetworkInterface)eNi.nextElement();
			if(ni != null) {
				InetAddress ia = null;
				Enumeration eA = ni.getInetAddresses();
				if(eA != null && eA.hasMoreElements())
					ia = (InetAddress)eA.nextElement();
				if(ia != null)
					sFrom = ia.getHostAddress();
				System.err.println("FROM: " + sFrom);
			}
		} catch(Exception ex2) {
			System.err.println("Error finding ip-adr: " + ex2);
		}
		return sFrom;
	}
    
    /**
     * Verifies the certificate by creating an OCSP request
     * and sending it to SK server.
     * @param cert certificate to verify
     * @param httpFrom HTTP_FROM optional argument
     * @throws DigiDocException if the certificate is not valid
     * @return ocsp response
     */   
    public OCSPResp checkCertificate(X509Certificate cert) 
        throws DigiDocException
    {
    	return checkCertificate(cert, composeHttpFrom());
    }
    
    /**
     * Verifies the certificate by creating an OCSP request
     * and sending it to SK server.
     * @param cert certificate to verify
     * @param httpFrom HTTP_FROM optional argument
     * @throws DigiDocException if the certificate is not valid
     * @return ocsp response
     */   
    public OCSPResp checkCertificate(X509Certificate cert, String httpFrom) 
        throws DigiDocException 
    {
    	OCSPResp resp = null;
        try {
        	String verifier = ConfigManager.instance().
                getStringProperty("DIGIDOC_CERT_VERIFIER", "OCSP");
            if(verifier != null && verifier.equals("OCSP")) {
        	// create the request
            DigiDocFactory ddocFac = ConfigManager.instance().getDigiDocFactory();
            TrustServiceFactory tslFac = ConfigManager.instance().getTslFactory();
            boolean bUseLocal = ConfigManager.instance().getBooleanProperty("DIGIDOC_USE_LOCAL_TSL", false);
        	X509Certificate caCert = tslFac.findCaForCert(cert, bUseLocal);
        	if(m_logger.isDebugEnabled()) {
        		m_logger.debug("Find CA for: " + SignedDoc.getCommonName(ConvertUtils.convX509Name(cert.getIssuerX500Principal())));
            	m_logger.debug("Check cert: " + cert.getSubjectDN().getName());            	
            	m_logger.debug("Check CA cert: " + caCert.getSubjectDN().getName());
        	}
        	String strTime = new java.util.Date().toString();
            byte[] nonce1 = SignedDoc.digest(strTime.getBytes()); // sha256?
            OCSPReq req = createOCSPRequest(nonce1, cert, caCert, m_bSignRequests);
            //debugWriteFile("req1.der", req.getEncoded());
            if(m_logger.isDebugEnabled()) {
            	m_logger.debug("Sending ocsp request: " + req.getEncoded().length + " bytes");
                m_logger.debug("REQUEST:\n" + Base64Util.encode(req.getEncoded(), 0));
            }    
            // send it
            String ocspUrl = tslFac.findOcspUrlForCert(cert, 0, bUseLocal);
            resp = sendRequestToUrl(req, ocspUrl, httpFrom, null, null);
            //debugWriteFile("resp1.der", resp.getEncoded());
            if(m_logger.isDebugEnabled()) {
                m_logger.debug("Got ocsp response: " + resp.getEncoded().length + " bytes");
                m_logger.debug("RESPONSE:\n" + Base64Util.encode(resp.getEncoded(), 0));
            }
            // check response status
            verifyRespStatus(resp);
            // now read the info from the response
            BasicOCSPResp basResp = 
                (BasicOCSPResp)resp.getResponseObject();
            
            byte[] nonce2 = getNonce(basResp, null);
            if(m_logger.isDebugEnabled()) 
                m_logger.debug("Nonce1: " + ((nonce1 != null) ? ConvertUtils.bin2hex(nonce1) + " len: " + nonce1.length : "NULL") + 
                		" nonce2: " + ((nonce2 != null) ? ConvertUtils.bin2hex(nonce2) + " len: " + nonce2.length : "NULL"));
            if(!SignedDoc.compareDigests(nonce1, nonce2)) 
            	throw new DigiDocException(DigiDocException.ERR_OCSP_UNSUCCESSFULL,
                    "Invalid nonce value! Possible replay attack!", null); 
            // verify the response
            try {
            	String respId = responderIDtoString(basResp);
            	X509Certificate notaryCert = getNotaryCert(ConvertUtils.getCommonName(respId), null);
            	boolean bOk = false;
            	if(notaryCert != null)
            	  bOk = basResp.verify(notaryCert.getPublicKey(), "BC");
            	else
            		throw new DigiDocException(DigiDocException.ERR_OCSP_VERIFY,
                            "Responder cert not found for: " + respId, null);
            	if(!bOk)
            		throw new DigiDocException(DigiDocException.ERR_OCSP_VERIFY,
                            "OCSP verification error!", null);
            } catch (Exception ex) {
                m_logger.error("OCSP Signature verification error!!!", ex); 
                DigiDocException.handleException(ex, DigiDocException.ERR_OCSP_VERIFY);
            } 
            // check the response about this certificate
            checkCertStatus(cert, basResp, caCert);
            } else if(verifier != null && verifier.equals("CRL")) {
            	CRLFactory crlFac = ConfigManager.instance().getCRLFactory();
            	crlFac.checkCertificate(cert, new Date());
            }
        } catch(DigiDocException ex) {
            throw ex;
        } catch(Exception ex) {
            DigiDocException.handleException(ex, DigiDocException.ERR_OCSP_GET_CONF);        
        }
        return resp;
    }

    
    /**
     * Verifies the certificate by creating an OCSP request
     * and sending it to ocsp server.
     * @param cert certificate to verify
     * @param caCert CA certificate
     * @param url OCSP responder url
     * @param bosNonce buffer to return generated nonce
     * @param sbRespId buffer to return responderId field
     * @param bosReq buffer to return ocsp request
     * @param httpFrom http_from atribute
     * @throws DigiDocException if the certificate is not valid
     */   
    public OCSPResp sendCertOcsp(X509Certificate cert, X509Certificate caCert, String url, 
    		ByteArrayOutputStream bosNonce, StringBuffer sbRespId, 
    		ByteArrayOutputStream bosReq, String httpFrom) 
        throws DigiDocException 
    {
        try {
        	OCSPResp resp = null;
        	// create the request
            if(m_logger.isDebugEnabled()) {
        		m_logger.debug("Find CA for: " + SignedDoc.getCommonName(ConvertUtils.convX509Name(cert.getIssuerX500Principal())));
            	m_logger.debug("Check cert: " + cert.getSubjectDN().getName());            	
            	m_logger.debug("Check CA cert: " + caCert.getSubjectDN().getName());
        	}
        	String strTime = new java.util.Date().toString();
            byte[] nonce1 = SignedDoc.digest(strTime.getBytes()); //sha256?
        	//byte[] nonce1 = SignedDoc.digestOfType(strTime.getBytes(),
            //		sig.getSignedDoc().getFormat().equals(SignedDoc.FORMAT_BDOC) ? SignedDoc.SHA256_DIGEST_TYPE : SignedDoc.SHA1_DIGEST_TYPE);
            
            bosNonce.write(nonce1);
            OCSPReq req = createOCSPRequest(nonce1, cert, caCert, false);
            //debugWriteFile("req1.der", req.getEncoded());
            if(m_logger.isDebugEnabled()) {
            	m_logger.debug("Sending ocsp request: " + req.getEncoded().length + " bytes");
                m_logger.debug("REQUEST:\n" + Base64Util.encode(req.getEncoded(), 0));
            }    
            if(req != null && bosReq != null)
            	bosReq.write(req.getEncoded());
            // send it
            resp = sendRequestToUrl(req, url, httpFrom, null, null);
            if(resp != null) {
            BasicOCSPResp basResp = 
                (BasicOCSPResp)resp.getResponseObject();
            String sRespId = responderIDtoString(basResp);
            if(sRespId != null)
            sbRespId.append(sRespId);
            }
            //debugWriteFile("resp1.der", resp.getEncoded());
            if(m_logger.isDebugEnabled()) {
                m_logger.debug("Got ocsp response: " + ((resp != null) ? resp.getEncoded().length : 0) + " bytes");
                if(resp != null)
                m_logger.debug("RESPONSE:\n" + Base64Util.encode(resp.getEncoded(), 0));
            }
            
            return resp;
        } catch(DigiDocException ex) {
            throw ex;
        } catch(Exception ex) {
            DigiDocException.handleException(ex, DigiDocException.ERR_OCSP_GET_CONF);        
        }
        return null;
    }
    
    /**
     * Verifies OCSP response by given responder cert. Checks actual certificate status. 
     * @param resp ocsp response
     * @param cert certificate to check
     * @param ocspCert OCSP responders cert
     * @param nonce1 initial nonce value
     * @return true if verified ok
     * @throws DigiDocException
     */
    public boolean checkCertOcsp(OCSPResp resp, X509Certificate cert, 
    		X509Certificate ocspCert, byte[] nonce1, X509Certificate caCert)
    throws DigiDocException
    {
    	try {
    		// check response status
            verifyRespStatus(resp);
            // now read the info from the response
            BasicOCSPResp basResp = 
                (BasicOCSPResp)resp.getResponseObject();
            byte[] nonce2 = getNonce(basResp, null);
            if(!SignedDoc.compareDigests(nonce1, nonce2)) 
            	throw new DigiDocException(DigiDocException.ERR_OCSP_UNSUCCESSFULL,
                    "Invalid nonce value! Possible replay attack!", null); 
            // verify the response
            boolean bOk = false;
            try {
            	String respId = responderIDtoString(basResp);
            	bOk = basResp.verify(ocspCert.getPublicKey(), "BC");
            } catch (Exception ex) {
                m_logger.error("OCSP Signature verification error!!!", ex); 
                DigiDocException.handleException(ex, DigiDocException.ERR_OCSP_VERIFY);
            } 
            // check the response about this certificate
            checkCertStatusWithCa(cert, basResp, caCert);
            return bOk;
    	} catch(DigiDocException ex) {
            throw ex;
        } catch(Exception ex) {
            DigiDocException.handleException(ex, DigiDocException.ERR_OCSP_GET_CONF);        
        }
        return false;
    }
    
    
    /**
     * Verifies the certificate.
     * @param cert certificate to verify
     * @param bUseOcsp flag: use OCSP to verify cert. If false then use CRL instead
     * @throws DigiDocException if the certificate is not valid
     */   
    public void checkCertificateOcspOrCrl(X509Certificate cert, boolean bUseOcsp) 
        throws DigiDocException 
    {
        try {
        	if(bUseOcsp)  {
        	// create the request
            boolean bUseLocal = ConfigManager.instance().getBooleanProperty("DIGIDOC_USE_LOCAL_TSL", false);
            TrustServiceFactory tslFac = ConfigManager.instance().getTslFactory();
            X509Certificate caCert = tslFac.findCaForCert(cert, bUseLocal);
        	if(m_logger.isDebugEnabled()) {
        		m_logger.debug("Find CA for: " + SignedDoc.getCommonName(ConvertUtils.convX509Name(cert.getIssuerX500Principal())));
            	m_logger.debug("Check cert: " + cert.getSubjectDN().getName());            	
            	m_logger.debug("Check CA cert: " + caCert.getSubjectDN().getName());
        	}
        	String strTime = new java.util.Date().toString();
            byte[] nonce1 = SignedDoc.digest(strTime.getBytes()); // sha256?
            OCSPReq req = createOCSPRequest(nonce1, cert, caCert, m_bSignRequests);
            //debugWriteFile("req1.der", req.getEncoded());
            if(m_logger.isDebugEnabled()) {
            	m_logger.debug("Sending ocsp request: " + req.getEncoded().length + " bytes");
                m_logger.debug("REQUEST:\n" + Base64Util.encode(req.getEncoded(), 0));
            }    
            // send it
            OCSPResp resp = sendRequest(req, null, null, null);
            //debugWriteFile("resp1.der", resp.getEncoded());
            if(m_logger.isDebugEnabled()) {
                m_logger.debug("Got ocsp response: " + resp.getEncoded().length + " bytes");
                m_logger.debug("RESPONSE:\n" + Base64Util.encode(resp.getEncoded(), 0));
            }
            // check response status
            verifyRespStatus(resp);
            // now read the info from the response
            BasicOCSPResp basResp = 
                (BasicOCSPResp)resp.getResponseObject();
            byte[] nonce2 = getNonce(basResp, null);
            if(!SignedDoc.compareDigests(nonce1, nonce2)) 
            	throw new DigiDocException(DigiDocException.ERR_OCSP_UNSUCCESSFULL,
                    "Invalid nonce value! Possible replay attack!", null); 
            // verify the response
            try {
            	String respId = responderIDtoString(basResp);
            	X509Certificate notaryCert = getNotaryCert(SignedDoc.getCommonName(respId), null);
            	boolean bOk = basResp.verify(notaryCert.getPublicKey(), "BC");
            	if(!bOk) {
            		m_logger.error("OCSP Signature verification error!!!"); 
                    throw new DigiDocException(DigiDocException.ERR_OCSP_VERIFY, "OCSP Signature verification error!!!", null );
            	}
            } catch (Exception ex) {
                m_logger.error("OCSP Signature verification error!!!", ex); 
                DigiDocException.handleException(ex, DigiDocException.ERR_OCSP_VERIFY);
            } 
            // check the response about this certificate
            checkCertStatus(cert, basResp, caCert);
            } else  {
            	CRLFactory crlFac = ConfigManager.instance().getCRLFactory();
            	crlFac.checkCertificate(cert, new Date());
            }
        } catch(DigiDocException ex) {
            throw ex;
        } catch(Exception ex) {
            DigiDocException.handleException(ex, DigiDocException.ERR_OCSP_GET_CONF);        
        }
    }    
    
    /**
     * Check the response and parse it's data.
     * @param sig Signature object
     * @param resp OCSP response
     * @param nonce1 nonve value used for request
     * @param notaryCert notarys own cert
     * @returns Notary object
     */
    private Notary parseAndVerifyResponse(Signature sig, OCSPResp resp, 
        byte[] nonce1/*, X509Certificate notaryCert*/)
        throws DigiDocException
    {
    	String notId = sig.getId().replace('S', 'N');
    	X509Certificate sigCert = sig.getKeyInfo().getSignersCertificate();
    	return parseAndVerifyResponse(sig, notId, sigCert, resp, nonce1, null, null);
    }
    

    /**
     * Check the response and parse it's data
     * @param sig Signature object
     * @param notId new id for Notary object
     * @param signersCert signature owners certificate
     * @param resp OCSP response
     * @param nonce1 nonve value used for request
     * @returns Notary object
     */
    private Notary parseAndVerifyResponse(Signature sig, String notId, 
    	X509Certificate signersCert, OCSPResp resp, byte[] nonce1, X509Certificate notaryCert, X509Certificate caCert)
        throws DigiDocException
    {
        Notary not = null;
        
        // check the result
        if(resp == null || resp.getStatus() != OCSPRespStatus.SUCCESSFUL)
            throw new DigiDocException(DigiDocException.ERR_OCSP_UNSUCCESSFULL,
                "OCSP response unsuccessfull!", null);
        try {            
            // now read the info from the response
            BasicOCSPResp basResp = 
                (BasicOCSPResp)resp.getResponseObject();
            // find real notary cert suitable for this response
            String respId = responderIDtoString(basResp);
            if(notaryCert == null) {
            	String nCn = ConvertUtils.getCommonName(respId);
            	/*int n = nCn.indexOf(',');
            	if(n > 0)
            		nCn = nCn.substring(0, n); */ // fix CN search
            	notaryCert = getNotaryCert(nCn, null);
            	if(m_logger.isDebugEnabled())
            	  m_logger.debug("Find notary cert: " + nCn + " found: " + ((notaryCert != null) ? "OK" : "NULL"));
            }
            if(notaryCert == null) {
            	throw new DigiDocException(DigiDocException.ERR_OCSP_VERIFY, "Notary cert not found for: " + respId, null);
            }
            // verify the response
            boolean bOk = false;
            try {
            	bOk = basResp.verify(notaryCert.getPublicKey(), "BC");
            } catch (Exception ex) {
                m_logger.error("OCSP Signature verification error!!!", ex); 
                DigiDocException.handleException(ex, DigiDocException.ERR_OCSP_VERIFY);
            }
            if(!bOk) {
            	 m_logger.error("OCSP Signature verification error!!!"); 
                 throw new DigiDocException(DigiDocException.ERR_OCSP_VERIFY, "OCSP Signature verification error!!!", null);
            }
            if(m_logger.isDebugEnabled() && notaryCert != null)
        		m_logger.debug("Using responder cert: " + notaryCert.getSerialNumber().toString());
            // done't care about SingleResponses because we have
            // only one response and the whole response was successfull
            // but we should verify that the nonce hasn't changed
            byte[] nonce2 = getNonce(basResp, (sig != null) ? sig.getSignedDoc() : null);
            boolean ok = true;
            if(nonce1 == null || nonce2 == null || nonce1.length != nonce2.length)
                ok = false;
            for(int i = 0; (nonce1 != null) && (nonce2 != null) && (i < nonce1.length); i++)
                if(nonce1[i] != nonce2[i])
                    ok = false;
            if(!ok && sig != null) {
            	m_logger.error("DDOC ver: " + sig.getSignedDoc().getVersion() + " SIG: " + sig.getId() +
            			" Real nonce: " + Base64Util.encode(nonce2, 0)
            			+ " SigVal hash: " + Base64Util.encode(nonce1, 0)
            			+ " SigVal hash hex: " + ConvertUtils.bin2hex(nonce1));
                throw new DigiDocException(DigiDocException.ERR_OCSP_NONCE,
                    "OCSP response's nonce doesn't match the requests nonce!", null);
            }
            // check the response on our cert
            checkCertStatus(signersCert, basResp, caCert);
            // create notary            
            not = new Notary(notId, resp.getEncoded(), respId, basResp.getResponseData().getProducedAt()); 
            if(notaryCert != null)
            	not.setCertNr(notaryCert.getSerialNumber().toString());
        } catch(DigiDocException ex) {
            throw ex;
        } catch(Exception ex) {
            DigiDocException.handleException(ex, DigiDocException.ERR_OCSP_PARSE);
        }
        return not;
    }


    /**
     * Verifies that the OCSP response is about our signers
     * cert and the response status is successfull
     * @param sig Signature object
     * @param basResp OCSP Basic response
     * @throws DigiDocException if the response is not successfull
     */
    private void checkCertStatus(Signature sig, BasicOCSPResp basResp)
        throws DigiDocException
    {
        checkCertStatus(sig.getKeyInfo().getSignersCertificate(), basResp, null);
    }
    
    
    /**
     * Verifies that the OCSP response is about our signers
     * cert and the response status is successfull
     * @param sig Signature object
     * @param basResp OCSP Basic response
     * @throws DigiDocException if the response is not successfull
     */
    private void checkCertStatus(X509Certificate cert, BasicOCSPResp basResp, X509Certificate caCert)
        throws DigiDocException
    {
        try {
        	if(m_logger.isDebugEnabled())
            	m_logger.debug("Checking response status, CERT: " + ((cert != null) ? cert.getSubjectDN().getName() : "NULL") + 
            		" SEARCH: " + ((cert != null) ? SignedDoc.getCommonName(ConvertUtils.convX509Name(cert.getIssuerX500Principal())) : "NULL"));
        	if(cert == null)
        		throw new DigiDocException(DigiDocException.ERR_CERT_UNKNOWN,
        				"No certificate to check! Error reading certificate from file?", null);
            // check the response on our cert
        	TrustServiceFactory tslFac = ConfigManager.instance().getTslFactory();
        	boolean bUseLocal = ConfigManager.instance().getBooleanProperty("DIGIDOC_USE_LOCAL_TSL", false);
            if(caCert == null)
            	caCert = tslFac.findCaForCert(cert, bUseLocal);
            if(m_logger.isDebugEnabled()) {
            	m_logger.debug("CA cert: " + ((caCert != null) ? caCert.getSubjectDN().getName() : "NULL"));
            	m_logger.debug("RESP: " + basResp);
            	m_logger.debug("CERT: " + ((cert != null) ? cert.getSubjectDN().getName() : "NULL") + 
            				" ISSUER: " + ConvertUtils.convX509Name(cert.getIssuerX500Principal()) +
            				" nr: " + ((caCert != null) ? ConvertUtils.bin2hex(caCert.getSerialNumber().toByteArray()) : "NULL"));
            }
            if(caCert == null)
            	throw new DigiDocException(DigiDocException.ERR_CERT_UNKNOWN, "Unknown CA cert: " + cert.getIssuerDN().getName(), null);
            SingleResp[] sresp = basResp.getResponseData().getResponses();
            CertificateID rc = creatCertReq(cert, caCert);
            //ertificateID certId = creatCertReq(signersCert, caCert);
            if(m_logger.isDebugEnabled())
                m_logger.debug("Search alg: " + rc.getHashAlgOID() + " cert ser: " + cert.getSerialNumber().toString() +
            	" serial: " + rc.getSerialNumber() + " issuer: " + Base64Util.encode(rc.getIssuerKeyHash()) +
            	" subject: " + Base64Util.encode(rc.getIssuerNameHash()));
            boolean ok = false;
            for(int i=0;i < sresp.length;i++) {
            	CertificateID id = sresp[i].getCertID();
            	if(id != null) {
            		if(m_logger.isDebugEnabled())
                		m_logger.debug("Got alg: " + id.getHashAlgOID() + 
            			" serial: " + id.getSerialNumber() + 
            			" issuer: " + Base64Util.encode(id.getIssuerKeyHash()) +
            			" subject: " + Base64Util.encode(id.getIssuerNameHash()));
            		if(rc.getHashAlgOID().equals(id.getHashAlgOID()) &&
            			rc.getSerialNumber().equals(id.getSerialNumber()) &&
            			SignedDoc.compareDigests(rc.getIssuerKeyHash(), id.getIssuerKeyHash()) &&
            			SignedDoc.compareDigests(rc.getIssuerNameHash(), id.getIssuerNameHash())) {
            			if(m_logger.isDebugEnabled())
                			m_logger.debug("Found it!");
            			ok = true;
            			Object status = sresp[i].getCertStatus();
            			if(status != null) {
            				if(m_logger.isDebugEnabled())
                				m_logger.debug("CertStatus: " + status.getClass().getName());
            			   	if(status instanceof RevokedStatus) {
            			   		m_logger.error("Certificate has been revoked!");
            					throw new DigiDocException(DigiDocException.ERR_CERT_REVOKED,
                    				"Certificate has been revoked!", null);
            			   	}
            			   	if(status instanceof UnknownStatus) {
            			   		m_logger.error("Certificate status is unknown!");
            					throw new DigiDocException(DigiDocException.ERR_CERT_UNKNOWN,
                    				"Certificate status is unknown!", null);
            			   	}
            			   	   	
            			}
            			break;
            		}
            	}
            }

            if(!ok) {
            	if(m_logger.isDebugEnabled())
                	m_logger.debug("Error checkCertStatus - not found ");
                throw new DigiDocException(DigiDocException.ERR_OCSP_RESP_STATUS,
                    "Bad OCSP response status!", null);
            }
            //System.out.println("Response status OK!");
        } catch(DigiDocException ex) {
        	throw ex;
        } catch(Exception ex) {
        	m_logger.error("Error checkCertStatus: " + ex);
        	System.out.println("Error checkCertStatus: " + ex);
        	ex.printStackTrace();
            throw new DigiDocException(DigiDocException.ERR_OCSP_RESP_STATUS,
                    "Error checking OCSP response status!", null);
        }
    }
    
    
    /**
     * Verifies that the OCSP response is about our signers
     * cert and the response status is successfull
     * @param sig Signature object
     * @param basResp OCSP Basic response
     * @throws DigiDocException if the response is not successfull
     */
    private void checkCertStatusWithCa(X509Certificate cert, BasicOCSPResp basResp, X509Certificate caCert)
        throws DigiDocException
    {
        try {
        	if(m_logger.isDebugEnabled())
            	m_logger.debug("Checking response status, CERT: " + cert.getSubjectDN().getName() + 
            		" SEARCH: " + SignedDoc.getCommonName(ConvertUtils.convX509Name(cert.getIssuerX500Principal())));
            // check the response on our cert
            DigiDocFactory ddocFac = ConfigManager.instance().getDigiDocFactory();
        	//X509Certificate caCert = (X509Certificate)m_ocspCACerts.
            //	get(SignedDoc.getCommonName(ConvertUtils.convX509Name(cert.getIssuerX500Principal())));
            if(m_logger.isDebugEnabled()) {
            	m_logger.debug("CA cert: " + ((caCert == null) ? "NULL" : "OK"));
            	m_logger.debug("RESP: " + basResp);
            	m_logger.debug("CERT: " + cert.getSubjectDN().getName() + 
            				" ISSUER: " + ConvertUtils.convX509Name(cert.getIssuerX500Principal()));
            	if(caCert != null)
            	  m_logger.debug("CA CERT: " + caCert.getSubjectDN().getName());
            }
            SingleResp[] sresp = basResp.getResponseData().getResponses();
            CertificateID rc = creatCertReq(cert, caCert);
            //ertificateID certId = creatCertReq(signersCert, caCert);
            if(m_logger.isDebugEnabled())
                m_logger.debug("Search alg: " + rc.getHashAlgOID() + 
            	" serial: " + rc.getSerialNumber() + " issuer: " + Base64Util.encode(rc.getIssuerKeyHash()) +
            	" subject: " + Base64Util.encode(rc.getIssuerNameHash()));
            boolean ok = false;
            for(int i=0;i < sresp.length;i++) {
            	CertificateID id = sresp[i].getCertID();
            	if(id != null) {
            		if(m_logger.isDebugEnabled())
                		m_logger.debug("Got alg: " + id.getHashAlgOID() + 
            			" serial: " + id.getSerialNumber() + 
            			" issuer: " + Base64Util.encode(id.getIssuerKeyHash()) +
            			" subject: " + Base64Util.encode(id.getIssuerNameHash()));
            		if(rc.getHashAlgOID().equals(id.getHashAlgOID()) &&
            			rc.getSerialNumber().equals(id.getSerialNumber()) &&
            			SignedDoc.compareDigests(rc.getIssuerKeyHash(), id.getIssuerKeyHash()) &&
            			SignedDoc.compareDigests(rc.getIssuerNameHash(), id.getIssuerNameHash())) {
            			if(m_logger.isDebugEnabled())
                			m_logger.debug("Found it!");
            			ok = true;
            			Object status = sresp[i].getCertStatus();
            			if(status != null) {
            				if(m_logger.isDebugEnabled())
                				m_logger.debug("CertStatus: " + status.getClass().getName());
            			   	if(status instanceof RevokedStatus) {
            			   		m_logger.error("Certificate has been revoked!");
            					throw new DigiDocException(DigiDocException.ERR_OCSP_RESP_STATUS,
                    				"Certificate has been revoked!", null);
            			   	}
            			   	if(status instanceof UnknownStatus) {
            			   		m_logger.error("Certificate status is unknown!");
            					throw new DigiDocException(DigiDocException.ERR_OCSP_RESP_STATUS,
                    				"Certificate status is unknown!", null);
            			   	}
            			   	   	
            			}
            			break;
            		}
            	}
            }

            if(!ok) {
            	if(m_logger.isDebugEnabled())
                	m_logger.debug("Error checkCertStatus - not found ");
                throw new DigiDocException(DigiDocException.ERR_OCSP_RESP_STATUS,
                    "Bad OCSP response status!", null);
            }
            //System.out.println("Response status OK!");
        } catch(DigiDocException ex) {
        	throw ex;
        } catch(Exception ex) {
        	m_logger.error("Error checkCertStatus: " + ex);
        	System.out.println("Error checkCertStatus: " + ex);
        	ex.printStackTrace();
            throw new DigiDocException(DigiDocException.ERR_OCSP_RESP_STATUS,
                    "Error checking OCSP response status!", null);
        }
    }
    
	/*   System.out.println("Writing debug file: " + str);
            FileOutputStream fos = new FileOutputStream(str);
            fos.write(data);
            fos.close();
        } catch(Exception ex) {
            System.out.println("Error: " + ex);
            ex.printStackTrace(System.out);
        }
    }*/
    
    /**
     * Check the response and parse it's data
     * Used by UnsignedProperties.verify()
     * @param not initial Notary object that contains only the
     * raw bytes of an OCSP response
     * @returns Notary object with data parsed from OCSP response
     */
    public Notary parseAndVerifyResponse(Signature sig, Notary not)
        throws DigiDocException
    {
        try {     
        	// DEBUG
        	//debugWriteFile("respin.resp", not.getOcspResponseData());
            OCSPResp  resp = new OCSPResp(not.getOcspResponseData());
            // now read the info from the response
            BasicOCSPResp basResp = (BasicOCSPResp)resp.getResponseObject();
            // verify the response
            X509Certificate[] lNotCerts = null;
            if(sig != null && sig.getUnsignedProperties() != null &&
            		sig.getUnsignedProperties().getRespondersCertificate() == null) {
    			throw new DigiDocException(DigiDocException.ERR_RESPONDERS_CERT, "OCSP responders certificate is required!", null);
    		}
            try {
            	String respondIDstr = responderIDtoString(basResp);
            	
            	if(m_logger.isDebugEnabled()) {
                	m_logger.debug("SIG: " + ((sig == null) ? "NULL" : sig.getId()));
                	m_logger.debug("UP: " + ((sig.getUnsignedProperties() == null) ? "NULL" : "OK: " + sig.getUnsignedProperties().getNotary().getId()));
                	m_logger.debug("RESP-CERT: " + ((sig.getUnsignedProperties().
        					getRespondersCertificate() == null) ? "NULL" : "OK"));
                	m_logger.debug("RESP-ID: " + respondIDstr);
                	ee.sk.digidoc.CertID cid = sig.getCertID(ee.sk.digidoc.CertID.CERTID_TYPE_RESPONDER);
                	if(cid != null)
                		m_logger.debug("CID: " + cid.getType() + " id: " + cid.getId() +
                				", " + cid.getSerial() + " issuer: " + cid.getIssuer());
                	m_logger.debug("RESP: " + Base64Util.encode(resp.getEncoded()));
            	}
            	if(lNotCerts == null && sig != null) {
            		String ddocRespCertNr = sig.getUnsignedProperties().
						getRespondersCertificate().getSerialNumber().toString();
            		String respSrch = respondIDstr;
            		if((respSrch.indexOf("CN") != -1))
            			respSrch = ConvertUtils.getCommonName(respondIDstr);
            		if(respSrch.startsWith("byKey: "))
            			respSrch = respSrch.substring("byKey: ".length());
            		int n1 = respSrch.indexOf(',');
            		if(n1 > 0)
            			respSrch = respSrch.substring(0, n1);
            		if(m_logger.isDebugEnabled())
            			m_logger.debug("Search not cert by: " + respSrch + " nr: " + ddocRespCertNr);
            		// TODO: get multiple certs
            		lNotCerts = getNotaryCerts(respSrch, null /*ddocRespCertNr*/);
            	}
            	if(lNotCerts == null || lNotCerts.length == 0)
            		throw new DigiDocException(DigiDocException.ERR_OCSP_RECPONDER_NOT_TRUSTED, 
            				"No certificate for responder: \'" + respondIDstr + "\' found in local certificate store!", null);
            	boolean bOk = false;
            	for(int j = 0; (lNotCerts != null) && (j < lNotCerts.length) && !bOk; j++) {
            		X509Certificate cert = lNotCerts[j];
            		if(m_logger.isDebugEnabled())
            			m_logger.debug("Verify using responders cert: " + 
            					((cert != null) ? ConvertUtils.getCommonName(cert.getSubjectDN().getName()) + " nr: " + cert.getSerialNumber().toString() : "NULL"));
            		bOk = basResp.verify(cert.getPublicKey(), "BC"); 
            		if(m_logger.isDebugEnabled())
            			m_logger.debug("OCSP resp: " + ((basResp != null) ? responderIDtoString(basResp) : "NULL") +
                			" verify using: " + ((cert != null) ? ConvertUtils.getCommonName(cert.getSubjectDN().getName()) : "NULL") +
                			" verify: " + bOk);
            	}
                if(!bOk)
                  throw new DigiDocException(DigiDocException.ERR_OCSP_VERIFY, "OCSP verification error!", null);
            } catch (Exception ex) {
                m_logger.error("Signature verification error: " + ex); 
                ex.printStackTrace();
                DigiDocException.handleException(ex, DigiDocException.ERR_OCSP_VERIFY);
            } 
            // done't care about SingleResponses because we have
            // only one response and the whole response was successfull
            // but we should verify that the nonce hasn't changed
            // calculate the nonce
            byte[] nonce1 = SignedDoc.digestOfType(sig.getSignatureValue().getValue(),
            		sig.getSignedDoc().getFormat().equals(SignedDoc.FORMAT_BDOC) ? SignedDoc.SHA256_DIGEST_TYPE : SignedDoc.SHA1_DIGEST_TYPE);
            byte[] nonce2 = getNonce(basResp, sig.getSignedDoc());
            boolean ok = true;
            if(nonce1 == null || nonce2 == null || nonce1.length != nonce2.length)
                ok = false;
            for(int i = 0; (nonce1 != null) && (nonce2 != null) && (i < nonce1.length); i++)
                if(nonce1[i] != nonce2[i])
                    ok = false;
            // TODO: investigate further
            if(!ok && sig.getSignedDoc() != null) {
            	if(m_logger.isDebugEnabled()) {
                	m_logger.debug("SigVal\n---\n" + Base64Util.encode(sig.getSignatureValue().getValue()) +
                			"\n---\nOCSP\n---\n" + Base64Util.encode(not.getOcspResponseData()) + "\n---\n");
                	m_logger.debug("DDOC ver: " + sig.getSignedDoc().getVersion() + 
            			" SIG: " + sig.getId() + " NOT: " + not.getId() +
            			" Real nonce: " + ((nonce2 != null) ? Base64Util.encode(nonce2, 0) : "NULL") + " noncelen: " + ((nonce2 != null) ? nonce2.length : 0)
            			+ " SigVal hash: " + Base64Util.encode(nonce1, 0)
            			+ " SigVal hash hex: " + ConvertUtils.bin2hex(nonce1) + " svlen: " + ((nonce1 != null) ? nonce1.length : 0));
                	m_logger.debug("SIG:\n---\n" + sig.toString() + "\n--\n");
            	}
                throw new DigiDocException(DigiDocException.ERR_OCSP_NONCE,
                    "OCSP response's nonce doesn't match the requests nonce!", null);
            }
            if(m_logger.isDebugEnabled())
            	m_logger.debug("Verify not: " + not.getId());
            // check the response on our cert
            //if(not.getId() != null && not.getId().indexOf('-') == -1) { // signers certs ocsp
              checkCertStatus(sig, basResp);
            /*} else {
              X509Certificate cert = null, caCert = null;
              if(m_logger.isDebugEnabled())
              	  m_logger.debug("Verify not: " + not.getId());
              int n = not.getId().indexOf('-');
              String cId = not.getId().substring(n+1);
              cert = getCACert(cId);
              OcspRef orf = sig.getUnsignedProperties().getCompleteRevocationRefs().getOcspRefByUri("#"+not.getId());
              if(orf != null) {
            	  String caId = SignedDoc.getCommonName(orf.getResponderCommonName());
            	  caCert = getCACert(caId);
              }
              if(cert != null && caCert != null) {
                if(m_logger.isDebugEnabled())
              	  m_logger.debug("Verify not: " + not.getId() + " cert: " + cert.getSubjectDN().getName() + " ca: " + caCert.getSubjectDN().getName());
                checkCertStatusWithCa(cert, basResp, caCert);
              }
            }*/
            not.setProducedAt(basResp.getResponseData().getProducedAt());
            not.setResponderId(responderIDtoString(basResp));
        } catch(DigiDocException ex) {
            throw ex;
        } catch(Exception ex) {
            DigiDocException.handleException(ex, DigiDocException.ERR_OCSP_PARSE);
        }
        return not;
    }
    
    
    /**
	 * Get String represetation of ResponderID
	 * @param basResp
	 * @return stringified responder ID
	 */
	private String responderIDtoString(BasicOCSPResp basResp) {
		if(basResp != null) {
			ResponderID respid = basResp.getResponseData().getResponderId().toASN1Object();
			Object o = ((DERTaggedObject)respid.toASN1Object()).getObject();
			if(o instanceof org.bouncycastle.asn1.DEROctetString) {
				org.bouncycastle.asn1.DEROctetString oc = (org.bouncycastle.asn1.DEROctetString)o;
				return "byKey: " + SignedDoc.bin2hex(oc.getOctets()); 
			} else {
				X509Name name = new X509Name((ASN1Sequence)o);
				return "byName: " + name.toString();
			}
		}
		else
			return null;
	}
	
	private static final int V_ASN1_OCTET_STRING = 4;
	/**
	 * Method to get NONCE array from responce
	 * @param basResp
	 * @return OCSP nonce value
	 */
	private byte[] getNonce(BasicOCSPResp basResp, SignedDoc sdoc) {
		if(basResp != null) {
			try {
			X509Extensions ext = basResp.getResponseData().getResponseExtensions();
			X509Extension ex1 = ext.getExtension(new DERObjectIdentifier(nonceOid));
			byte[] nonce2 = ex1.getValue().getOctets();
			boolean bCheckOcspNonce = ConfigManager.instance().getBooleanProperty("CHECK_OCSP_NONCE", false);
			if(m_logger.isDebugEnabled())
            	m_logger.debug("Nonce hex: " + ConvertUtils.bin2hex(nonce2) + " b64: " + Base64Util.encode(nonce2) + " len: " + nonce2.length);
			boolean bAsn1=false;
            if(sdoc != null && sdoc.getFormat().equals(SignedDoc.FORMAT_DIGIDOC_XML) || sdoc == null) {
            	if(nonce2 != null && nonce2.length == 22 /*&& nonce2[0] == V_ASN1_OCTET_STRING*/) {
            		byte[] b = new byte[20];
            		System.arraycopy(nonce2, nonce2.length - 20, b, 0, 20);
            		nonce2 = b;
            		bAsn1=true;
            	} 
            }
            if(sdoc != null && sdoc.getFormat().equals(SignedDoc.FORMAT_BDOC)) {
            	if(nonce2 != null && nonce2.length == 34) {
            		byte[] b = new byte[32];
            		System.arraycopy(nonce2, nonce2.length - 32, b, 0, 32);
            		nonce2 = b;
            		bAsn1=true;
            	} 
            }
            if(m_logger.isDebugEnabled())
            	m_logger.debug("Nonce hex: " + ConvertUtils.bin2hex(nonce2) + " b64: " + Base64Util.encode(nonce2) + " len: " + nonce2.length);
            if(!bAsn1 && bCheckOcspNonce) {
        		throw new DigiDocException(DigiDocException.ERR_OCSP_NONCE,
                        "Invalid nonce: " + ConvertUtils.bin2hex(nonce2) + " length: " + nonce2.length+ "!", null);
        	}
            return nonce2;
			} catch(Exception ex) {
				m_logger.error("Error reading ocsp nonce: " + ex);
				return null;
			}
		}
		else
			return null;
	}

	/**
	 * Helper method to verify response status
	 * @param resp OCSP response
	 * @throws DigiDocException if the response status is not ok
	 */
	private void verifyRespStatus(OCSPResp resp) 
		throws DigiDocException 
	{
		int status = resp.getStatus();
			switch (status) {
				case OCSPRespStatus.INTERNAL_ERROR: m_logger.error("An internal error occured in the OCSP Server!"); break;
				case OCSPRespStatus.MALFORMED_REQUEST: m_logger.error("Your request did not fit the RFC 2560 syntax!"); break;
				case OCSPRespStatus.SIGREQUIRED: m_logger.error("Your request was not signed!"); break;
				case OCSPRespStatus.TRY_LATER: m_logger.error("The server was too busy to answer you!"); break;
				case OCSPRespStatus.UNAUTHORIZED: m_logger.error("The server could not authenticate you!"); break;
				case OCSPRespStatus.SUCCESSFUL: break;
				default: m_logger.error("Unknown OCSPResponse status code! "+status);
			}
		if(resp == null || resp.getStatus() != OCSPRespStatus.SUCCESSFUL)
		    throw new DigiDocException(DigiDocException.ERR_OCSP_UNSUCCESSFULL,
		        "OCSP response unsuccessfull! ", null);
	}

    
    /**
	 * Method for creating CertificateID for OCSP request
	 * @param signersCert
	 * @param caCert
	 * @param provider
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws CertificateEncodingException
	 */
	private CertificateID creatCertReq(X509Certificate signersCert, X509Certificate caCert)
		throws NoSuchAlgorithmException, NoSuchProviderException, 
		CertificateEncodingException, DigiDocException, Exception
	{
		return new CertificateID(CertificateID.HASH_SHA1, caCert, signersCert.getSerialNumber());
	}

    
    
    /**
     * Creates a new OCSP request
     * @param nonce 128 byte RSA+SHA1 signatures digest
     * Use null if you want to verify only the certificate
     * and this is not related to any signature
     * @param signersCert signature owners cert
     * @param caCert CA cert for this signer
     * @param bSigned flag signed request or not
     */
    private OCSPReq createOCSPRequest(byte[] nonce, X509Certificate signersCert, 
    	X509Certificate caCert, boolean bSigned)
        throws DigiDocException 
    {
    	OCSPReq req = null;
        OCSPReqGenerator ocspRequest = new OCSPReqGenerator();
        try {
        	//Create certificate id, for OCSP request
        	CertificateID certId = creatCertReq(signersCert, caCert);
        	if(m_logger.isDebugEnabled())
    		  m_logger.debug("Request for: " + certId.getHashAlgOID() + 
			  " serial: " + certId.getSerialNumber() + 
			  " issuer: " + ConvertUtils.bin2hex(certId.getIssuerKeyHash()) +
			  " subject: " + ConvertUtils.bin2hex(certId.getIssuerNameHash()) + 
			  " nonce: " + ConvertUtils.bin2hex(nonce));
			ocspRequest.addRequest(certId);
			if(nonce != null && nonce[0] != V_ASN1_OCTET_STRING) {
				byte[] b = new byte[nonce.length + 2];
				b[0] = V_ASN1_OCTET_STRING;
				b[1] = (byte)nonce.length;
				System.arraycopy(nonce, 0, b, 2, nonce.length);
				if(m_logger.isDebugEnabled())
		    		  m_logger.debug("Nonce in: " + ConvertUtils.bin2hex(nonce) + " with-asn1: " + ConvertUtils.bin2hex(b));
				nonce = b;
			}
			if(nonce!=null) {
				/*ASN1OctetString ocset = new BERConstructedOctetString(nonce);
				X509Extension ext = new X509Extension(false, ocset);
				//nonce Identifier
				DERObjectIdentifier nonceIdf = new DERObjectIdentifier(nonceOid);
				Hashtable tbl = new Hashtable(1);
				tbl.put(nonceIdf, ext);
				// create extendions, with one extendion(NONCE)
				X509Extensions extensions = new X509Extensions(tbl);*/
				Vector oids = new Vector();
		        Vector values = new Vector();
		        oids.add(OCSPObjectIdentifiers.id_pkix_ocsp_nonce);
		        values.add(new X509Extension(false, new DEROctetString(nonce)));
		        X509Extensions ret = new X509Extensions(oids, values);
				ocspRequest.setRequestExtensions(ret);
			}
			//X509Name n = new X509Name()
			GeneralName name = null;

			X509Certificate signCert = ocspSignCert.get().getSignCert();
			
			if (bSigned) {
				

				if(m_logger.isDebugEnabled()) {
					
					m_logger.debug("SignCert: " + ((signCert != null) ? signCert.toString() : "NULL"));
				}

				if(signCert == null) {
					throw new DigiDocException(DigiDocException.ERR_INVALID_CONFIG, "Invalid config file! Attempting to sign ocsp request but PKCS#12 token not configured!", null);
				}
				
				name = new GeneralName(PrincipalUtil.getSubjectX509Principal(signCert));
			} else {
				if(signersCert == null)
					throw new DigiDocException(DigiDocException.ERR_OCSP_SIGN, "Signature owners certificate is NULL!", null);
				name = new GeneralName(PrincipalUtil.getSubjectX509Principal(signersCert));
			}
    
			ocspRequest.setRequestorName(name);
			
			if(bSigned) {
				// lets generate signed request
				X509Certificate[] chain = {signCert};
				req = ocspRequest.generate("SHA1WITHRSA", ocspSignCert.get().getSignKey(), chain, "BC");
				if(!req.verify(signCert.getPublicKey(), "BC")){
					m_logger.error("Verify failed");
				}
			} else { // unsigned request
				req = ocspRequest.generate();
			}
                
        } catch(Exception ex) {
            DigiDocException.handleException(ex, DigiDocException.ERR_OCSP_REQ_CREATE);
        }
        return req;
    }

    
    /**
     * Sends the OCSP request to Notary and
     * retrieves the response
     * @param req OCSP request
     * @param httpFrom HTTP_FROM value (optional)
     * @returns OCSP response
     */
    private OCSPResp sendRequest(OCSPReq req, String httpFrom, String format, String formatVer)
        throws DigiDocException 
    {
    	String responderUrl = ConfigManager.instance().
        	getProperty("DIGIDOC_OCSP_RESPONDER_URL");
    	return sendRequestToUrl(req, responderUrl, httpFrom, format, formatVer);
    }
    
    private String getUserInfo(String format, String formatVer)
    {
    	StringBuffer sb = null;
    	try {
    		sb = new StringBuffer("LIB ");
    		sb.append(SignedDoc.LIB_NAME);
    		sb.append("/");
    		sb.append(SignedDoc.LIB_VERSION);
    		if(format != null && formatVer != null) {
    			sb.append(" format: ");
        		sb.append(format);
        		sb.append("/");
        		sb.append(formatVer);
    		}
    		sb.append(" Java: ");
    		sb.append(System.getProperty("java.version"));
    		sb.append("/");
    		sb.append(System.getProperty("java.vendor"));
    		sb.append(" OS: ");
    		sb.append(System.getProperty("os.name"));
    		sb.append("/");
    		sb.append(System.getProperty("os.arch"));
    		sb.append("/");
    		sb.append(System.getProperty("os.version"));
    		sb.append(" JVM: ");
    		sb.append(System.getProperty("java.vm.name"));
    		sb.append("/");
    		sb.append(System.getProperty("java.vm.vendor"));
    		sb.append("/");
    		sb.append(System.getProperty("java.vm.version"));
    	} catch(Throwable ex) {
    		m_logger.error("Error reading java system properties: " + ex);
    	}
    	return ((sb != null) ? sb.toString() : null);
    }
    
    /**
     * Sends the OCSP request to Notary and
     * retrieves the response
     * @param req OCSP request
     * @param url OCSP responder url
     * @param httpFrom HTTP_FROM value (optional)
     * @returns OCSP response
     */
    private OCSPResp sendRequestToUrl(OCSPReq req, String url, String httpFrom, String format, String formatVer)
        throws DigiDocException 
    {
        OCSPResp resp = null;
        try {
            byte[] breq = req.getEncoded();
            URL uUrl = new URL(url);
            if(m_logger.isDebugEnabled())
				m_logger.debug("Connecting to ocsp url: " + url);
            URLConnection con = uUrl.openConnection();
            int nTmout = con.getConnectTimeout();
            if(m_logger.isDebugEnabled())
				m_logger.debug("Default connection timeout: " + nTmout + " [ms]");
            int nConfTm = ConfigManager.instance().getIntProperty("OCSP_TIMEOUT", -1);
            if(nConfTm >= 0) {
            	if(m_logger.isDebugEnabled())
    				m_logger.debug("Setting connection timeout to: " + nConfTm + " [ms]");
            	con.setConnectTimeout(nConfTm);
            }
            con.setAllowUserInteraction(false);
            con.setUseCaches(false);
            con.setDoOutput(true);
            con.setDoInput(true);
            // send the OCSP request
            con.setRequestProperty("Content-Type", "application/ocsp-request");
            String sUserInfo = getUserInfo(format, formatVer);
            if(sUserInfo != null) {
            	if(m_logger.isDebugEnabled())
					m_logger.debug("User-Agent: " + sUserInfo);
            	con.setRequestProperty("User-Agent", sUserInfo);
            }
            if(httpFrom != null && httpFrom.trim().length() > 0) {
            	if(m_logger.isDebugEnabled())
					m_logger.debug("HTTP_FROM: " + httpFrom);
            	con.setRequestProperty("HTTP_FROM", httpFrom);
            }
            OutputStream os = con.getOutputStream();
            os.write(breq);
            os.close();
            // read the response
            InputStream is = con.getInputStream();
            int cl = con.getContentLength();
            byte[] bresp = null;
            //System.out.println("Content: " + cl + " bytes");
            if(cl > 0) {
                int avail = 0;
                do {
                    avail = is.available();
                    byte[] data = new byte[avail];
                    int rc = is.read(data);
                    if(bresp == null) {
                        bresp = new byte[rc];
                        System.arraycopy(data, 0, bresp, 0, rc);
                    } else {
                        byte[] tmp = new byte[bresp.length + rc];
                        System.arraycopy(bresp, 0, tmp, 0, bresp.length);
                        System.arraycopy(data, 0, tmp, bresp.length, rc);
                        bresp = tmp;
                    }
                    //System.out.println("Got: " + avail + "/" + rc + " bytes!");
                    cl -= rc;
                } while(cl > 0);
            }
            is.close();
            if(bresp != null) {
            	//debugWriteFile("response-bc.resp", bresp);
                resp = new OCSPResp(bresp);     
                //System.out.println("Response: " + resp.toString());
            }
        } catch(Exception ex) {
            DigiDocException.handleException(ex, DigiDocException.ERR_OCSP_REQ_SEND);        
        }
        return resp;
    }
    
    /**l
     * initializes the implementation class
     */
    public void init()
        throws DigiDocException 
    {
        try {
            String proxyHost = ConfigManager.instance().
                getProperty("DIGIDOC_PROXY_HOST");
            String proxyPort = ConfigManager.instance().
                getProperty("DIGIDOC_PROXY_PORT");
            if(proxyHost != null && proxyPort != null) {
                System.setProperty("http.proxyHost", proxyHost);
                System.setProperty("http.proxyPort", proxyPort);
            }
            String sigFlag = ConfigManager.instance().getProperty("SIGN_OCSP_REQUESTS");
        	m_bSignRequests = (sigFlag != null && sigFlag.equals("true"));
        	// only need this if we must sign the requests
            Provider prv = (Provider)Class.forName(ConfigManager.
                instance().getProperty("DIGIDOC_SECURITY_PROVIDER")).newInstance();
            //System.out.println("Provider");
            //prv.list(System.out);
            Security.addProvider(prv);
                     
        } catch(Exception ex) {
            DigiDocException.handleException(ex, DigiDocException.ERR_NOT_FAC_INIT);
        }
    }
}
