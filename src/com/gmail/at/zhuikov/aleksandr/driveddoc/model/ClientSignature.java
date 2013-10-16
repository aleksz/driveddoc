package com.gmail.at.zhuikov.aleksandr.driveddoc.model;

import java.util.Collection;

public class ClientSignature {

	public String signerName;
	public String personalCode;
	public Collection<String> errors;
	
	public ClientSignature(String signerName, String personalCode,
			Collection<String> errors) {
		this.signerName = signerName;
		this.personalCode = personalCode;
		this.errors = errors;
	}
	
	@Override
	public String toString() {
		return signerName + " " + personalCode;
	}
}
