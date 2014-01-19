package com.gmail.at.zhuikov.aleksandr.driveddoc.servlet;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.json.JsonFactory;
import com.google.drive.samples.dredit.model.State;

@Singleton
public class DDocServlet extends AuthorizationCodeServlet {

	private static final long serialVersionUID = 1L;

	@Inject
	public DDocServlet(JsonFactory jsonFactory) {
		super(jsonFactory);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		String stateParam = req.getParameter("state");
		
		if (stateParam != null) {

			State state = new State(stateParam);
			
			if (state.ids != null && state.ids.size() > 0) {
				resp.sendRedirect("/#/edit/" + state.ids.toArray()[0]);
				
			} else if (state.folderId != null) {
				resp.sendRedirect("/#/create/" + state.folderId);
			}
		} 
		
		req.getRequestDispatcher("/public/index.html").forward(req, resp);
	}
}
