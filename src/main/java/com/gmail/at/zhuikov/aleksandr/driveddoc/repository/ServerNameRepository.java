package com.gmail.at.zhuikov.aleksandr.driveddoc.repository;

import javax.inject.Singleton;

@Singleton
public class ServerNameRepository {
	
	private String name;

	public void save(String name) {
		this.name = name;
	}
	
	public String get() {
		return name;
	}
}
