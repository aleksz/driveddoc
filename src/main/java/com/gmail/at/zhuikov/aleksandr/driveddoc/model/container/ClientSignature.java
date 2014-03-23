package com.gmail.at.zhuikov.aleksandr.driveddoc.model.container;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

public class ClientSignature implements Serializable  {

	private static final long serialVersionUID = 1L;
	
	public String signerName;
	public String personalCode;
	public Date date;
	public Collection<String> errors;
	
	public ClientSignature(String signerName, String personalCode, Date date,
			Collection<String> errors) {
		this.signerName = signerName;
		this.personalCode = personalCode;
		this.date = date;
		this.errors = errors;
	}
	
	@Override
	public String toString() {
		return signerName + " " + personalCode;
	}
}
