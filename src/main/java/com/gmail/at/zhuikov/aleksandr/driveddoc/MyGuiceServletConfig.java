package com.gmail.at.zhuikov.aleksandr.driveddoc;

import com.gmail.at.zhuikov.aleksandr.driveddoc.filter.ServerNameFilter;
import com.gmail.at.zhuikov.aleksandr.driveddoc.servlet.AuthorizationCodeCallbackServlet;
import com.gmail.at.zhuikov.aleksandr.driveddoc.servlet.AuthorizationCodeServlet;
import com.gmail.at.zhuikov.aleksandr.driveddoc.servlet.DDocServlet;
import com.gmail.at.zhuikov.aleksandr.driveddoc.servlet.IdSignServlet;
import com.gmail.at.zhuikov.aleksandr.driveddoc.servlet.OCSPSignatureContainerServlet;
import com.gmail.at.zhuikov.aleksandr.driveddoc.servlet.OCSPSignatureContainerUploadURLServlet;
import com.gmail.at.zhuikov.aleksandr.driveddoc.servlet.SignServlet;
import com.gmail.at.zhuikov.aleksandr.driveddoc.servlet.SignatureServlet;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.server.spi.guice.GuiceSystemServiceServletModule;
import com.google.drive.samples.dredit.AboutServlet;
import com.google.drive.samples.dredit.FileServlet;
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
				serve("/api/svc").with(FileServlet.class);
				serve("/api/user").with(UserServlet.class);
				serve("/api/about").with(AboutServlet.class);
				serve("/api/sign").with(SignServlet.class);
				serve("/api/sign/id").with(IdSignServlet.class);
				serve("/api/signatures").with(SignatureServlet.class);
				serve("/api/OCSPSignatureContainer").with(OCSPSignatureContainerServlet.class);
				serve("/api/OCSPSignatureContainerUploadURL").with(OCSPSignatureContainerUploadURLServlet.class);
				serve("/").with(DDocServlet.class);
				serve("/api/oauth2callback").with(AuthorizationCodeCallbackServlet.class);
			}
		}, new GuiceSystemServiceServletModule() {
			
			@Override
			protected void configureServlets() {
				super.configureServlets();

//				Set<Class<?>> serviceClasses = new HashSet<Class<?>>();
//				serviceClasses.add(UserEndpoint.class);
//				this.serveGuiceSystemServiceServlet("/_ah/spi/*", serviceClasses);
			}
		});
	}
}
