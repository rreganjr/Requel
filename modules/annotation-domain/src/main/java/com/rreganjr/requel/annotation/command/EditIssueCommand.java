/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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

import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.IssueSeverity;

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

	/**
	 * Partial-update form (issue #271, following #316's contract): {@code null} means
	 * {@code false} when the command creates an issue and "leave it unchanged" when it updates
	 * one.
	 *
	 * @param mustBeResolved -
	 *            true if this issue must be resolved, or null
	 */
	public void setMustBeResolved(Boolean mustBeResolved);

	/**
	 * {@code null} means the issue kind's default when the command creates an issue, and "leave
	 * it unchanged" when it updates one. A supplied value is also applied to an existing issue the
	 * command reuses because it has the same text (or, for a lexical issue, the same word).
	 * Issue #271.
	 *
	 * @param severity -
	 *            the severity, or null
	 */
	public void setSeverity(IssueSeverity severity);
}
