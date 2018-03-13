/*
 * $Id: StakeholderEditorPanel.java 124 2009-05-21 23:46:02Z rreganjr $
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

import java.text.MessageFormat;

import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.validator.InvalidStateException;
import org.hibernate.validator.InvalidValue;

import echopointng.text.StringDocumentEx;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.repository.EntityException;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalContainer;
import com.rreganjr.requel.project.NonUserStakeholder;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.command.DeleteStakeholderCommand;
import com.rreganjr.requel.project.command.EditNonUserStakeholderCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.ui.annotation.AnnotationsTable;
import com.rreganjr.requel.user.UserRepository;
import net.sf.echopm.navigation.event.DeletedEntityEvent;
import net.sf.echopm.navigation.event.UpdateEntityEvent;

/**
 * An editor for non-user stakeholders.
 * 
 * @author ron
 */
public class NonUserStakeholderEditorPanel extends AbstractRequelProjectEditorPanel {
	private static final Log log = LogFactory.getLog(UserStakeholderEditorPanel.class);

	static final long serialVersionUID = 0L;

	/**
	 * The name to use in the StakeholderEditorPanel.properties file to set the
	 * label of the name field. If the property is undefined "Name" is used.
	 */
	public static final String PROP_LABEL_NAME = "Name.Label";

	/**
	 * The name to use in the StakeholderEditorPanel.properties file to set the
	 * label of the description field. If the property is undefined
	 * "Description" is used.
	 */
	public static final String PROP_LABEL_DESCRIPTION = "Description.Label";

	private UpdateListener updateListener;

	// this is set by the DeleteListener so that the UpdateListener can ignore
	// events between when the object was deleted and the panel goes away.
	private boolean deleted;

	/**
	 * @param commandHandler
	 * @param projectCommandFactory
	 * @param userRepository
	 * @param projectRepository
	 */
	public NonUserStakeholderEditorPanel(CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, UserRepository userRepository,
			ProjectRepository projectRepository) {
		this(NonUserStakeholderEditorPanel.class.getName(), commandHandler, projectCommandFactory,
				userRepository, projectRepository);
	}

	/**
	 * @param resourceBundleName
	 * @param commandHandler
	 * @param projectCommandFactory
	 * @param userRepository
	 * @param projectRepository
	 */
	public NonUserStakeholderEditorPanel(String resourceBundleName, CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, UserRepository userRepository,
			ProjectRepository projectRepository) {
		super(resourceBundleName, NonUserStakeholder.class, commandHandler, projectCommandFactory,
				projectRepository);
	}

	/**
	 * If the editor is editing an existing stakeholder the title specified in
	 * the properties file as PROP_EXISTING_OBJECT_PANEL_TITLE If that property
	 * is not set it then tries the standard PROP_PANEL_TITLE and if that does
	 * not exist it defaults to:<br>
	 * "Stakeholder: {0}"<br>
	 * Valid variables are:<br>
	 * {0} - stakeholder name / username<br>
	 * {1} - project/domain name<br>
	 * For new stakeholders it first tries PROP_NEW_OBJECT_PANEL_TITLE, then
	 * PROP_PANEL_TITLE and finally defaults to:<br>
	 * "New Stakeholder"<br>
	 * 
	 * @see AbstractEditorPanel.PROP_EXISTING_OBJECT_PANEL_TITLE
	 * @see AbstractEditorPanel.PROP_NEW_OBJECT_PANEL_TITLE
	 * @see Panel.PROP_PANEL_TITLE
	 * @see net.sf.echopm.panel.AbstractPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		if (getStakeholder() != null) {
			String msgPattern = getResourceBundleHelper(getLocale()).getString(
					PROP_EXISTING_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE,
							"Non-User Stakeholder: {0}"));
			return MessageFormat.format(msgPattern, getStakeholder().getName(),
					getProjectOrDomain().getName());
		} else {
			String msg = getResourceBundleHelper(getLocale()).getString(
					PROP_NEW_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE,
							"New Non-User Stakeholder"));
			return msg;
		}
	}

	@Override
	public void setup() {
		super.setup();
		NonUserStakeholder stakeholder = getStakeholder();
		if (stakeholder != null) {
			addInput("name", PROP_LABEL_NAME, "Name", new TextField(), new StringDocumentEx(
					stakeholder.getName()));
			addInput("text", PROP_LABEL_DESCRIPTION, "Description", new TextArea(),
					new StringDocumentEx(stakeholder.getText()));
			addMultiRowInput("goals", GoalsTable.PROP_LABEL_GOALS, "Goals", new GoalsTable(this,
					getResourceBundleHelper(getLocale()), getProjectCommandFactory(),
					getCommandHandler()), stakeholder);
			addMultiRowInput("annotations", AnnotationsTable.PROP_LABEL_ANNOTATIONS, "Annotations",
					new AnnotationsTable(this, getResourceBundleHelper(getLocale())), stakeholder);
		} else {
			addInput("name", PROP_LABEL_NAME, "Name", new TextField(), new StringDocumentEx());
			addInput("text", PROP_LABEL_DESCRIPTION, "Description", new TextArea(),
					new StringDocumentEx());
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
			updateListener = null;
		}
	}

	@Override
	public void save() {
		try {
			super.save();
			EditNonUserStakeholderCommand command = getProjectCommandFactory()
					.newEditNonUserStakeholderCommand();
			command.setStakeholder(getStakeholder());
			command.setProjectOrDomain(getProjectOrDomain());
			command.setEditedBy(getCurrentUser());
			command.setName(getInputValue("name", String.class));
			command.setText(getInputValue("text", String.class));
			command = getCommandHandler().execute(command);
			setValid(true);
			if (updateListener != null) {
				getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
						updateListener);
				updateListener = null;
			}
			// TODO: remove other listeners?
			getEventDispatcher().dispatchEvent(
					new UpdateEntityEvent(this, command.getStakeholder()));
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
			log.error("could not save the stakeholder: " + e, e);
			setGeneralMessage("Could not save: " + e);
		}
	}

	@Override
	public void delete() {
		try {
			DeleteStakeholderCommand deleteStakeholderCommand = getProjectCommandFactory()
					.newDeleteStakeholderCommand();
			deleteStakeholderCommand.setEditedBy(getCurrentUser());
			deleteStakeholderCommand.setStakeholder(getStakeholder());
			deleteStakeholderCommand = getCommandHandler().execute(deleteStakeholderCommand);
			deleted = true;
			getEventDispatcher().dispatchEvent(new DeletedEntityEvent(this, getStakeholder()));
		} catch (Exception e) {
			setGeneralMessage("Could not delete entity: " + e);
		}
	}

	// This is needed because permissions are granted at the Stakeholder level
	// and not
	// the UserStakeholder or NonUserStakeholder level.
	@Override
	public boolean isReadOnlyMode() {
		UserStakeholder stakeholder = getUserStakeholder(getTargetObject());
		if (stakeholder != null) {
			return !stakeholder.hasPermission(Stakeholder.class, StakeholderPermissionType.Edit);
		}
		return true;
	}

	// This is needed because permissions are granted at the Stakeholder level
	// and not
	// the UserStakeholder or NonUserStakeholder level.
	@Override
	protected boolean isShowDelete() {
		UserStakeholder stakeholder = getUserStakeholder(getTargetObject());
		if (stakeholder != null) {
			return stakeholder.hasPermission(Stakeholder.class, StakeholderPermissionType.Delete);
		}
		return false;
	}

	private ProjectOrDomain getProjectOrDomain() {
		ProjectOrDomain pod = null;
		if (getTargetObject() != null) {
			if (getTargetObject() instanceof ProjectOrDomain) {
				pod = (ProjectOrDomain) getTargetObject();
			} else if (getTargetObject() instanceof Stakeholder) {
				pod = getStakeholder().getProjectOrDomain();
			}
		}
		return pod;
	}

	private NonUserStakeholder getStakeholder() {
		if (getTargetObject() instanceof NonUserStakeholder) {
			return (NonUserStakeholder) getTargetObject();
		}
		return null;
	}

	private static class UpdateListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final NonUserStakeholderEditorPanel panel;

		private UpdateListener(NonUserStakeholderEditorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (panel.deleted) {
				return;
			}
			NonUserStakeholder existingStakeholder = panel.getStakeholder();
			if ((e instanceof UpdateEntityEvent) && (existingStakeholder != null)) {
				UpdateEntityEvent event = (UpdateEntityEvent) e;
				NonUserStakeholder updatedStakeholder = null;
				if (event.getObject() instanceof NonUserStakeholder) {
					updatedStakeholder = (NonUserStakeholder) event.getObject();
					if ((event instanceof DeletedEntityEvent)
							&& existingStakeholder.equals(updatedStakeholder)) {
						panel.deleted = true;
						panel.getEventDispatcher().dispatchEvent(
								new DeletedEntityEvent(this, panel, existingStakeholder));
						return;
					}
				} else if (event.getObject() instanceof Goal) {
					Goal updatedGoal = (Goal) event.getObject();
					if (event instanceof DeletedEntityEvent) {
						if (existingStakeholder.getGoals().contains(updatedGoal)) {
							existingStakeholder.getGoals().remove(updatedGoal);
						}
						updatedStakeholder = existingStakeholder;
					} else if (updatedGoal.getReferers().contains(existingStakeholder)) {
						for (GoalContainer gc : updatedGoal.getReferers()) {
							if (gc.equals(existingStakeholder)) {
								updatedStakeholder = (NonUserStakeholder) gc;
								break;
							}
						}
					}
				} else if (event.getObject() instanceof Annotation) {
					Annotation updatedAnnotation = (Annotation) event.getObject();
					if (event instanceof DeletedEntityEvent) {
						if (existingStakeholder.getAnnotations().contains(updatedAnnotation)) {
							existingStakeholder.getAnnotations().remove(updatedAnnotation);
						}
						updatedStakeholder = existingStakeholder;
					} else if (updatedAnnotation.getAnnotatables().contains(existingStakeholder)) {
						for (Annotatable annotatable : updatedAnnotation.getAnnotatables()) {
							if (annotatable.equals(existingStakeholder)) {
								updatedStakeholder = (NonUserStakeholder) annotatable;
								break;
							}
						}
					}
				}
				if ((updatedStakeholder != null) && updatedStakeholder.equals(existingStakeholder)) {
					// TODO: check the input fields to see if the user has made
					// a change before resetting the object and updating the
					// input fields.
					panel.setInputValue("name", updatedStakeholder.getName());
					panel.setInputValue("text", updatedStakeholder.getText());
					panel.setInputValue("goals", updatedStakeholder);
					panel.setInputValue("annotations", updatedStakeholder);
					panel.setTargetObject(updatedStakeholder);
				}
			}
		}
	}
}
