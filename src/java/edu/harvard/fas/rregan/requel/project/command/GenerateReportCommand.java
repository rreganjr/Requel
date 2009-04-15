/*
 * $Id: GenerateReportCommand.java,v 1.2 2008/12/13 00:41:18 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.command;

import java.io.OutputStream;

import edu.harvard.fas.rregan.command.Command;
import edu.harvard.fas.rregan.requel.project.ReportGenerator;

/**
 * Export the supplied project to the supplied stream.
 * 
 * @author ron
 */
public interface GenerateReportCommand extends Command {

	/**
	 * @param reportGenerator -

	 */
	public void setReportGenerator(ReportGenerator reportGenerator);

	/**
	 * @param outputStream -
	 *            the stream to write the generated report to.
	 */
	public void setOutputStream(OutputStream outputStream);
}
