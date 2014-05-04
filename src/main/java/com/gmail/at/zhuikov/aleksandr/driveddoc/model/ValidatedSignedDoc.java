package com.gmail.at.zhuikov.aleksandr.driveddoc.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.SignedDoc;

public class ValidatedSignedDoc implements Serializable {

	private SignedDoc signedDoc;
	private Collection<DigiDocException> warnings = new ArrayList<>();

	public ValidatedSignedDoc(SignedDoc signedDoc,
			Collection<DigiDocException> warnings) {
		this.signedDoc = signedDoc;
		this.warnings = warnings;
	}
	
	public SignedDoc getSignedDoc() {
		return signedDoc;
	}
	
	public Collection<DigiDocException> getWarnings() {
		return warnings;
	}
}
