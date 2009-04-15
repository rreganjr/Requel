/*
 * $Id: ImportSemcorFileCommand.java,v 1.1 2008/12/13 00:40:07 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.dictionary.command;

import edu.harvard.fas.rregan.command.Command;
import edu.mit.jsemcor.element.IContext;
import edu.mit.jsemcor.main.IConcordance;

/**
 * Import a specific Semcor data file from the source files. Semcor is a
 * semantically annotated corpus based on the Brown corpus.
 * 
 * @author ron
 */
public interface ImportSemcorFileCommand extends Command {

	/**
	 * @param section -
	 *            the semcor section to load the file from.
	 */
	public void setSection(IConcordance section);

	/**
	 * @param context -
	 *            the context (file) to load.
	 */
	public void setContext(IContext context);
}