/*
 * $Id: EditProjectCommand.java,v 1.7 2009/02/13 12:08:05 rregan Exp $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
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
package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.command.AnalyzableEditCommand;
import edu.harvard.fas.rregan.requel.project.Project;

/**
 * @author ron
 */
public interface EditProjectCommand extends AnalyzableEditCommand {

	/**
	 * The name of the "name" field used to correlate to the field in an editor
	 * and through exceptions.
	 */
	public static final String FIELD_NAME = "name";

	/**
	 * @param name
	 */
	public void setName(String name);

	/**
	 * @param description
	 */
	public void setText(String description);

	/**
	 * @param organizationName
	 */
	public void setOrganizationName(String organizationName);

	/**
	 * @param project
	 */
	public void setProject(Project project);

	/**
	 * @return
	 */
	public Project getProject();
}
