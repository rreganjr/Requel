/*
 * $Id: DeleteGlossaryTermCommand.java,v 1.1 2008/11/20 09:55:13 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.command.EditCommand;
import edu.harvard.fas.rregan.requel.project.GlossaryTerm;

/**
 * @author ron
 */
public interface DeleteGlossaryTermCommand extends EditCommand {

	/**
	 * Set the glossaryTerm to delete.
	 * 
	 * @param glossaryTerm
	 */
	public void setGlossaryTerm(GlossaryTerm glossaryTerm);
}
