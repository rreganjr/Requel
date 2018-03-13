/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
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
package com.rreganjr.requel.project.impl.command;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.xalan.processor.TransformerFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.xml.sax.InputSource;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ReportGenerator;
import com.rreganjr.requel.project.command.ExportProjectCommand;
import com.rreganjr.requel.project.command.GenerateReportCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.UserRepository;

/**
 * Generate a report for a project given the report generator and output stream.
 * 
 * @author ron
 */
@Controller("generateReportCommand")
@Scope("prototype")
public class GenerateReportCommandImpl extends AbstractProjectCommand implements
		GenerateReportCommand {

	private ReportGenerator reportGenerator;
	private OutputStream outputStream;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public GenerateReportCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	/**
	 * @see com.rreganjr.requel.project.command.GenerateReportCommand#setReportGenerator(com.rreganjr.requel.project.ReportGenerator)
	 */
	@Override
	public void setReportGenerator(ReportGenerator reportGenerator) {
		this.reportGenerator = reportGenerator;
	}

	protected ReportGenerator getReportGenerator() {
		return reportGenerator;
	}

	@Override
	public void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}

	protected OutputStream getOutputStream() {
		return outputStream;
	}

	/**
	 * @see com.rreganjr.command.Command#execute()
	 */
	@Override
	public void execute() {
		try {
			ReportGenerator reportGenerator = getRepository().get(getReportGenerator());

			// export the project to a tmp file
			ExportProjectCommand exportCommand = getProjectCommandFactory()
					.newExportProjectCommand();
			exportCommand.setProject((Project) reportGenerator.getProjectOrDomain());
			File tmpUpload = File.createTempFile("projectExport", ".xml");
			OutputStream projectOutputStream = new FileOutputStream(tmpUpload);
			exportCommand.setOutputStream(projectOutputStream);
			exportCommand = getCommandHandler().execute(exportCommand);
			projectOutputStream.close();

			InputStream projectInputStream = new FileInputStream(tmpUpload);
			InputStream xsltInputStream = new ByteArrayInputStream(reportGenerator.getText()
					.getBytes());

			// create an XMLReader so we can set parsing features on and off
			XMLReader reader = XMLReaderFactory.createXMLReader();

			// enable validation if supported
			try {
				reader.setFeature("http://xml.org/sax/features/validation", false);
				reader.setFeature("http://apache.org/xml/features/validation/schema", false);
			} catch (SAXNotSupportedException e) {
				log.warn("The parser does not support XML validation.");
			}
			SAXSource saxSource = new SAXSource(reader, new InputSource(projectInputStream));
			Transformer transformer = new TransformerFactoryImpl().newTransformer(new StreamSource(
					xsltInputStream));
			transformer.setErrorListener(new AnErrorListener());
			transformer.transform(saxSource, new StreamResult(new BufferedWriter(
					new OutputStreamWriter(getOutputStream(), "UTF8"))));
		} catch (Exception e) {
			log.error(e, e);
		}
	}

	/**
	 * Handle processing errors: throw an exception on error and fatalError,
	 * only log warnings.
	 */
	private class AnErrorListener implements ErrorListener {
		public void warning(TransformerException e) throws TransformerException {
			// only log warnings
			log.warn("Parser warning:  " + e.getMessage());
		}

		public void error(TransformerException e) throws TransformerException {
			log.error("Parsing error:  " + e.getMessage());
			throw e;
		}

		public void fatalError(TransformerException e) throws TransformerException {
			log.error("Fatal parsing error:  " + e.getMessage());
			throw e;
		}
	}
}
