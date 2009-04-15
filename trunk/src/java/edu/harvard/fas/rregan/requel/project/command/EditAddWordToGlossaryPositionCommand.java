/*
 * $Id: EditAddWordToGlossaryPositionCommand.java,v 1.1 2008/08/13 10:16:55 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.annotation.command.EditPositionCommand;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;

/**
 * Create or edit a position on a LexicalIssue for adding a word to the project
 * glossary.
 * 
 * @author ron
 */
public interface EditAddWordToGlossaryPositionCommand extends EditPositionCommand {
	/**
	 * @param projectOrDomain -
	 *            the project or domain to add the term to.
	 */
	public void setProjectOrDomain(ProjectOrDomain projectOrDomain);
}
