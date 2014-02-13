package com.gmail.at.zhuikov.aleksandr.driveddoc;

import java.security.Permission;

import org.junit.rules.ExternalResource;

public class RestrictedFileWritingRule extends ExternalResource {

	@Override
	protected void before() throws Throwable {
		super.before();
		System.setSecurityManager(new SecurityManager() {
			
			@Override
			public void checkWrite(String fd) {
				throw new SecurityException();
			}
			
			@Override
			public void checkPermission(Permission perm) {
				return;
			}
		});
	}
	
	@Override
	protected void after() {
		System.setSecurityManager(null); // or save and restore original
		super.after();
	}
}
