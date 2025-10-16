/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * 
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
package com.rreganjr.requel.project.command;

import java.util.Set;

import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.UserStakeholder;

/**
 * Create or edit a stakeholder that is a system user.
 * 
 * @author ron
 */
public interface EditUserStakeholderCommand extends EditProjectOrDomainEntityCommand {

	/**
	 * The name of the "user" field used to correlate to the field in an editor
	 * and through exceptions.
	 */
	public static final String FIELD_USER = "user";

	/**
	 * Set the project or domain this stakeholder is a part of.
	 * 
	 * @param projectOrDomain
	 */
	public void setProjectOrDomain(ProjectOrDomain projectOrDomain);

	/**
	 * Set the user that this stakeholder represents in the project. NOTE:
	 * domains should not have user stakeholders, only representational
	 * stakeholders such as a standards board.
	 * 
	 * @param username
	 */
	public void setUsername(String username);

	/**
	 * Supply a role (by name) for this stakeholder. If the supplied name does
	 * not match an existing role for the project a new role will be created.
	 * 
	 * @param roleNames
	 */
	public void setStakeholderPermissions(Set<String> roleNames);

	/**
	 * Set the name of the team this stakeholder is on.
	 * 
	 * @param teamName
	 */
	public void setTeamName(String teamName);

	/**
	 * Used to pass in an existing stakeholder for editing purposes. If no
	 * stakeholder is supplied a new one will be created.
	 * 
	 * @param stakeholder
	 */
	public void setStakeholder(UserStakeholder stakeholder);

	/**
	 * Get the stakeholder created or edited via the command. If a stakeholder
	 * was supplied via setStakeholder the returned stakeholder will represent
	 * the same entity although it may not be equal (for example the user has
	 * changed.)
	 * 
	 * @return
	 */
	public UserStakeholder getStakeholder();
}
