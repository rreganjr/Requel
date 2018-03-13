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

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This listener detects if the database specified in the db.properties file
 * exists, and if it doesn't it creates it. <br/> NOTE: This listener must be
 * registered before all other listeners that use the database.
 * 
 * @sequence.diagram tomcat:Tomcat[a] /dcl:DatabaseCreationListener[a]
 *                   /dbProperties:File dm:DriverManager[a]
 *                   connection:Connection createDbStmt:Statement tomcat:dcl.new
 *                   tomcat:dcl.contextInitialized() dcl:dbProperties.new
 *                   dcl:dm.getConnection(dbUrl) [c:exception no database]
 *                   dcl:dm.getConnection(dbUrl)
 *                   dcl:connection.createStatement()
 *                   dcl:createDbStmt.execute("create database") [/c]
 * @author ron
 * @deprecated let Spring or flyway do it
 */
@Deprecated
public class DatabaseCreationListener implements ServletContextListener {
	private static final Log log = LogFactory.getLog(DatabaseCreationListener.class);

	public DatabaseCreationListener() {
		super();
	}

	public void contextDestroyed(ServletContextEvent event) {
	}

	public void contextInitialized(ServletContextEvent event) {
		Properties dbProperties = new Properties();
		try {
			File webInfDirectory = new File(event.getServletContext()
					.getRealPath("WEB-INF/classes"));
			File dbPropertiesFile = new File(webInfDirectory, "db.properties");
			FileInputStream fis = new FileInputStream(dbPropertiesFile);
			dbProperties.load(fis);
			fis.close();

			// make sure the driver is loaded
			Class.forName(dbProperties.getProperty("db.driver"));

			// create the full url from the properties
			StringBuilder jdbcUrlBuilder = new StringBuilder();
			jdbcUrlBuilder.append(dbProperties.getProperty("db.baseUrl"));
			jdbcUrlBuilder.append(dbProperties.getProperty("db.server"));
			jdbcUrlBuilder.append(":");
			jdbcUrlBuilder.append(dbProperties.getProperty("db.port"));
			jdbcUrlBuilder.append("/");

			// connection string without database
			String jdbcUrlNoDatabase = jdbcUrlBuilder.toString();

			jdbcUrlBuilder.append(dbProperties.getProperty("db.name"));
			jdbcUrlBuilder.append(dbProperties.getProperty("db.urlParams"));

			String jdbcUrl = jdbcUrlBuilder.toString();
			if (log.isDebugEnabled()) {
				log.debug("jdbc url = " + jdbcUrl);
				listDriverOptions(jdbcUrlNoDatabase);
			}

			try {
				// try to connect to the database to see if it already exists
				DriverManager.getConnection(jdbcUrl, dbProperties.getProperty("db.username"),
						dbProperties.getProperty("db.password"));
			} catch (SQLException se) {
				// TODO: check the exception to see if it really was thrown
				// because the database doesn't exist
				// create the database
				Connection con = DriverManager.getConnection(jdbcUrlNoDatabase, dbProperties
						.getProperty("db.username"), dbProperties.getProperty("db.password"));
				Statement createDbStmt = con.createStatement();
				createDbStmt.execute("create database " + dbProperties.getProperty("db.name"));
			}
		} catch (ClassNotFoundException e) {
			// TODO: throw an exception, the app won't be available
			log.warn("cound not create database '" + dbProperties.getProperty("db.name")
					+ "', the driver class '" + dbProperties.getProperty("db.driver")
					+ "' in db.properties could not be loaded.", e);
		} catch (Exception e) {
			// TODO: throw an exception, the app won't be available
			if (dbProperties.getProperty("db.name") == null) {
				log.warn(
						"could not create database, the properties in db.properties could not be loaded: "
								+ e, e);
			} else {
				log.warn("could not create database '" + dbProperties.getProperty("db.name")
						+ "': " + e, e);
			}
		}
	}

	/**
	 * @param jdbcUrl
	 * see http://exampledepot.com/egs/java.sql/GetPropInfo.html
	 */
	private String listDriverOptions(String jdbcUrl) throws Exception {

		// Get the Driver instance
		Driver driver = DriverManager.getDriver(jdbcUrl);

		// Get available properties
		DriverPropertyInfo[] info = driver.getPropertyInfo(jdbcUrl, null);
		StringBuilder description = new StringBuilder();

		for (int i = 0; i < info.length; i++) {
			description.append(info[i].name);
			description.append(" ");
			description.append(info[i].description);
			description.append(" ");
			description.append(info[i].required);
			description.append(" ");
			description.append(info[i].value);
			description.append(" ");
			description.append(info[i].choices);
			description.append(System.getProperty("line.separator"));
		}
		return description.toString();
	}
}
