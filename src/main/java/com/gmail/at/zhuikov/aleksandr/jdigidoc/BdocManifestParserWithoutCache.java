package com.gmail.at.zhuikov.aleksandr.jdigidoc;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;

import ee.sk.digidoc.DataFile;
import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.Manifest;
import ee.sk.digidoc.ManifestFileEntry;
import ee.sk.digidoc.SignedDoc;
import ee.sk.digidoc.factory.BdocManifestParser;
import ee.sk.digidoc.factory.SAXDigiDocException;
import ee.sk.digidoc.factory.SAXDigiDocFactory;

public class BdocManifestParserWithoutCache extends BdocManifestParser {

	private SignedDoc m_sdoc;
	private Logger m_logger  =  Logger.getLogger(BdocManifestParser.class);
	
	public BdocManifestParserWithoutCache(SignedDoc sdoc) {
		super(sdoc);
		m_sdoc = sdoc;
	}

	/**
	 * Start Element handler
	 * @param namespaceURI namespace URI
	 * @param lName local name
	 * @param qName qualified name
	 * @param attrs attributes
	 */
	public void startElement(String namespaceURI, String lName, String qName, Attributes attrs)
		throws SAXDigiDocException
	{
		if(m_logger.isDebugEnabled())
			m_logger.debug("Start Element: "	+ qName + " lname: "  + lName + " uri: " + namespaceURI);
		//m_tags.push(qName);
		String tag = qName;
		int p1 = tag.indexOf(':');
		if(p1 > 0)
			tag = qName.substring(p1+1);
		// <manifest>
		if(tag.equals("manifest")) {
			Manifest mf = new Manifest();
			m_sdoc.setManifest(mf);
		}
		// <file-entry>
		if(tag.equals("file-entry")) {
			String sType = null, sPath = null;
			for(int i = 0; i < attrs.getLength(); i++) {
				String key = attrs.getQName(i);
				p1 = key.indexOf(':');
				if(p1 > 0)
					key = key.substring(p1+1);
				if(m_logger.isDebugEnabled())
					m_logger.debug("attr: " + key);
				if(key.equals("media-type")) {
					sType = attrs.getValue(i);
				}
				if(key.equals("full-path")) {
					sPath = attrs.getValue(i);
				}
			}
			ManifestFileEntry fe = new ManifestFileEntry(sType, sPath);
			m_sdoc.getManifest().addFileEntry(fe);
			try {
				if(sPath.equals("/")) { // signed doc entry
					m_sdoc.setMimeType(sType);
					m_sdoc.setFormat(SignedDoc.FORMAT_BDOC);
					if(sType != null && sType.equals(SignedDoc.MIMET_FILE_CONTENT_10))
					  m_sdoc.setVersion(SignedDoc.VERSION_1_0);
					if(sType != null && sType.equals(SignedDoc.MIMET_FILE_CONTENT_11))
					  m_sdoc.setVersion(SignedDoc.VERSION_1_1);
					m_sdoc.setProfile(SignedDoc.BDOC_PROFILE_BES); // default is weakest profile
					if(m_logger.isDebugEnabled())
						m_logger.debug("Sdoc: " + m_sdoc.getFormat() + " / " + m_sdoc.getVersion() + " / " + m_sdoc.getProfile());
				} else if(sPath.indexOf("signature") != -1) { // signature entry
					if(m_logger.isDebugEnabled())
						m_logger.debug("Find sig: " + sPath + " type: " + sType);
					if(sType.startsWith(SAXDigiDocFactory.MIME_SIGNATURE_BDOC) &&
					   !sType.equals(MIME_SIGNATURE_BDOC_BES) &&
					   !sType.equals(MIME_SIGNATURE_BDOC_T) &&
					   !sType.equals(MIME_SIGNATURE_BDOC_CL) &&
					   !sType.equals(MIME_SIGNATURE_BDOC_TM) &&
					   !sType.equals(MIME_SIGNATURE_BDOC_TS) &&
					   !sType.equals(MIME_SIGNATURE_BDOC_TMA) &&
					   !sType.equals(MIME_SIGNATURE_BDOC_TSA) ) {
						DigiDocException dex = new DigiDocException(DigiDocException.ERR_DIGIDOC_FORMAT,
								"Invalid bdoc format: " + sPath, null);
						SAXDigiDocException.handleException(dex); // report invalid signature format
					}
					String sigProfile = m_sdoc.findSignatureProfile(sPath);
					if(sigProfile == null) {
						if(sType.equals(MIME_SIGNATURE_BDOC_BES)) {
							m_sdoc.addSignatureProfile(sPath, SignedDoc.BDOC_PROFILE_BES);
							if(m_sdoc.getProfile() == null || 
								m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_BES)) // weakest profile to be set only if
								m_sdoc.setProfile(SignedDoc.BDOC_PROFILE_BES);
						}
						if(sType.equals(MIME_SIGNATURE_BDOC_T)) {
							m_sdoc.addSignatureProfile(sPath, SignedDoc.BDOC_PROFILE_T);
							if(m_sdoc.getProfile() == null || 
								m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_BES) ||
								m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_T)) 
								m_sdoc.setProfile(SignedDoc.BDOC_PROFILE_T);
						}
						if(sType.equals(MIME_SIGNATURE_BDOC_CL)) {
							m_sdoc.addSignatureProfile(sPath, SignedDoc.BDOC_PROFILE_CL);
							if(m_sdoc.getProfile() == null || 
							   m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_BES) ||
							   m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_T) ||
							   m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_CL)) 
							   m_sdoc.setProfile(SignedDoc.BDOC_PROFILE_CL);
						}
						if(sType.equals(MIME_SIGNATURE_BDOC_TM)) {
							m_sdoc.addSignatureProfile(sPath, SignedDoc.BDOC_PROFILE_TM);
							if(m_sdoc.getProfile() == null || 
							  m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_BES) ||
								m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_T) ||
								m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_CL) ||
								m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_TM)) 
								m_sdoc.setProfile(SignedDoc.BDOC_PROFILE_TM);
						}
						if(sType.equals(MIME_SIGNATURE_BDOC_TS)) {
							m_sdoc.addSignatureProfile(sPath, SignedDoc.BDOC_PROFILE_TS);
							if(m_sdoc.getProfile() == null || 
							   m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_BES) ||
								m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_T) ||
								m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_CL) ||
								m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_TS)) 
								m_sdoc.setProfile(SignedDoc.BDOC_PROFILE_TS);
						}
						if(sType.equals(MIME_SIGNATURE_BDOC_TMA)) {
							m_sdoc.addSignatureProfile(sPath, SignedDoc.BDOC_PROFILE_TMA);
							if(m_sdoc.getProfile() == null || 
							m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_BES) ||
							m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_T) ||
							m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_CL) ||
							m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_TM) ||
							m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_TMA)) 
							m_sdoc.setProfile(SignedDoc.BDOC_PROFILE_TMA);
						}
						if(sType.equals(MIME_SIGNATURE_BDOC_TSA)) {
							m_sdoc.addSignatureProfile(sPath, SignedDoc.BDOC_PROFILE_TSA);
							if(m_sdoc.getProfile() == null || 
							m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_BES) ||
							m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_T) ||
							m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_CL) ||
							m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_TS) ||
							m_sdoc.getProfile().equals(SignedDoc.BDOC_PROFILE_TSA)) 
							m_sdoc.setProfile(SignedDoc.BDOC_PROFILE_TSA);
						}
					}
				} else { // data file entry
				  if(m_logger.isDebugEnabled())
					m_logger.debug("Find df: " + sPath);
				  //System.out.println("Manif entry: " + sPath + " nlen: " + sPath.length());
				  DataFile df = m_sdoc.findDataFileById(sPath);
				  if(df != null) {
					//df.setSize(ze.getSize())
					if(m_logger.isDebugEnabled())
						m_logger.debug("Existing DF: " + df.getId() + " file: " + sPath + " mime: " + sType);
					df.setContentType(DataFile.CONTENT_BINARY);
					df.setFileName(sPath);
					df.setMimeType(sType);
				  } else {
					if(m_logger.isDebugEnabled())
						m_logger.debug("New DF: " + sPath + " file: " + sPath + " mime: " + sType);
					df = new DataFileWithoutCache(sPath, DataFile.CONTENT_BINARY, sPath, sType, m_sdoc);
					m_sdoc.addDataFile(df);
				  }
				}
			} catch(DigiDocException ex) {
				SAXDigiDocException.handleException(ex);
			}
		}
		
	}
}
