/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.rreganjr.requel;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import com.rreganjr.repository.DatabaseInitializer;

/**
 * This listener checks if the database has been initialized with the base data,
 * and if not it initializes it. <br/> NOTE: This listener must be registered
 * after the Spring context config.
 * 
 * @author ron
 */
@WebListener
public class DatabaseInitializationListener implements ServletContextListener {

	public void contextDestroyed(ServletContextEvent event) {
	}

	public void contextInitialized(ServletContextEvent event) {
		WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(event.getServletContext());
		DatabaseInitializer initializer = (DatabaseInitializer) ctx.getAutowireCapableBeanFactory().getBean("databaseInitializer");

		initializer.initialize();
	}
}
