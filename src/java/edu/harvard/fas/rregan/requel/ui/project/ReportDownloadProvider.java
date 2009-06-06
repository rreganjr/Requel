/*
 * $Id$
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
package edu.harvard.fas.rregan.requel.ui.project;

import java.io.IOException;
import java.io.OutputStream;

import nextapp.echo2.app.filetransfer.DownloadProvider;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.project.ReportGenerator;
import edu.harvard.fas.rregan.requel.project.command.GenerateReportCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import net.sf.echopm.MessageHandler;

/**
 * Execute a report generator and return the report in the supplied output
 * stream.
 * 
 * @author ron
 */
class ReportDownloadProvider implements DownloadProvider {
	private final ProjectCommandFactory projectCommandFactory;
	private final CommandHandler commandHandler;
	private final ReportGenerator reportGenerator;
	private final MessageHandler messageHandler;

	ReportDownloadProvider(MessageHandler messageHandler,
			ProjectCommandFactory projectCommandFactory, CommandHandler commandHandler,
			ReportGenerator reportGenerator) {
		this.projectCommandFactory = projectCommandFactory;
		this.commandHandler = commandHandler;
		this.reportGenerator = reportGenerator;
		this.messageHandler = messageHandler;
	}

	public String getContentType() {
		return "text/html";
	}

	public String getFileName() {
		return reportGenerator.getProjectOrDomain().getName() + ".html";
	}

	public int getSize() {
		return 0;
	}

	public void writeFile(OutputStream outputStream) throws IOException {
		GenerateReportCommand generateReportCommand = projectCommandFactory
				.newGenerateReportCommand();
		generateReportCommand.setReportGenerator(reportGenerator);
		generateReportCommand.setOutputStream(outputStream);
		try {
			generateReportCommand = commandHandler.execute(generateReportCommand);
		} catch (Exception e) {
			messageHandler.setGeneralMessage("Report generation failed: " + e);
		}
	}
}