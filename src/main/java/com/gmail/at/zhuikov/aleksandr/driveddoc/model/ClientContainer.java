package com.gmail.at.zhuikov.aleksandr.driveddoc.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

public class ClientContainer implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public String id;
	public String title;
	public Collection<FileInContainer> files = new ArrayList<>();
	public Collection<ClientSignature> signatures = new ArrayList<>();
}
