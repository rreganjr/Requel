/*
 * $Id: ImportSemcorCommand.java,v 1.1 2008/12/13 00:40:12 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.dictionary.command;

import java.net.URL;

import edu.harvard.fas.rregan.command.Command;

/**
 * Import the Semcor data from the source files. Semcor is a semantically
 * annotated corpus based on the Brown corpus.
 * 
 * @author ron
 */
public interface ImportSemcorCommand extends Command {

	/**
	 * @param baseURL -
	 *            the url to the root semcor directory. This directory should
	 *            contain the
	 */
	public void setBaseURL(URL baseURL);

}