package com.gmail.at.zhuikov.aleksandr.driveddoc.model.container;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;


public class FileInContainer implements Serializable  {

	private static final long serialVersionUID = 1L;
	
	private final String title;
	private Collection<ClientApp> apps = new ArrayList<>();
	private String iconLink;
	private final String mimeType;
	
	public FileInContainer(String title, String mimeType) {
		this.title = title;
		this.mimeType = mimeType;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getMimeType() {
		return mimeType;
	}
}
