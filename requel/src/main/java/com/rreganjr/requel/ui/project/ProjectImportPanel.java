/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * 
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
package com.rreganjr.requel.ui.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import nextapp.echo2.app.CheckBox;
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
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.ImportProjectCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import net.sf.echopm.navigation.event.UpdateEntityEvent;
import net.sf.echopm.panel.editor.ToggleButtonModelEx;

/**
 * Panel for importing a project.
 * 
 * @author ron
 */
public class ProjectImportPanel extends AbstractRequelProjectEditorPanel {
	private static final Log log = LogFactory.getLog(ProjectImportPanel.class);

	static final long serialVersionUID = 0L;

	/**
	 * The name to use in the ProjectImportPanel.properties file to set the
	 * label of the name field. If the property is undefined "Name" is used.
	 */
	public static final String PROP_LABEL_NAME = "Name.Label";

	/**
	 * The name to use in the ProjectImportPanel.properties file to set the
	 * label of the upload button for the project import function. If the
	 * property is undefined "Upload" is used.
	 */
	public static final String PROP_LABEL_UPLOAD_BUTTON = "UploadButton.Label";

	/**
	 * The name to use in the ProjectImportPanel.properties file to set the
	 * label of the upload button for the project import function. If the
	 * property is undefined "Upload" is used.
	 */
	public static final String PROP_LABEL_ENABLE_ANALYSIS_BUTTON = "EnableAnalysisButton.Label";

	private UpdateListener updateListener;
	private File tmpUpload;

	/**
	 * @param commandHandler
	 * @param projectCommandFactory
	 * @param projectRepository
	 * @param userRepository
	 */
	public ProjectImportPanel(CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, ProjectRepository projectRepository,
			UserRepository userRepository) {
		this(ProjectImportPanel.class.getName(), commandHandler, projectCommandFactory,
				projectRepository, userRepository);
	}

	/**
	 * @param resourceBundleName
	 * @param commandHandler
	 * @param projectCommandFactory
	 * @param projectRepository
	 * @param userRepository
	 */
	public ProjectImportPanel(String resourceBundleName, CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, ProjectRepository projectRepository,
			UserRepository userRepository) {
		super(resourceBundleName, Project.class,
				ProjectManagementPanelNames.PROJECT_IMPORT_PANEL_NAME, commandHandler,
				projectCommandFactory, projectRepository);
	}

	@Override
	public boolean isReadOnlyMode() {
		// if this is a new project
		ProjectUserRole projectUserRole = ((User) getApp().getUser()).getRoleForType(ProjectUserRole.class);
		return !projectUserRole.canCreateProjects();
	}

	/**
	 * If the editor is editing an existing project the title specified in the
	 * properties file as PROP_EXISTING_OBJECT_PANEL_TITLE If that property is
	 * not set it then tries the standard PROP_PANEL_TITLE and if that does not
	 * exist it defaults to:<br>
	 * "Project Overview: {0}"<br>
	 * Valid variables are:<br>
	 * {0} - project name<br>
	 * {1} - organization name<br>
	 * For new projects it first tries PROP_NEW_OBJECT_PANEL_TITLE, then
	 * PROP_PANEL_TITLE and finally defaults to:<br>
	 * "New Project"<br>
	 * 
	 * @see AbstractEditorPanel.PROP_EXISTING_OBJECT_PANEL_TITLE
	 * @see AbstractEditorPanel.PROP_NEW_OBJECT_PANEL_TITLE
	 * @see Panel.PROP_PANEL_TITLE
	 * @see net.sf.echopm.panel.AbstractPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		String msg = getResourceBundleHelper(getLocale()).getString(
				PROP_NEW_OBJECT_PANEL_TITLE, getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE, "Project Import"));
		return msg;
	}

	@Override
	public void setup() {
		super.setup();
		addInput(EditProjectCommand.FIELD_NAME, PROP_LABEL_NAME, "Rename", new TextField(),
				new StringDocumentEx());
		try {
			UploadSelect importXml = addInput("importXml", PROP_LABEL_UPLOAD_BUTTON, "Upload",
					new UploadSelect(), null);
			importXml.addUploadListener(new ProjectImportUploadListener(this));
			importXml.setEnabledSendButtonText(getResourceBundleHelper(getLocale()).getString(
					PROP_LABEL_UPLOAD_BUTTON, "Upload"));
		} catch (Exception e) {
			log.error(e, e);
		}
		addInput("enableAnalysis", PROP_LABEL_ENABLE_ANALYSIS_BUTTON, "Enable Analysis",
				new CheckBox(), new ToggleButtonModelEx(true));

		if (updateListener != null) {
			getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
					updateListener);
		}
		updateListener = new UpdateListener(this);
		getEventDispatcher().addEventTypeActionListener(UpdateEntityEvent.class, updateListener);
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
			if (tmpUpload != null) {
				try {
					InputStream inputStream = new FileInputStream(tmpUpload);
					ImportProjectCommand command = getProjectCommandFactory().newImportProjectCommand();
					command.setName(getInputValue(EditProjectCommand.FIELD_NAME, String.class));
					command.setEditedBy(getCurrentUser());
					command.setInputStream(inputStream);
					command.setAnalysisEnabled(getInputValue("enableAnalysis", Boolean.class));
					command = getCommandHandler().execute(command);
					setTargetObject(command.getProject());
					getEventDispatcher().dispatchEvent(new UpdateEntityEvent(this, command.getProject()));
				} catch (Exception e) {
					log.error("could not import the project: " + e, e);
					setGeneralMessage("Could not import: " + e);
				}
			} else {
				setGeneralMessage("A file must be uploaded before importing.");
			}

			if (updateListener != null) {
				getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
						updateListener);
			}
		} catch (EntityException e) {
			if ((e.getEntityPropertyNames() != null) && (e.getEntityPropertyNames().length > 0)) {
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
			log.error("could not import the project: " + e, e);
			setGeneralMessage("Could not import: " + e);
		}
	}

	private class ProjectImportUploadListener implements UploadListener {
		static final long serialVersionUID = 0L;

		private final ProjectImportPanel panel;

		private ProjectImportUploadListener(ProjectImportPanel panel) {
			this.panel = panel;
		}

		@Override
		public void fileUpload(UploadEvent uploadEvent) {
			FileOutputStream tmpOutStream = null;
			try {
				tmpUpload = File.createTempFile("projectImport", ".xml");
				tmpOutStream = new FileOutputStream(tmpUpload);
				IOUtils.copy(uploadEvent.getInputStream(), tmpOutStream);
				panel.setGeneralMessage("Project file " + uploadEvent.getFileName()
						+ " uploaded and ready for import.");
			} catch (Exception e) {
				panel.setGeneralMessage("Could not upload file: " + e);
			} finally {
				if (tmpOutStream != null) {
					IOUtils.closeQuietly(tmpOutStream);
				}
			}
		}

		@Override
		public void invalidFileUpload(UploadEvent uploadEvent) {
			panel.setGeneralMessage("Could not upload file.");
		}
	}

	private static class UpdateListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final ProjectImportPanel panel;

		private UpdateListener(ProjectImportPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
		}
	}
}
