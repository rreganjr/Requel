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

import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;

import nextapp.echo2.app.Label;
import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.filetransfer.DownloadProvider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.validator.InvalidStateException;
import org.hibernate.validator.InvalidValue;

import echopointng.ComboBox;
import echopointng.text.StringDocumentEx;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.repository.EntityException;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.ExportProjectCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.ui.annotation.AnnotationsTable;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import net.sf.echopm.navigation.DownloadButton;
import net.sf.echopm.navigation.event.DeletedEntityEvent;
import net.sf.echopm.navigation.event.UpdateEntityEvent;
import net.sf.echopm.panel.editor.CombinedTextListModel;

/**
 * Create or Update a project's name, description, organization, or project
 * general annotations.
 * 
 * @author ron
 */
public class ProjectOverviewPanel extends AbstractRequelProjectEditorPanel {
	private static final Log log = LogFactory.getLog(ProjectOverviewPanel.class);

	static final long serialVersionUID = 0L;

	/**
	 * The name to use in the ProjectOverviewPanel.properties file to set the
	 * label of the name field. If the property is undefined "Name" is used.
	 */
	public static final String PROP_LABEL_NAME = "Name.Label";

	/**
	 * The name to use in the ProjectOverviewPanel.properties file to set the
	 * label of the description field. If the property is undefined
	 * "Description" is used.
	 */
	public static final String PROP_LABEL_DESCRIPTION = "Description.Label";

	/**
	 * The name to use in the ProjectOverviewPanel.properties file to set the
	 * label of the organization field. If the property is undefined
	 * "Organization" is used.
	 */
	public static final String PROP_LABEL_ORGANIZATION = "Organization.Label";

	/**
	 * The name to use in the ProjectOverviewPanel.properties file to set the
	 * label of the created by user field. If the property is undefined "Created
	 * By" is used.
	 */
	public static final String PROP_LABEL_CREATED_BY = "CreatedBy.Label";

	/**
	 * The name to use in the ProjectOverviewPanel.properties file to set the
	 * label of the export button. If the property is undefined "Export" is
	 * used.
	 */
	public static final String PROP_LABEL_EXPORT_BUTTON = "ExportButton.Label";

	private final UserRepository userRepository;
	private UpdateListener updateListener;

	/**
	 * @param commandHandler
	 * @param projectCommandFactory
	 * @param projectRepository
	 * @param userRepository
	 */
	public ProjectOverviewPanel(CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, ProjectRepository projectRepository,
			UserRepository userRepository) {
		this(ProjectOverviewPanel.class.getName(), commandHandler, projectCommandFactory,
				projectRepository, userRepository);
	}

	/**
	 * @param resourceBundleName
	 * @param commandHandler
	 * @param projectCommandFactory
	 * @param projectRepository
	 * @param userRepository
	 */
	public ProjectOverviewPanel(String resourceBundleName, CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, ProjectRepository projectRepository,
			UserRepository userRepository) {
		super(resourceBundleName, Project.class,
				ProjectManagementPanelNames.PROJECT_OVERVIEW_PANEL_NAME, commandHandler,
				projectCommandFactory, projectRepository);
		this.userRepository = userRepository;
	}

	@Override
	public boolean isReadOnlyMode() {
		if (getProject() == null) {
			// if this is a new project
			ProjectUserRole projectUserRole = ((User) getApp().getUser())
					.getRoleForType(ProjectUserRole.class);
			return !projectUserRole.canCreateProjects();
		} else {
			return super.isReadOnlyMode();
		}
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
		if (getProject() != null) {
			String msgPattern = getResourceBundleHelper(getLocale()).getString(
					PROP_EXISTING_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE,
							"Project Overview: {0}"));
			return MessageFormat.format(msgPattern, getProject().getName(), getProject()
					.getOrganization().getName());
		} else {
			String msg = getResourceBundleHelper(getLocale())
					.getString(
							PROP_NEW_OBJECT_PANEL_TITLE,
							getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE,
									"New Project"));
			return msg;
		}
	}

	@Override
	public void setup() {
		super.setup();
		Project project = getProject();
		if (project != null) {
			addInput(EditProjectCommand.FIELD_NAME, PROP_LABEL_NAME, "Name", new TextField(),
					new StringDocumentEx(project.getName()));
			addInput("description", PROP_LABEL_DESCRIPTION, "Description", new TextArea(),
					new StringDocumentEx(project.getText()));
			addInput("organizationName", PROP_LABEL_ORGANIZATION, "Organization", new ComboBox(),
					new CombinedTextListModel(getUserRepository().getOrganizationNames(), project
							.getOrganization().getName()));

			User createdBy = project.getCreatedBy();
			StringBuilder sb = new StringBuilder(50);
			sb.append(createdBy.getUsername());
			if ((createdBy.getName() != null) && (createdBy.getName().length() > 0)) {
				sb.append(" [");
				sb.append(createdBy.getName());
				sb.append("]");
			}
			addInput("createdBy", PROP_LABEL_CREATED_BY, "CreatedBy", new Label(), sb.toString());
			addMultiRowInput("annotations", AnnotationsTable.PROP_LABEL_ANNOTATIONS, "Annotations",
					new AnnotationsTable(this, getResourceBundleHelper(getLocale())), project);

			addActionButton(new DownloadButton(getResourceBundleHelper(getLocale()).getString(
					PROP_LABEL_EXPORT_BUTTON, "Export"), new ProjectExportDownloadProvider(this)));
		} else {
			addInput(EditProjectCommand.FIELD_NAME, PROP_LABEL_NAME, "Name", new TextField(),
					new StringDocumentEx());
			addInput("description", PROP_LABEL_DESCRIPTION, "Description", new TextArea(),
					new StringDocumentEx());
			addInput("organizationName", PROP_LABEL_ORGANIZATION, "Organization", new ComboBox(),
					new CombinedTextListModel(getUserRepository().getOrganizationNames(), ""));
		}

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
			EditProjectCommand command = getProjectCommandFactory().newEditProjectCommand();
			command.setProject(getProject());
			command.setEditedBy(getCurrentUser());
			command.setName(getInputValue(EditProjectCommand.FIELD_NAME, String.class));
			command.setText(getInputValue("description", String.class));
			command.setOrganizationName(getInputValue("organizationName", String.class));
			command = getCommandHandler().execute(command);
			setValid(true);

			if (updateListener != null) {
				getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
						updateListener);
			}

			getEventDispatcher().dispatchEvent(new UpdateEntityEvent(this, command.getProject()));
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
			log.error("could not save the project: " + e, e);
			setGeneralMessage("Could not save: " + e);
		}
	}

	private void exportProject(OutputStream outputStream) {
		try {
			ExportProjectCommand command = getProjectCommandFactory().newExportProjectCommand();
			command.setProject(getProject());
			command.setOutputStream(outputStream);
			command = getCommandHandler().execute(command);
		} catch (Exception e) {
			log.error("could not export the project: " + e, e);
			setGeneralMessage("Could not export: " + e);
		}
	}

	private Project getProject() {
		return (Project) getTargetObject();
	}

	private UserRepository getUserRepository() {
		return userRepository;
	}

	private class ProjectExportDownloadProvider implements DownloadProvider {
		private final ProjectOverviewPanel panel;

		private ProjectExportDownloadProvider(ProjectOverviewPanel panel) {
			this.panel = panel;
		}

		public String getContentType() {
			return "text/xml";
		}

		public String getFileName() {
			return panel.getProject().getName() + ".xml";
		}

		public int getSize() {
			return 0;
		}

		public void writeFile(OutputStream outputStream) throws IOException {
			panel.exportProject(outputStream);
		}
	}

	private static class UpdateListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final ProjectOverviewPanel panel;

		private UpdateListener(ProjectOverviewPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Project existingProject = (Project) panel.getTargetObject();
			if ((e instanceof UpdateEntityEvent) && (existingProject != null)) {
				UpdateEntityEvent event = (UpdateEntityEvent) e;
				Project updatedProject = null;
				if (event.getObject() instanceof Project) {
					updatedProject = (Project) event.getObject();
				} else if (event.getObject() instanceof Annotation) {
					Annotation updatedAnnotation = (Annotation) event.getObject();
					if (event instanceof DeletedEntityEvent) {
						updatedProject = existingProject;
						if (updatedProject.getAnnotations().contains(updatedAnnotation)) {
							updatedProject.getAnnotations().remove(updatedAnnotation);
						}
					} else {
						if (updatedAnnotation.getAnnotatables().contains(existingProject)) {
							for (Annotatable annotatable : updatedAnnotation.getAnnotatables()) {
								if (annotatable.equals(existingProject)) {
									updatedProject = (Project) annotatable;
									break;
								}
							}
						}
					}
				}
				if ((updatedProject != null) && updatedProject.equals(existingProject)) {
					// TODO: check the input fields to see if the user has made
					// a change before resetting the object and updating the
					// input fields.
					panel.setInputValue("annotations", updatedProject);
					panel.setTargetObject(updatedProject);
				}
			}
		}
	}
}
