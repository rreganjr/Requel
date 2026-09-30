/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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

import java.io.OutputStream;

import com.rreganjr.command.Command;
import com.rreganjr.requel.project.ReportGenerator;

/**
 * Render the supplied report generator over its project to the supplied stream. Nothing is
 * written to the stream when generation fails (issue #275); a ReportGenerationException is thrown.
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

	/**
	 * Issue #275: a stylesheet parameter ({@code <xsl:param name="...">}), e.g. projectVersion.
	 */
	public void setParameter(String name, String value);

	/**
	 * @return after execute, the media type of the generated document, read from the generator's
	 *         {@code xsl:output} (text/html for the xml and html methods).
	 */
	public String getMediaType();

	/**
	 * @return after execute, the file extension for the generated document, including the dot.
	 */
	public String getFileExtension();
}
