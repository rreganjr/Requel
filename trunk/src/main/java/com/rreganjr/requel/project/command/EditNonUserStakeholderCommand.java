/*
 * $Id: EditStakeholderCommand.java 124 2009-05-21 23:46:02Z rreganjr $
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

import com.rreganjr.requel.project.NonUserStakeholder;
import com.rreganjr.requel.project.ProjectOrDomain;

/**
 * @author ron
 */
public interface EditNonUserStakeholderCommand extends EditProjectOrDomainEntityCommand {

	/**
	 * Set the project or domain this stakeholder is a part of.
	 * 
	 * @param projectOrDomain
	 */
	public void setProjectOrDomain(ProjectOrDomain projectOrDomain);

	/**
	 * Set the description of the stakeholder.
	 * 
	 * @param text
	 */
	public void setText(String text);

	/**
	 * Set the name.<br>
	 * 
	 * @param name
	 */
	public void setName(String name);

	/**
	 * Used to pass in an existing stakeholder for editing purposes. If no
	 * stakeholder is supplied a new one will be created.
	 * 
	 * @param stakeholder
	 */
	public void setStakeholder(NonUserStakeholder stakeholder);

	/**
	 * Get the stakeholder created or edited via the command. If a stakeholder
	 * was supplied via setStakeholder the returned stakeholder will represent
	 * the same entity although it may not be equal (for example the name has
	 * changed.)
	 * 
	 * @return
	 */
	public NonUserStakeholder getStakeholder();
}
