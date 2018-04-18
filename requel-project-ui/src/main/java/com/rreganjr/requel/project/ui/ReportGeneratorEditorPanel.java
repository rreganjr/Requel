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
package com.rreganjr.requel.project.ui;

import java.text.MessageFormat;

import net.sf.echopm.panel.Panel;
import net.sf.echopm.panel.editor.AbstractEditorPanel;
import nextapp.echo2.app.Button;
import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.filetransfer.UploadEvent;
import nextapp.echo2.app.filetransfer.UploadListener;
import nextapp.echo2.app.filetransfer.UploadSelect;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.validator.InvalidStateException;
import org.hibernate.validator.InvalidValue;

import echopointng.text.StringDocumentEx;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.EntityException;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.project.ReportGenerator;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.command.DeleteReportGeneratorCommand;
import com.rreganjr.requel.project.command.EditReportGeneratorCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import net.sf.echopm.navigation.DownloadButton;
import net.sf.echopm.navigation.event.DeletedEntityEvent;
import net.sf.echopm.navigation.event.UpdateEntityEvent;

/**
 * @author ron
 */
public class ReportGeneratorEditorPanel extends AbstractRequelProjectEditorPanel {
	private static final Log log = LogFactory.getLog(ReportGeneratorEditorPanel.class);

	static final long serialVersionUID = 0L;

	/**
	 * The name to use in the ReportGeneratorEditorPanel.properties file to set
	 * the label of the name field. If the property is undefined "Name" is used.
	 */
	public static final String PROP_LABEL_NAME = "Name.Label";

	/**
	 * The name to use in the ReportGeneratorEditorPanel.properties file to set
	 * the label of the text field. If the property is undefined "Text" is used.
	 */
	public static final String PROP_LABEL_TEXT = "Text.Label";

	/**
	 * The name to use in the ReportGeneratorEditorPanel.properties file to set
	 * the label of the run report button. If the property is undefined "Run" is
	 * used.
	 */
	public static final String PROP_LABEL_RUN_BUTTON = "RunButton.Label";

	/**
	 * The name to use in the ReportGeneratorEditorPanel.properties file to set
	 * the label of the upload button. If the property is undefined "Upload" is
	 * used.
	 */
	public static final String PROP_LABEL_UPLOAD_BUTTON = "UploadButton.Label";

	private UpdateListener updateListener;
	private Button runButton;

	// this is set by the DeleteListener so that the UpdateListener can ignore
	// events between when the object was deleted and the panel goes away.
	private boolean deleted;

	/**
	 * @param commandHandler
	 * @param projectCommandFactory
	 * @param projectRepository
	 */
	public ReportGeneratorEditorPanel(CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, ProjectRepository projectRepository) {
		this(ReportGeneratorEditorPanel.class.getName(), commandHandler, projectCommandFactory,
				projectRepository);
	}

	/**
	 * @param resourceBundleName
	 * @param commandHandler
	 * @param projectCommandFactory
	 * @param projectRepository
	 */
	public ReportGeneratorEditorPanel(String resourceBundleName, CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, ProjectRepository projectRepository) {
		super(resourceBundleName, ReportGenerator.class, commandHandler, projectCommandFactory,
				projectRepository);
	}

	/**
	 * If the editor is editing an existing ReportGenerator the title specified
	 * in the properties file as PROP_EXISTING_OBJECT_PANEL_TITLE If that
	 * property is not set it then tries the standard PROP_PANEL_TITLE and if
	 * that does not exist it defaults to:<br>
	 * "ReportGenerator: {0}"<br>
	 * Valid variables are:<br>
	 * {0} - ReportGenerator name<br>
	 * {1} - project/domain name<br>
	 * For new ReportGenerator it first tries PROP_NEW_OBJECT_PANEL_TITLE, then
	 * PROP_PANEL_TITLE and finally defaults to:<br>
	 * "New Report"<br>
	 * 
	 * @see net.sf.echopm.panel.editor.AbstractEditorPanel#PROP_EXISTING_OBJECT_PANEL_TITLE
	 * @see net.sf.echopm.panel.editor.AbstractEditorPanel#PROP_NEW_OBJECT_PANEL_TITLE
	 * @see net.sf.echopm.panel.Panel#PROP_PANEL_TITLE
	 * @see net.sf.echopm.panel.AbstractPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		if (getReportGenerator() != null) {
			String msgPattern = getResourceBundleHelper(getLocale()).getString(
					AbstractEditorPanel.PROP_EXISTING_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(Panel.PROP_PANEL_TITLE,
							"Document: {0}"));
			return MessageFormat.format(msgPattern, getReportGenerator().getName(),
					getProjectOrDomain().getName());
		} else {
			String msg = getResourceBundleHelper(getLocale()).getString(
					AbstractEditorPanel.PROP_NEW_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale())
							.getString(Panel.PROP_PANEL_TITLE, "New Document"));
			return msg;
		}
	}

	@Override
	public void setup() {
		super.setup();
		ReportGenerator reportGenerator = getReportGenerator();
		if (reportGenerator != null) {
			addInput(EditReportGeneratorCommand.FIELD_NAME, PROP_LABEL_NAME, "Name",
					new TextField(), new StringDocumentEx(reportGenerator.getName()));

			try {
				UploadSelect uploadXslt = addInput("uploadXslt", PROP_LABEL_UPLOAD_BUTTON,
						"Upload", new UploadSelect(), null);
				uploadXslt.addUploadListener(new XsltUploadListener(this));
				uploadXslt.setEnabledSendButtonText(getResourceBundleHelper(getLocale()).getString(
						PROP_LABEL_UPLOAD_BUTTON, "Upload"));
			} catch (Exception e) {
				log.error(e, e);
				setGeneralMessage("Problem initializing the upload button: " + e);
			}

			addInput(EditReportGeneratorCommand.FIELD_TEXT, PROP_LABEL_TEXT, "Text",
					new TextArea(), new StringDocumentEx(reportGenerator.getText()));
			runButton = addActionButton(new DownloadButton(getResourceBundleHelper(getLocale())
					.getString(PROP_LABEL_RUN_BUTTON, "Run"), new ReportDownloadProvider(this,
					getProjectCommandFactory(), getCommandHandler(), getReportGenerator())));
			runButton.setEnabled(true);
		} else {
			addInput("name", PROP_LABEL_NAME, "Name", new TextField(), new StringDocumentEx());

			try {
				UploadSelect uploadXslt = addInput("uploadXslt", PROP_LABEL_UPLOAD_BUTTON,
						"Upload", new UploadSelect(), null);
				uploadXslt.addUploadListener(new XsltUploadListener(this));
				uploadXslt.setEnabledSendButtonText(getResourceBundleHelper(getLocale()).getString(
						PROP_LABEL_UPLOAD_BUTTON, "Upload"));
			} catch (Exception e) {
				log.error(e, e);
				setGeneralMessage("Problem initializing the upload button: " + e);
			}
			addInput(EditReportGeneratorCommand.FIELD_TEXT, PROP_LABEL_TEXT, "Text",
					new TextArea(), new StringDocumentEx());
			runButton = addActionButton(new DownloadButton(getResourceBundleHelper(getLocale())
					.getString(PROP_LABEL_RUN_BUTTON, "Run"), new ReportDownloadProvider(this,
					getProjectCommandFactory(), getCommandHandler(), getReportGenerator())));
			runButton.setEnabled(false);
		}

		if (updateListener != null) {
			getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
					updateListener);
		}
		updateListener = new UpdateListener(this);
		getEventDispatcher().addEventTypeActionListener(UpdateEntityEvent.class, updateListener);
	}

	@Override
	public void dispose() {
		super.dispose();
		removeAll();
		if (updateListener != null) {
			getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
					updateListener);
			updateListener = null;
		}
	}

	@Override
	public void cancel() {
		super.cancel();
		if (updateListener != null) {
			getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
					updateListener);
		}
	}

	@Override
	public void save() {
		try {
			super.save();
			EditReportGeneratorCommand command = getProjectCommandFactory()
					.newEditReportGeneratorCommand();
			command.setReportGenerator(getReportGenerator());
			command.setName(getInputValue(EditReportGeneratorCommand.FIELD_NAME, String.class));
			command.setText(getInputValue(EditReportGeneratorCommand.FIELD_TEXT, String.class));
			command.setProjectOrDomain(getProjectOrDomain());
			command.setEditedBy(getCurrentUser());
			command = getCommandHandler().execute(command);
			setValid(true);
			if (updateListener != null) {
				getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
						updateListener);
				// TODO: remove other listeners?
			}
			getEventDispatcher().dispatchEvent(
					new UpdateEntityEvent(this, command.getReportGenerator()));
		} catch (EntityException e) {
			if (e.isStaleEntity()) {
				// TODO: compare the original values before the user edited
				// to the current revisions values and if they are the same
				// then update the new revision with the user's changes and
				// continue, otherwise show the new changed value vs. the users
				// new values.
				String newName = getInputValue(EditReportGeneratorCommand.FIELD_NAME, String.class);
				String newText = getInputValue(EditReportGeneratorCommand.FIELD_TEXT, String.class);
				ReportGenerator newReportGenerator = getProjectRepository().get(
						getReportGenerator());

				setTargetObject(newReportGenerator);
				if (!newName.equals(newReportGenerator.getName())
						|| !newText.equals(newReportGenerator.getText())) {
					setGeneralMessage("The report was changed by another user and the value conflicts with your input.");
					if (!newName.equals(newReportGenerator.getName())) {
						setValidationMessage(EditReportGeneratorCommand.FIELD_NAME, "Your input '"
								+ newName + "'");
						setInputValue(EditReportGeneratorCommand.FIELD_NAME, newReportGenerator
								.getName());
					}
					if (!newText.equals(newReportGenerator.getText())) {
						setValidationMessage(EditReportGeneratorCommand.FIELD_TEXT, "Your input '"
								+ newText + "'");
						setInputValue(EditReportGeneratorCommand.FIELD_TEXT, newReportGenerator
								.getText());
					}
				} else {
					getEventDispatcher().dispatchEvent(
							new UpdateEntityEvent(this, newReportGenerator));
				}

			} else if ((e.getEntityPropertyNames() != null)
					&& (e.getEntityPropertyNames().length > 0)) {
				for (String propertyName : e.getEntityPropertyNames()) {
					setValidationMessage(propertyName, e.getMessage());
				}
			} else if ((e.getCause() != null) && (e.getCause() instanceof InvalidStateException)) {
				InvalidStateException ise = (InvalidStateException) e.getCause();
				for (InvalidValue invalidValue : ise.getInvalidValues()) {
					String propertyName = invalidValue.getPropertyName();
					setValidationMessage(propertyName, invalidValue.getMessage());
				}
			} else {
				setGeneralMessage(e.toString());
			}

		} catch (Exception e) {
			log.error("could not save the reportGenerator: " + e, e);
			setGeneralMessage("Could not save: " + e);
		}
	}

	@Override
	public void delete() {
		try {
			DeleteReportGeneratorCommand deleteReportGeneratorCommand = getProjectCommandFactory()
					.newDeleteReportGeneratorCommand();
			deleteReportGeneratorCommand.setEditedBy(getCurrentUser());
			deleteReportGeneratorCommand.setReportGenerator(getReportGenerator());
			deleteReportGeneratorCommand = getCommandHandler()
					.execute(deleteReportGeneratorCommand);
			deleted = true;
			getEventDispatcher().dispatchEvent(new DeletedEntityEvent(this, getReportGenerator()));
		} catch (Exception e) {
			setGeneralMessage("Could not delete entity: " + e);
		}
	}

	private ProjectOrDomain getProjectOrDomain() {
		if (getTargetObject() instanceof ProjectOrDomain) {
			return (ProjectOrDomain) getTargetObject();
		} else if (getTargetObject() instanceof ProjectOrDomainEntity) {
			return ((ProjectOrDomainEntity) getTargetObject()).getProjectOrDomain();
		}
		return null;
	}

	ReportGenerator getReportGenerator() {
		if (getTargetObject() instanceof ReportGenerator) {
			return (ReportGenerator) getTargetObject();
		}
		return null;
	}

	private static class UpdateListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final ReportGeneratorEditorPanel panel;

		private UpdateListener(ReportGeneratorEditorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (panel.deleted) {
				return;
			}
			ReportGenerator existingReportGenerator = panel.getReportGenerator();
			if ((e instanceof UpdateEntityEvent) && (existingReportGenerator != null)) {
				UpdateEntityEvent event = (UpdateEntityEvent) e;
				ReportGenerator updatedReportGenerator = null;
				if (event.getObject() instanceof ReportGenerator) {
					updatedReportGenerator = (ReportGenerator) event.getObject();
					if ((event instanceof DeletedEntityEvent)
							&& existingReportGenerator.equals(updatedReportGenerator)) {
						panel.deleted = true;
						panel.getEventDispatcher().dispatchEvent(
								new DeletedEntityEvent(this, panel, existingReportGenerator));
						return;
					}
				} else if (event.getObject() instanceof Annotation) {
					Annotation updatedAnnotation = (Annotation) event.getObject();
					if (event instanceof DeletedEntityEvent) {
						if (existingReportGenerator.getAnnotations().contains(updatedAnnotation)) {
							existingReportGenerator.getAnnotations().remove(updatedAnnotation);
						}
						updatedReportGenerator = existingReportGenerator;
					} else if (updatedAnnotation.getAnnotatables()
							.contains(existingReportGenerator)) {
						for (Annotatable annotatable : updatedAnnotation.getAnnotatables()) {
							if (annotatable.equals(existingReportGenerator)) {
								updatedReportGenerator = (ReportGenerator) annotatable;
								break;
							}
						}
					}
				}
				if ((updatedReportGenerator != null)
						&& updatedReportGenerator.equals(existingReportGenerator)) {
					// TODO: check the input fields to see if the user has made
					// a change before reseting the object and updating the
					// input fields.
					panel.setInputValue(EditReportGeneratorCommand.FIELD_NAME,
							updatedReportGenerator.getName());
					panel.setInputValue(EditReportGeneratorCommand.FIELD_TEXT,
							updatedReportGenerator.getText());
					panel.setTargetObject(updatedReportGenerator);
				}
			}
		}
	}

	private class XsltUploadListener implements UploadListener {
		static final long serialVersionUID = 0L;

		private final ReportGeneratorEditorPanel panel;

		private XsltUploadListener(ReportGeneratorEditorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void fileUpload(UploadEvent uploadEvent) {
			try {
				panel.setInputValue(EditReportGeneratorCommand.FIELD_NAME, uploadEvent
						.getFileName());
				panel.setInputValue(EditReportGeneratorCommand.FIELD_TEXT, IOUtils
						.toString(uploadEvent.getInputStream()));
				panel.runButton.setEnabled(true);
				panel.setGeneralMessage("file " + uploadEvent.getFileName() + " uploaded.");
			} catch (Exception e) {
				panel.setGeneralMessage("Could not upload file: " + e);
			}
		}

		@Override
		public void invalidFileUpload(UploadEvent uploadEvent) {
			panel.setGeneralMessage("Could not upload file.");
		}
	}
}
