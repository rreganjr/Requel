/*
 * $Id: ReportDownloadProvider.java,v 1.2 2008/12/13 00:41:05 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.ui.project;

import java.io.IOException;
import java.io.OutputStream;

import nextapp.echo2.app.filetransfer.DownloadProvider;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.project.ReportGenerator;
import edu.harvard.fas.rregan.requel.project.command.GenerateReportCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.uiframework.MessageHandler;

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