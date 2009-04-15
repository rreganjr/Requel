/*
 * $Id: DatabaseInitializationListener.java,v 1.9 2008/12/13 00:41:40 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import edu.harvard.fas.rregan.repository.DatabaseInitializer;

/**
 * This listener checks if the database has been initialized with the base data,
 * and if not it inizializes it. <br/> NOTE: This listener must be registered
 * after the Spring context config.
 * 
 * @author ron
 */
public class DatabaseInitializationListener implements ServletContextListener {

	public void contextDestroyed(ServletContextEvent event) {
	}

	public void contextInitialized(ServletContextEvent event) {
		WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(event
				.getServletContext());
		DatabaseInitializer initializer = (DatabaseInitializer) ctx.getAutowireCapableBeanFactory().getBean("databaseInitializer");
		initializer.initialize();
	}
}
