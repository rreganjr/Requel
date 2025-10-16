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
package com.rreganjr.requel.annotation;

import java.util.Set;

import com.rreganjr.requel.CreatedEntity;
import com.rreganjr.requel.user.User;

/**
 * A position on how to resolve an issue.
 * 
 * @author ron
 */
public interface Position extends Comparable<Position>, CreatedEntity {

	/**
	 * @return All the issues that have this position as a resolution option.
	 */
	public Set<Issue> getIssues();

	/**
	 * @return the text describing how to resolve an issue.
	 */
	public String getText();

	/**
	 * @return The arguments that are for and against this position.
	 */
	public Set<Argument> getArguments();

	/**
	 * Resolve the supplied issue with this position.
	 * 
	 * @param issue -
	 *            the issue to resolve.
	 * @param resolvedByUser -
	 *            the user that initiated the resolution.
	 * @throws Exception -
	 *             TODO: what can be thrown?
	 */
	public void resolveIssue(Issue issue, User resolvedByUser) throws Exception;

	/**
	 * Resolve all issues with this position.
	 * 
	 * @param resolvedByUser
	 * @throws Exception
	 */
	public void resolveAllIssue(User resolvedByUser) throws Exception;
}
