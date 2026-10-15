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
package com.rreganjr.requel.project.impl.command;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.XMLConstants;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

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
import com.rreganjr.requel.project.exception.ReportGenerationException;
import com.rreganjr.requel.project.impl.BuiltinReportGenerators;
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
	private final Map<String, String> parameters = new LinkedHashMap<>();
	private String mediaType = "text/html";
	private String fileExtension = ".html";

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
	 * Issue #275: the transform writes into a buffer, and only a complete document reaches the
	 * output stream. Any failure — a transform error, or the template stopping itself with
	 * {@code <xsl:message terminate="yes">} over an unresolved reference — throws a
	 * {@link ReportGenerationException} carrying the template's message, and nothing is written.
	 *
	 * @see com.rreganjr.command.Command#execute()
	 */
	@Override
	public void execute() {
		ReportGenerator reportGenerator = getRepository().get(getReportGenerator());
		File tmpExport = null;
		MessageCollector messages = new MessageCollector();
		try {
			// export the project to a tmp file
			ExportProjectCommand exportCommand = getProjectCommandFactory()
					.newExportProjectCommand();
			exportCommand.setProject((Project) reportGenerator.getProjectOrDomain());
			tmpExport = File.createTempFile("projectExport", ".xml");
			try (OutputStream projectOutputStream = new FileOutputStream(tmpExport)) {
				exportCommand.setOutputStream(projectOutputStream);
				getCommandHandler().execute(exportCommand);
			}

			// create an XMLReader so we can set parsing features on and off
			XMLReader reader = XMLReaderFactory.createXMLReader();
			try {
				reader.setFeature("http://xml.org/sax/features/validation", false);
				reader.setFeature("http://apache.org/xml/features/validation/schema", false);
			} catch (SAXNotSupportedException e) {
				log.warn("The parser does not support XML validation.");
			}
			// #273: the JDK's own XSLTC, not whatever factory the classpath supplies. Xalan 2.7.3 is
			// on the app classpath, and with secure processing on it drops every attribute of a
			// literal result element ("\"href\" attribute is not allowed on the a element"), so
			// the bundled generator's links, anchors and classes never reached the output.
			TransformerFactory tf = TransformerFactory.newDefaultInstance();
			try {
				tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
				tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
				tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
			} catch (Throwable t) {
				// ignore if not supported
			}
			tf.setErrorListener(messages);
			Transformer transformer = tf.newTransformer(new StreamSource(new StringReader(
					templateText(reportGenerator))));
			transformer.setErrorListener(messages);
			for (Map.Entry<String, String> parameter : parameters.entrySet()) {
				transformer.setParameter(parameter.getKey(), parameter.getValue());
			}
			resolveMediaType(transformer);

			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			try (InputStream projectInputStream = new FileInputStream(tmpExport)) {
				transformer.transform(new SAXSource(reader, new InputSource(projectInputStream)),
						new StreamResult(buffer));
			}
			getOutputStream().write(buffer.toByteArray());
			getOutputStream().flush();
		} catch (ReportGenerationException e) {
			throw e;
		} catch (Exception e) {
			String message = messages.describe(e);
			log.warn("Report generator \"" + reportGenerator.getName() + "\" failed: " + message);
			throw new ReportGenerationException("Report \"" + reportGenerator.getName()
					+ "\" failed: " + message, e);
		} finally {
			if (tmpExport != null && !tmpExport.delete()) {
				tmpExport.deleteOnExit();
			}
		}
	}

	/**
	 * @return the bundled template for a keyed generator, else the generator's own text.
	 */
	private static String templateText(ReportGenerator reportGenerator) {
		return BuiltinReportGenerators.forKey(reportGenerator.getBuiltinKey())
				.map(BuiltinReportGenerators.Builtin::text)
				.orElseGet(() -> reportGenerator.getText() == null ? "" : reportGenerator.getText());
	}

	/**
	 * The generator's {@code xsl:output} decides the media type: an explicit media-type wins;
	 * the xml and html methods are rendered as HTML (the bundled HTML generator emits XHTML with
	 * method="xml"); text is plain text.
	 */
	private void resolveMediaType(Transformer transformer) {
		String method = transformer.getOutputProperty(OutputKeys.METHOD);
		String declared = transformer.getOutputProperties().getProperty(OutputKeys.MEDIA_TYPE);
		String type;
		if (declared != null && !declared.isBlank() && !"text/xml".equals(declared)
				&& !"text/plain".equals(declared)) {
			type = declared.trim();
		} else if ("text".equals(method)) {
			type = "text/plain";
		} else {
			type = "text/html";
		}
		mediaType = type;
		fileExtension = switch (type) {
			case "text/markdown", "text/x-markdown" -> ".md";
			case "text/plain" -> ".txt";
			case "application/json" -> ".json";
			case "application/xml" -> ".xml";
			default -> ".html";
		};
	}

	@Override
	public void setParameter(String name, String value) {
		parameters.put(name, value);
	}

	@Override
	public String getMediaType() {
		return mediaType;
	}

	@Override
	public String getFileExtension() {
		return fileExtension;
	}

	/**
	 * Collects the template's {@code xsl:message} text and the processor's errors, so a failure
	 * names what the template said rather than "Termination forced by an xsl:message
	 * instruction". Warnings are kept (XSLTC reports xsl:message through warning); errors and
	 * fatal errors stop the transform.
	 */
	private class MessageCollector implements ErrorListener {
		private final List<String> collected = new ArrayList<>();

		public void warning(TransformerException e) {
			collected.add(e.getMessage());
		}

		public void error(TransformerException e) throws TransformerException {
			collected.add(e.getMessage());
			throw e;
		}

		public void fatalError(TransformerException e) throws TransformerException {
			collected.add(e.getMessage());
			throw e;
		}

		String describe(Exception e) {
			List<String> parts = new ArrayList<>();
			for (String message : collected) {
				if (message != null && !message.isBlank() && !isTerminationNoise(message)
						&& !parts.contains(message.trim())) {
					parts.add(message.trim());
				}
			}
			if (parts.isEmpty()) {
				Throwable cause = e;
				while (cause.getCause() != null && cause.getCause() != cause) {
					cause = cause.getCause();
				}
				parts.add(cause.getMessage() != null ? cause.getMessage()
						: cause.getClass().getSimpleName());
			}
			return String.join("; ", parts);
		}

		private boolean isTerminationNoise(String message) {
			return message.contains("Termination forced by an xsl:message instruction");
		}
	}
}
