/*
 * $Id: EditProjectOrDomainEntityCommand.java,v 1.8 2009/02/13 12:08:05 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.command.AnalyzableEditCommand;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;

/**
 * @author ron
 */
public interface EditProjectOrDomainEntityCommand extends AnalyzableEditCommand {

	/**
	 * The name of the "name" field used to correlate to the field in an editor
	 * and through exceptions.
	 */
	public static final String FIELD_NAME = "name";

	/**
	 * Set the project or domain this entity is a part of.
	 * 
	 * @param projectOrDomain
	 */
	public void setProjectOrDomain(ProjectOrDomain projectOrDomain);

	/**
	 * @param name
	 */
	public void setName(String name);
}
