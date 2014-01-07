package com.gmail.at.zhuikov.aleksandr.driveddoc.filter;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.gmail.at.zhuikov.aleksandr.driveddoc.repository.ServerNameRepository;

@Singleton
public class ServerNameFilter implements Filter {

	private ServerNameRepository domainRepository;

	@Inject
	public ServerNameFilter(ServerNameRepository domainRepository) {
		this.domainRepository = domainRepository;
	}
	
	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		domainRepository.save("https://" + request.getServerName() + ":" + request.getServerPort());
		chain.doFilter(request, response);
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}
}
