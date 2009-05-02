/*
 * $Id: DocumentationInitializationListener.java,v 1.2 2008/10/20 02:07:51 rregan Exp $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
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
package edu.harvard.fas.rregan.requel;

import java.io.File;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.requel.utils.Untar;

/**
 * This listener detects if the documentation archives exist and if they have
 * been extracted or not. If not they are extracted.
 * 
 * @author ron
 */
public class DocumentationInitializationListener implements ServletContextListener {
	private static final Logger log = Logger.getLogger(DatabaseCreationListener.class);

	/**
	 * 
	 */
	public DocumentationInitializationListener() {
		super();
	}

	public void contextDestroyed(ServletContextEvent event) {
	}

	public void contextInitialized(ServletContextEvent event) {
		File docDirectory = null;
		File docArchiveFile = null;
		File docHtmlDirectory = null;
		try {
			docDirectory = new File(event.getServletContext().getRealPath("doc"));
			docArchiveFile = new File(docDirectory, "devdocs.tgz");
			docHtmlDirectory = new File(docDirectory, "html");
			// if the doc/html directory exists assume that the archive was
			// already extracted.
			if (!docHtmlDirectory.exists() && docArchiveFile.exists()) {
				log.info("extracting developer documentation from " + docArchiveFile);
				Untar.untar(docArchiveFile, docDirectory);
			}
		} catch (Exception e) {
			if ((docDirectory != null) && (docArchiveFile != null)) {
				log.warn("could not extract the documentation archive "
						+ docArchiveFile.getAbsolutePath() + " to "
						+ docDirectory.getAbsolutePath() + ": " + e, e);
			} else {
				log.warn("could not extract the documentation archive: " + e, e);
			}
		}
	}
}
