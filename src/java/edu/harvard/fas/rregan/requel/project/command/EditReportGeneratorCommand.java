/*
 * $Id: EditReportGeneratorCommand.java,v 1.3 2009/01/27 09:30:17 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.project.ReportGenerator;

/**
 * @author ron
 */
public interface EditReportGeneratorCommand extends EditTextEntityCommand {

	/**
	 * Set the ReportGenerator to edit.
	 * 
	 * @param ReportGenerator
	 */
	public void setReportGenerator(ReportGenerator ReportGenerator);

	/**
	 * Get the new or updated ReportGenerator.
	 * 
	 * @return
	 */
	public ReportGenerator getReportGenerator();
}
