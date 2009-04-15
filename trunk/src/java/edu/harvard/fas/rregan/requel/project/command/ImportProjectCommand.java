/*
 * $Id: ImportProjectCommand.java,v 1.5 2009/02/13 12:08:06 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.command;

import java.io.InputStream;

import edu.harvard.fas.rregan.requel.command.AnalyzableEditCommand;
import edu.harvard.fas.rregan.requel.project.Project;

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
