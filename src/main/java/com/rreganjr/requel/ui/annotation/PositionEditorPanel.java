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
package com.rreganjr.requel.ui.annotation;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Column;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.layout.ColumnLayoutData;
import nextapp.echo2.app.layout.RowLayoutData;

import org.apache.log4j.Logger;
import com.rreganjr.validator.InvalidStateException;
import com.rreganjr.validator.InvalidValue;

import echopointng.text.StringDocumentEx;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.repository.EntityException;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Argument;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.DeletePositionCommand;
import com.rreganjr.requel.annotation.command.EditPositionCommand;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.user.User;
import net.sf.echopm.navigation.NavigatorButton;
import net.sf.echopm.navigation.WorkflowDisposition;
import net.sf.echopm.navigation.event.DeletedEntityEvent;
import net.sf.echopm.navigation.event.NavigationEvent;
import net.sf.echopm.navigation.event.OpenPanelEvent;
import net.sf.echopm.navigation.event.UpdateEntityEvent;
import net.sf.echopm.navigation.table.NavigatorTable;
import net.sf.echopm.navigation.table.NavigatorTableCellValueFactory;
import net.sf.echopm.navigation.table.NavigatorTableColumnConfig;
import net.sf.echopm.navigation.table.NavigatorTableConfig;
import net.sf.echopm.navigation.table.NavigatorTableModel;
import net.sf.echopm.panel.PanelActionType;

/**
 * @author ron
 */
public class PositionEditorPanel extends AbstractRequelAnnotationEditorPanel {
	private static final Logger log = Logger.getLogger(PositionEditorPanel.class);

	static final long serialVersionUID = 0L;

	/**
	 * The name to use in the PositionEditorPanel.properties file to set the
	 * label of the position text field. If the property is undefined "Position"
	 * is used.
	 */
	public static final String PROP_LABEL_POSITION = "Position.Label";

	/**
	 * The name to use in the PositionEditorPanel.properties file to set the
	 * label of the arguments field. If the property is undefined "Arguments" is
	 * used.
	 */
	public static final String PROP_LABEL_ARGUMENTS = "Arguments.Label";

	/**
	 * The name to use in the PositionEditorPanel.properties file to set the
	 * label of the view button in the argument edit table column. If the
	 * property is undefined "View" is used.
	 */
	public static final String PROP_VIEW_ARGUMENT_BUTTON_LABEL = "ViewArgument.Label";

	/**
	 * The name to use in the PositionEditorPanel.properties file to set the
	 * label of the edit button in the argument edit table column. If the
	 * property is undefined "Edit" is used.
	 */
	public static final String PROP_EDIT_ARGUMENT_BUTTON_LABEL = "EditArgument.Label";

	/**
	 * The name to use in the PositionEditorPanel.properties file to set the
	 * label of the add button in the arguments editor. If the property is
	 * undefined "Add" is used.
	 */
	public static final String PROP_ADD_ARGUMENT_BUTTON_LABEL = "AddArgument.Label";

	private final AnnotationCommandFactory annotationCommandFactory;
	private UpdateListener updateListener;

	// this is set by the DeleteListener so that the UpdateListener can ignore
	// events between when the object was deleted and the panel goes away.
	private boolean deleted = false;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param annotationRepository
	 */
	public PositionEditorPanel(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory,
			AnnotationRepository annotationRepository) {
		this(PositionEditorPanel.class.getName(), commandHandler, annotationCommandFactory,
				annotationRepository);
	}

	/**
	 * @param resourceBundleName
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param annotationRepository
	 */
	public PositionEditorPanel(String resourceBundleName, CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory,
			AnnotationRepository annotationRepository) {
		super(resourceBundleName, Position.class, commandHandler, annotationRepository);
		this.annotationCommandFactory = annotationCommandFactory;
	}

	/**
	 * If the editor is editing an existing position the title specified in the
	 * properties file as PROP_EXISTING_OBJECT_PANEL_TITLE If that property is
	 * not set it then tries the standard PROP_PANEL_TITLE and if that does not
	 * exist it defaults to:<br>
	 * "Edit Position"<br>
	 * For a new position it first tries PROP_NEW_OBJECT_PANEL_TITLE, then
	 * PROP_PANEL_TITLE and finally defaults to:<br>
	 * "New Position"<br>
	 * 
	 * @see AbstractEditorPanel.PROP_EXISTING_OBJECT_PANEL_TITLE
	 * @see AbstractEditorPanel.PROP_NEW_OBJECT_PANEL_TITLE
	 * @see Panel.PROP_PANEL_TITLE
	 * @see net.sf.echopm.panel.AbstractPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		if (getPosition() != null) {
			String msgPattern = getResourceBundleHelper(getLocale()).getString(
					PROP_EXISTING_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE,
							"Edit Position"));
			return MessageFormat.format(msgPattern, getPosition().toString());
		} else {
			String msg = getResourceBundleHelper(getLocale()).getString(
					PROP_NEW_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale())
							.getString(PROP_PANEL_TITLE, "New Position"));
			return msg;
		}
	}

	@Override
	public void setup() {
		super.setup();
		Position position = getPosition();
		if (position != null) {
			addInput("text", PROP_LABEL_POSITION, "Position", new TextArea(), new StringDocumentEx(
					position.getText()));
			addMultiRowInput("arguments", PROP_LABEL_ARGUMENTS, "Arguments", getArgumentsTable(),
					new NavigatorTableModel((Collection) position.getArguments()));
		} else {
			addInput("text", PROP_LABEL_POSITION, "Position", new TextArea(),
					new StringDocumentEx());
			addMultiRowInput("arguments", PROP_LABEL_ARGUMENTS, "Arguments", getArgumentsTable(),
					new NavigatorTableModel(new TreeSet<Object>()));
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

	private Component getArgumentsTable() {
		ColumnLayoutData layoutData = new ColumnLayoutData();
		layoutData.setAlignment(Alignment.ALIGN_CENTER);
		Column col = new Column();
		NavigatorTable relationTable = new NavigatorTable(getArgumentsTableConfig());
		relationTable.setLayoutData(layoutData);
		col.add(relationTable);
		if ((getPosition() != null) && !isReadOnlyMode()) {
			String buttonLabel = null;
			buttonLabel = getResourceBundleHelper(getLocale()).getString(
					PROP_ADD_ARGUMENT_BUTTON_LABEL, "Add");
			NavigationEvent openEditorEvent = new OpenPanelEvent(this, PanelActionType.Editor,
					getPosition(), Argument.class, null, WorkflowDisposition.NewFlow);
			NavigatorButton openEditorButton = new NavigatorButton(buttonLabel,
					getEventDispatcher(), openEditorEvent);
			openEditorButton.setStyleName(STYLE_NAME_DEFAULT);
			openEditorButton.setLayoutData(layoutData);
			col.add(openEditorButton);
		}
		return col;
	}

	private NavigatorTableConfig getArgumentsTableConfig() {
		NavigatorTableConfig tableConfig = new NavigatorTableConfig();

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Argument argument = (Argument) model.getBackingObject(row);
						String buttonLabel = null;
						if (isReadOnlyMode()) {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_VIEW_ARGUMENT_BUTTON_LABEL, "View");
						} else {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_EDIT_ARGUMENT_BUTTON_LABEL, "Edit");
						}
						NavigationEvent openEditorEvent = new OpenPanelEvent(this,
								PanelActionType.Editor, argument, Argument.class, null,
								WorkflowDisposition.NewFlow);
						NavigatorButton openEditorButton = new NavigatorButton(buttonLabel,
								getEventDispatcher(), openEditorEvent);
						openEditorButton.setStyleName(STYLE_NAME_PLAIN);
						RowLayoutData rld = new RowLayoutData();
						rld.setAlignment(Alignment.ALIGN_CENTER);
						openEditorButton.setLayoutData(rld);
						return openEditorButton;
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Support Level",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Argument argument = (Argument) model.getBackingObject(row);
						return argument.getSupportLevel().toString();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Text",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Argument argument = (Argument) model.getBackingObject(row);
						return argument.getText();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Created By",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Argument argument = (Argument) model.getBackingObject(row);
						return argument.getCreatedBy().getUsername();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Date Created",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Argument argument = (Argument) model.getBackingObject(row);
						DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
						return formatter.format(argument.getDateCreated());
					}
				}));

		return tableConfig;
	}

	@Override
	public boolean isReadOnlyMode() {
		// project entities are subject to stakeholder permissions, so if this
		// position is on an issue concerned with a project or project entity
		// check the stakeholder permissions for editing annotations.
		User user = (User) getApp().getUser();
		Project project = getProject();
		if (project != null) {
			UserStakeholder stakeholder = project.getUserStakeholder(user);
			if (stakeholder != null) {
				return !stakeholder.hasPermission(Annotation.class, StakeholderPermissionType.Edit);
			}
		}
		return false;
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
			EditPositionCommand command = getAnnotationCommandFactory().newEditPositionCommand();
			command.setPosition(getPosition());
			command.setIssue(getIssue());
			command.setEditedBy(getCurrentUser());
			command.setText(getInputValue("text", String.class));
			command = getCommandHandler().execute(command);
			setValid(true);
			if (updateListener != null) {
				getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
						updateListener);
			}
			Position position = command.getPosition();
			getEventDispatcher().dispatchEvent(new UpdateEntityEvent(this, position));
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
			log.error("could not save the goal: " + e, e);
			setGeneralMessage("Could not save: " + e);
		}
	}

	@Override
	public void delete() {
		try {
			Set<Issue> issues = getPosition().getIssues();
			DeletePositionCommand deletePositionCommand = getAnnotationCommandFactory()
					.newDeletePositionCommand();
			deletePositionCommand.setPosition(getPosition());
			deletePositionCommand.setEditedBy(getCurrentUser());
			deletePositionCommand = getCommandHandler().execute(deletePositionCommand);
			deleted = true;
			for (Issue issue : issues) {
				getEventDispatcher().dispatchEvent(new DeletedEntityEvent(this, issue));
			}
		} catch (Exception e) {
			setGeneralMessage("Could not delete entity: " + e);
		}
	}

	private Project getProject() {
		Project project = null;
		if (getIssue() != null) {
			project = (Project) getIssue().getGroupingObject();
		} else if (getPosition() != null) {
			project = (Project) getPosition().getIssues().iterator().next().getGroupingObject();
		}
		return project;
	}

	private Issue getIssue() {
		if (getTargetObject() instanceof Issue) {
			return (Issue) getTargetObject();
		}
		return null;
	}

	private Position getPosition() {
		if (getTargetObject() instanceof Position) {
			return (Position) getTargetObject();
		}
		return null;
	}

	private AnnotationCommandFactory getAnnotationCommandFactory() {
		return annotationCommandFactory;
	}

	private static class UpdateListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final PositionEditorPanel panel;

		private UpdateListener(PositionEditorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (panel.deleted) {
				return;
			}
			Position existingPosition = panel.getPosition();
			if ((e instanceof UpdateEntityEvent) && (existingPosition != null)) {
				UpdateEntityEvent event = (UpdateEntityEvent) e;
				Position updatedPosition = null;
				if (event.getObject() instanceof Position) {
					updatedPosition = (Position) event.getObject();
					if (existingPosition.equals(updatedPosition)
							&& (event instanceof DeletedEntityEvent)) {
						panel.deleted = true;
						panel.getEventDispatcher().dispatchEvent(
								new DeletedEntityEvent(this, panel, existingPosition));
						return;
					}
				} else if (event.getObject() instanceof Argument) {
					Argument updatedArgument = (Argument) event.getObject();
					if (panel.getPosition().equals(updatedArgument.getPosition())) {
						updatedPosition = updatedArgument.getPosition();
					}
				}
				if ((updatedPosition != null) && updatedPosition.equals(existingPosition)) {
					panel.setInputValue("text", updatedPosition.getText());
					panel.setInputValue("arguments", updatedPosition.getArguments());
					panel.setTargetObject(updatedPosition);
				}
			}
		}
	}
}
