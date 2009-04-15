/*
 * $Id: ResolveIssueCommand.java,v 1.7 2009/02/13 12:08:01 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.annotation.command;

import edu.harvard.fas.rregan.command.Command;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.Issue;
import edu.harvard.fas.rregan.requel.annotation.Position;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * Resolve the supplied issue with the supplied position.
 * 
 * @author ron
 */
public interface ResolveIssueCommand extends Command {

	/**
	 * @return The annotatable to resolve the issue with
	 */
	public Annotatable getAnnotatable();

	/**
	 * The annotatable to resolve the issue with
	 * 
	 * @param annotatable
	 */
	public void setAnnotatable(Annotatable annotatable);

	/**
	 * @return the resolved issue.
	 */
	public Issue getIssue();

	/**
	 * Set the issue to resolve with the position.
	 * 
	 * @param issue
	 */
	public void setIssue(Issue issue);

	/**
	 * @param position -
	 *            the position that resolves the issue.
	 */
	public void setPosition(Position position);

	/**
	 * @param user -
	 *            the user resolving the issue.
	 */
	public void setEditedBy(User user);
}
