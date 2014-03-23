package com.gmail.at.zhuikov.aleksandr.driveddoc.servlet;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gmail.at.zhuikov.aleksandr.driveddoc.service.CachedUserService;
import com.gmail.at.zhuikov.aleksandr.driveddoc.service.UserService;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.JsonFactory;
import com.google.drive.samples.dredit.DrEditServlet;

@Singleton
public class UserServlet extends DrEditServlet {
	
	private static final long serialVersionUID = 1L;
	
	private final UserService userService;

	@Inject
	public UserServlet(JsonFactory jsonFactory, CachedUserService userService) {
		super(jsonFactory);
		this.userService = userService;
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, ServletException {
		try {
			sendJson(resp, userService.getUserInfo(getUserId(req), getCredential()));
		} catch (GoogleJsonResponseException e) {
			if (e.getStatusCode() == 401) {
				// The user has revoked our token or it is otherwise bad.
				// Delete the local copy so that their next page load will
				// recover.
//				deleteCredential(req, resp);
				sendGoogleJsonResponseError(resp, e);
			}
		}
	}
}
