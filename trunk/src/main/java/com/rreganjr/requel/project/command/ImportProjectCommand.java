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
package com.rreganjr.requel.project.command;

import java.io.InputStream;

import com.rreganjr.requel.command.AnalyzableEditCommand;
import com.rreganjr.requel.project.Project;

/**
 * Import the contents of a project to create a new project or to update an
 * existing project from a supplied input stream.
 * 
 * @author ron
 */
public interface ImportProjectCommand extends AnalyzableEditCommand {

	/**
	 * @param project -
	 *            the project to update from the contents of the input stream.
	 *            Leave null to create a new project.
	 */
	public void setProject(Project project);

	/**
	 * @return the created or updated project.
	 */
	public Project getProject();

	/**
	 * @param inputStream -
	 *            the stream to read the project contents from.
	 */
	public void setInputStream(InputStream inputStream);

	/**
	 * @param name -
	 *            over ride the name of the project being imported.
	 */
	public void setName(String name);
}
