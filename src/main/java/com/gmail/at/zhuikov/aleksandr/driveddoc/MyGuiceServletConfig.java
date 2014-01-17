package com.gmail.at.zhuikov.aleksandr.driveddoc;

import com.gmail.at.zhuikov.aleksandr.driveddoc.filter.ServerNameFilter;
import com.gmail.at.zhuikov.aleksandr.driveddoc.servlet.IdSignServlet;
import com.gmail.at.zhuikov.aleksandr.driveddoc.servlet.OCSPSignatureContainerServlet;
import com.gmail.at.zhuikov.aleksandr.driveddoc.servlet.OCSPSignatureContainerUploadURLServlet;
import com.gmail.at.zhuikov.aleksandr.driveddoc.servlet.SignServlet;
import com.gmail.at.zhuikov.aleksandr.driveddoc.servlet.SignatureServlet;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.drive.samples.dredit.AboutServlet;
import com.google.drive.samples.dredit.FileServlet;
import com.google.drive.samples.dredit.StartPageServlet;
import com.google.drive.samples.dredit.UserServlet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;

public class MyGuiceServletConfig extends GuiceServletContextListener {

	@Override
	protected Injector getInjector() {
		return Guice.createInjector(new ServletModule() {
			
			@Provides
			public HttpTransport httpTransport() {
				return new NetHttpTransport();
			}
			
			@Provides
			public JsonFactory jsonFactory() {
				return GsonFactory.getDefaultInstance();
			}
			
			@Override
			protected void configureServlets() {
				filter("/*").through(ServerNameFilter.class);
				serve("/svc").with(FileServlet.class);
				serve("/user").with(UserServlet.class);
				serve("/about").with(AboutServlet.class);
				serve("/sign").with(SignServlet.class);
				serve("/sign/id").with(IdSignServlet.class);
				serve("/signatures").with(SignatureServlet.class);
				serve("/OCSPSignatureContainer").with(OCSPSignatureContainerServlet.class);
				serve("/OCSPSignatureContainerUploadURL").with(OCSPSignatureContainerUploadURLServlet.class);
				serve("/start", "/").with(StartPageServlet.class);
			}
		});
	}
}
