/*
 * $Id: DeleteIssueCommand.java,v 1.1 2009/02/13 12:08:00 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.annotation.command;

import edu.harvard.fas.rregan.requel.annotation.Issue;
import edu.harvard.fas.rregan.requel.command.EditCommand;

/**
 * Delete an issue, its positions and all the arguments of the positions. Update
 * all the annotatables that refer to the issue and remove the reference.
 * 
 * @author ron
 */
public interface DeleteIssueCommand extends EditCommand {

	/**
	 * Set the issue to delete.
	 * 
	 * @param issue
	 */
	public void setIssue(Issue issue);
}
