package com.gmail.at.zhuikov.aleksandr.digidocserviceadapter;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DigiDocServiceServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	private final DigiDocServiceAccessor digiDocServiceAccessor = new DigiDocServiceAccessor();
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.getOutputStream().print(digiDocServiceAccessor.pullUrl(req.getInputStream(), req.getContentLength()));
	}
}
