/*
 * $Id: EditReportGeneratorCommand.java,v 1.3 2009/01/27 09:30:17 rregan Exp $
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
