package com.gmail.at.zhuikov.aleksandr.driveddoc.model;

import java.io.Serializable;

public class User implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private final String email;
	private final String link;
	private final String pictureUrl;
	
	public User(String email, String link, String pictureUrl) {
		this.email = email;
		this.link = link;
		this.pictureUrl = pictureUrl;
	}
	
	public String getPictureUrl() {
		return pictureUrl;
	}
	
	public String getEmail() {
		return email;
	}
	
	public String getLink() {
		return link;
	}
}
