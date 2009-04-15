/*
 * $Id: EditProjectCommand.java,v 1.7 2009/02/13 12:08:05 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
