/*
 * $Id: DocumentationInitializationListener.java,v 1.2 2008/10/20 02:07:51 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
