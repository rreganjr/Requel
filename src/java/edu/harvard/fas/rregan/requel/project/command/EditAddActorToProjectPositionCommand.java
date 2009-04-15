/*
 * $Id: EditAddActorToProjectPositionCommand.java,v 1.1 2008/09/19 21:56:41 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.annotation.command.EditPositionCommand;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;

/**
 * Create or edit a position on a LexicalIssue for adding an actor to the
 * project.
 * 
 * @author ron
 */
public interface EditAddActorToProjectPositionCommand extends EditPositionCommand {
	/**
	 * @param projectOrDomain -
	 *            the project or domain to add the actor to.
	 */
	public void setProjectOrDomain(ProjectOrDomain projectOrDomain);
}
