package com.gmail.at.zhuikov.aleksandr.driveddoc.model;

import java.io.Serializable;

public class User implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private final String email;
	private final String link;
	
	public User(String email, String link) {
		this.email = email;
		this.link = link;
	}
	
	public String getEmail() {
		return email;
	}
	
	public String getLink() {
		return link;
	}
}
