/*
 * $Id: ExportProjectCommand.java,v 1.2 2008/12/13 00:41:18 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.command;

import java.io.OutputStream;

import edu.harvard.fas.rregan.command.Command;
import edu.harvard.fas.rregan.requel.project.Project;

/**
 * Export the supplied project to the supplied stream.
 * 
 * @author ron
 */
public interface ExportProjectCommand extends Command {

	/**
	 * @param project -
	 *            the project to export
	 */
	public void setProject(Project project);

	/**
	 * @param outputStream -
	 *            the stream to write the output to.
	 */
	public void setOutputStream(OutputStream outputStream);
}
