/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
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
package com.rreganjr.requel.annotation.command;

import com.rreganjr.command.Command;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.user.User;

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
