package com.gmail.at.zhuikov.aleksandr.driveddoc.model;

import java.util.ArrayList;
import java.util.Collection;

public class ClientContainer {

	public String id;
	public String title;
	public Collection<FileInContainer> files = new ArrayList<>();
	public Collection<ClientSignature> signatures = new ArrayList<>();
}
