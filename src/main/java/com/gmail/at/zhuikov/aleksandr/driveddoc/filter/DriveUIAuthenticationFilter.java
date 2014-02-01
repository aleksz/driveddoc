package com.gmail.at.zhuikov.aleksandr.driveddoc.filter;

import java.io.IOException;

import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.drive.samples.dredit.model.State;
import com.google.gson.Gson;

@Singleton
public class DriveUIAuthenticationFilter implements Filter {

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {
		
		HttpServletResponse httpRespponse = (HttpServletResponse) resp;
		HttpServletRequest httpRequest  = (HttpServletRequest) req;
		String stateParam = req.getParameter("state");
		
		if (stateParam == null) {
			httpRespponse.sendError(400, "missing 'state' param");
			return;
		}
		
		State state = new Gson().fromJson(stateParam, State.class);
		
		if (state.userId == null) {
			httpRespponse.sendError(401);
			return;
		}
		
		httpRequest.getSession().setAttribute("me", state.userId);
		
		chain.doFilter(req, resp);
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}
}
