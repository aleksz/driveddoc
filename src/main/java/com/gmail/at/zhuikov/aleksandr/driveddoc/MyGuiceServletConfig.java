package com.gmail.at.zhuikov.aleksandr.driveddoc;

import com.google.drive.samples.dredit.AboutServlet;
import com.google.drive.samples.dredit.FileServlet;
import com.google.drive.samples.dredit.StartPageServlet;
import com.google.drive.samples.dredit.UserServlet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;

public class MyGuiceServletConfig extends GuiceServletContextListener {

	@Override
	protected Injector getInjector() {
		return Guice.createInjector(new ServletModule() {
			
			@Override
			protected void configureServlets() {
				serve("/svc").with(FileServlet.class);
				serve("/user").with(UserServlet.class);
				serve("/about").with(AboutServlet.class);
				serve("/start", "/").with(StartPageServlet.class);
			}
		});
	}
}
