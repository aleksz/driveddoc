package com.gmail.at.zhuikov.aleksandr.driveddoc.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;


public class FileInContainer implements Serializable  {

	private static final long serialVersionUID = 1L;
	
	public String title;
	public Collection<ClientApp> apps = new ArrayList<>();
	public String iconLink;
}
