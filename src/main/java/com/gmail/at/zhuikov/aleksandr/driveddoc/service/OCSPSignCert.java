package com.gmail.at.zhuikov.aleksandr.driveddoc.service;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import ee.sk.digidoc.DigiDocException;

public class OCSPSignCert {

	private InputStream stream;
	private String password;
	private X509Certificate signCert;
	private KeyStore keyStore;
	private PrivateKey signKey;
	
	public OCSPSignCert(InputStream stream, String password) {
		this.stream = stream;
		this.password = password;
	}
	
    private KeyStore loadStore(InputStream in, String pass) throws DigiDocException {
    	try {
            
        	KeyStore store = KeyStore.getInstance("PKCS12", "BC");
        	store.load(in, pass.toCharArray());
        	                   
	    	return store;
        } catch(Exception ex) {
            DigiDocException.handleException(ex, DigiDocException.ERR_NOT_FAC_INIT);
            return null;
        }
    }
    
    private KeyStore getKeyStore() throws DigiDocException {
    	if (keyStore == null) {
    		keyStore = loadStore(stream, password);
    	} 
    	
    	return keyStore;
    }
    
    private String findAlias(KeyStore store) throws DigiDocException {
    	try {
	    	java.util.Enumeration en = store.aliases();
	    	// find the key alias
			while(en.hasMoreElements()) {
	    		String  n = (String)en.nextElement();
	    		if (store.isKeyEntry(n)) {
	        		return n;
	    		}
			}
			return null;
        } catch(Exception ex) {
            DigiDocException.handleException(ex, DigiDocException.ERR_NOT_FAC_INIT);
            return null;
        }
    }
    
    public X509Certificate getSignCert() throws DigiDocException {
    	
    	if (signCert == null) {
    		KeyStore store = getKeyStore();
    		signCert = getSignCert(store, findAlias(store));
    	}
    	
		return signCert;
    }
    
    private PrivateKey getSignKey(KeyStore store, String alias) throws DigiDocException {
    	try {
	    	return (PrivateKey)store.getKey(alias, null);
        } catch(Exception ex) {
            DigiDocException.handleException(ex, DigiDocException.ERR_NOT_FAC_INIT);
            return null;
        }
    }
    
    public PrivateKey getSignKey() throws DigiDocException {
    	if (signKey == null) {
    		KeyStore store = getKeyStore();
			signKey = getSignKey(store, findAlias(store));
    	}
    	
    	return signKey;
    }
	
	private X509Certificate getSignCert(KeyStore store, String alias) throws DigiDocException {
    	try {

    		java.security.cert.Certificate[] certs = store.getCertificateChain(alias);
	    	
	    	for (Certificate cert : certs) {
	    		
	    		X509Certificate x509 = (java.security.cert.X509Certificate) cert;
	    		
	    		if (x509.getSubjectDN().getName().contains(alias)) {
	    			return x509;
	    		}
	    	}

	    	return null;
        } catch(Exception ex) {
            DigiDocException.handleException(ex, DigiDocException.ERR_NOT_FAC_INIT);
            return null;
        }
    }
}
