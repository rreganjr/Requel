/*
 * $Id: EditIssueCommand.java,v 1.4 2008/09/04 09:47:19 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.annotation.command;

import edu.harvard.fas.rregan.requel.annotation.Issue;

/**
 * @author ron
 */
public interface EditIssueCommand extends EditAnnotationCommand {

	/**
	 * Set the issue to edit.
	 * 
	 * @param issue
	 */
	public void setIssue(Issue issue);

	/**
	 * Get the new or updated issue.
	 * 
	 * @return
	 */
	public Issue getIssue();

	/**
	 * @param mustBeResolved -
	 *            set to true if this issue must be resolved.
	 */
	public void setMustBeResolved(boolean mustBeResolved);
}
