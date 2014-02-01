package com.gmail.at.zhuikov.aleksandr.driveddoc.servlet;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.json.JsonFactory;
import com.google.drive.samples.dredit.model.State;
import com.google.gson.Gson;

@Singleton
public class DriveUIIntegrationServlet extends DriveUIAuthorizationServlet {

	private static final long serialVersionUID = 1L;

	@Inject
	public DriveUIIntegrationServlet(JsonFactory jsonFactory) {
		super(jsonFactory);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		State state = new Gson().fromJson( req.getParameter("state"), State.class);
		
		if (state.ids != null && state.ids.size() > 0) {
			resp.sendRedirect("/#/edit/" + state.ids.toArray()[0]);
			
		} else if (state.folderId != null) {
			resp.sendRedirect("/#/create/" + state.folderId);
		}
	}
}