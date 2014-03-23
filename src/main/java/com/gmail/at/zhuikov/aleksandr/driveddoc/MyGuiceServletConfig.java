package com.gmail.at.zhuikov.aleksandr.driveddoc;

import com.gmail.at.zhuikov.aleksandr.driveddoc.filter.AuthenticationFilter;
import com.gmail.at.zhuikov.aleksandr.driveddoc.filter.DriveUIAuthenticationFilter;
import com.gmail.at.zhuikov.aleksandr.driveddoc.filter.LegacyDriveUIIntegrationFilter;
import com.gmail.at.zhuikov.aleksandr.driveddoc.filter.ServerNameFilter;
import com.gmail.at.zhuikov.aleksandr.driveddoc.servlet.AuthorizationCallbackServlet;
import com.gmail.at.zhuikov.aleksandr.driveddoc.servlet.DriveDDocServlet;
import com.gmail.at.zhuikov.aleksandr.driveddoc.servlet.DriveUIIntegrationServlet;
import com.gmail.at.zhuikov.aleksandr.driveddoc.servlet.IdSignServlet;
import com.gmail.at.zhuikov.aleksandr.driveddoc.servlet.OCSPSignatureContainerServlet;
import com.gmail.at.zhuikov.aleksandr.driveddoc.servlet.OCSPSignatureContainerUploadURLServlet;
import com.gmail.at.zhuikov.aleksandr.driveddoc.servlet.SignServlet;
import com.gmail.at.zhuikov.aleksandr.driveddoc.servlet.SignatureServlet;
import com.gmail.at.zhuikov.aleksandr.driveddoc.servlet.UserServlet;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.drive.samples.dredit.AboutServlet;
import com.google.drive.samples.dredit.FileServlet;
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
				filter("/").through(LegacyDriveUIIntegrationFilter.class);
				filter("/driveui").through(DriveUIAuthenticationFilter.class);
				filter("/*").through(AuthenticationFilter.class);
				serve("/api/svc").with(FileServlet.class);
				serve("/api/user").with(UserServlet.class);
				serve("/api/about").with(AboutServlet.class);
				serve("/api/sign").with(SignServlet.class);
				serve("/api/sign/id").with(IdSignServlet.class);
				serve("/api/signatures").with(SignatureServlet.class);
				serve("/api/OCSPSignatureContainer").with(OCSPSignatureContainerServlet.class);
				serve("/api/OCSPSignatureContainerUploadURL").with(OCSPSignatureContainerUploadURLServlet.class);
				serve("/driveui").with(DriveUIIntegrationServlet.class);
				serve("/").with(DriveDDocServlet.class);
				serve("/api/oauth2callback").with(AuthorizationCallbackServlet.class);
			}
		});
	}
}
