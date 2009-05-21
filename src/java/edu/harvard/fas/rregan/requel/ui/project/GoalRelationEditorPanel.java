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

import java.text.MessageFormat;
import java.util.Set;
import java.util.TreeSet;

import nextapp.echo2.app.Button;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.SelectField;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;

import org.apache.log4j.Logger;
import org.hibernate.validator.InvalidStateException;
import org.hibernate.validator.InvalidValue;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.GoalRelation;
import edu.harvard.fas.rregan.requel.project.GoalRelationType;
import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.Stakeholder;
import edu.harvard.fas.rregan.requel.project.StakeholderPermissionType;
import edu.harvard.fas.rregan.requel.project.command.DeleteGoalRelationCommand;
import edu.harvard.fas.rregan.requel.project.command.EditGoalRelationCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.ui.annotation.AnnotationsTable;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.uiframework.navigation.event.DeletedEntityEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.UpdateEntityEvent;
import edu.harvard.fas.rregan.uiframework.panel.editor.CombinedListModel;
import edu.harvard.fas.rregan.uiframework.panel.editor.manipulators.ComponentManipulator;
import edu.harvard.fas.rregan.uiframework.panel.editor.manipulators.ComponentManipulators;

/**
 * @author ron
 */
public class GoalRelationEditorPanel extends AbstractRequelProjectEditorPanel {
	private static final Logger log = Logger.getLogger(GoalRelationEditorPanel.class);

	static final long serialVersionUID = 0L;

	/**
	 * The name to use in the GoalRelationEditorPanel.properties file to set the
	 * label of the from goal field. If the property is undefined "From Goal" is
	 * used.
	 */
	public static final String PROP_LABEL_FROM_GOAL = "FromGoal.Label";

	/**
	 * The name to use in the GoalRelationEditorPanel.properties file to set the
	 * label of the to goal field. If the property is undefined "To Goal" is
	 * used.
	 */
	public static final String PROP_LABEL_TO_GOAL = "ToGoal.Label";

	/**
	 * The name to use in the GoalRelationEditorPanel.properties file to set the
	 * label of the relation type field. If the property is undefined "Relation
	 * Type" is used.
	 */
	public static final String PROP_LABEL_RELATION_TYPE = "RelationType.Label";

	private UpdateListener updateListener;
	private Button deleteButton;
	// this is set by the DeleteListener so that the UpdateListener can ignore
	// events between when the object was deleted and the panel goes away.
	private boolean deleted;

	/**
	 * @param commandHandler
	 * @param projectCommandFactory
	 * @param userRepository
	 * @param projectRepository
	 */
	public GoalRelationEditorPanel(CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, ProjectRepository projectRepository) {
		this(GoalRelationEditorPanel.class.getName(), commandHandler, projectCommandFactory,
				projectRepository);
	}

	/**
	 * @param resourceBundleName
	 * @param commandHandler
	 * @param projectCommandFactory
	 * @param userRepository
	 * @param projectRepository
	 */
	public GoalRelationEditorPanel(String resourceBundleName, CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, ProjectRepository projectRepository) {
		super(resourceBundleName, GoalRelation.class, commandHandler, projectCommandFactory,
				projectRepository);
	}

	/**
	 * @return the value of Panel.PROP_PANEL_TITLE or "Goal Relation" if the
	 *         title isn't defined
	 * @see Panel.PROP_PANEL_TITLE
	 * @see edu.harvard.fas.rregan.uiframework.panel.AbstractPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		// return
		// getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE,
		// "Goal Relation");
		if ((getGoalRelation() != null) && (getGoalRelation().getToGoal() != null)) {
			String msgPattern = getResourceBundleHelper(getLocale()).getString(
					PROP_EXISTING_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE,
							"Goal Relation From: {0}"));
			return MessageFormat.format(msgPattern, getGoalRelation().getToGoal().getName(),
					getProjectOrDomain().getName());
		} else {
			String msg = getResourceBundleHelper(getLocale()).getString(
					PROP_NEW_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE,
							"New Goal Relation"));
			return msg;
		}
	}

	@Override
	public void setup() {
		super.setup();
		GoalRelation goalRelation = getGoalRelation();
		if (goalRelation != null) {
			addInput("fromGoal", PROP_LABEL_FROM_GOAL, "From Goal", new SelectField(),
					new CombinedListModel(getGoalNames(), goalRelation.getFromGoal().getName(),
							true));
			addInput("toGoal", PROP_LABEL_TO_GOAL, "To Goal", new SelectField(),
					new CombinedListModel(getGoalNames(), goalRelation.getToGoal().getName(), true));
			addInput("relationType", PROP_LABEL_RELATION_TYPE, "Relation", new SelectField(),
					new CombinedListModel(getGoalRelationTypeNames(), goalRelation
							.getRelationType().toString(), true));
			addMultiRowInput("annotations", AnnotationsTable.PROP_LABEL_ANNOTATIONS, "Annotations",
					new AnnotationsTable(this, getResourceBundleHelper(getLocale())), goalRelation);
			// TODO: special permission to delete?
			deleteButton = addActionButton(new Button(getResourceBundleHelper(getLocale())
					.getString(PROP_LABEL_DELETE_BUTTON, "Delete")));
			deleteButton.addActionListener(new DeleteListener(this));
			deleteButton.setEnabled(!isReadOnlyMode());
		} else {
			addInput("fromGoal", PROP_LABEL_FROM_GOAL, "From Goal", new SelectField(),
					new CombinedListModel(getGoalNames(), getFromGoal().getName(), true));
			addInput("toGoal", PROP_LABEL_TO_GOAL, "To Goal", new SelectField(),
					new CombinedListModel(getGoalNames(), "", true));
			addInput("relationType", PROP_LABEL_RELATION_TYPE, "Relation", new SelectField(),
					new CombinedListModel(getGoalRelationTypeNames(), "", true));
			addMultiRowInput("annotations", AnnotationsTable.PROP_LABEL_ANNOTATIONS, "Annotations",
					new AnnotationsTable(this, getResourceBundleHelper(getLocale())), null);
		}

		if (updateListener != null) {
			getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
					updateListener);
		}
		updateListener = new UpdateListener(this);
		getEventDispatcher().addEventTypeActionListener(UpdateEntityEvent.class, updateListener);
	}

	@Override
	public boolean isReadOnlyMode() {
		User user = (User) getApp().getUser();
		if (getProjectOrDomain() instanceof Project) {
			Project project = (Project) getProjectOrDomain();
			Stakeholder stakeholder = project.getUserStakeholder(user);
			if (stakeholder != null) {
				return !stakeholder.hasPermission(Goal.class, StakeholderPermissionType.Edit);
			}
		}
		return true;
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
			EditGoalRelationCommand command = getProjectCommandFactory()
					.newEditGoalRelationCommand();
			command.setGoalRelation(getGoalRelation());
			command.setProjectOrDomain(getProjectOrDomain());
			command.setEditedBy(getCurrentUser());
			command.setFromGoal(getInputValue("fromGoal", String.class));
			command.setToGoal(getInputValue("toGoal", String.class));
			command.setRelationType(getInputValue("relationType", String.class));
			command = getCommandHandler().execute(command);
			setValid(true);
			if (updateListener != null) {
				getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
						updateListener);
			}
			getEventDispatcher().dispatchEvent(
					new UpdateEntityEvent(this, command.getGoalRelation()));
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
			log.error("could not save the goal relation: " + e, e);
			setGeneralMessage("Could not save: " + e);
		}
	}

	private Set<String> getGoalNames() {
		Set<String> goalNames = new TreeSet<String>();
		if (getProjectOrDomain() != null) {
			for (Goal goal : getProjectOrDomain().getGoals()) {
				goalNames.add(goal.getName());
			}
		}
		return goalNames;
	}

	private Set<String> getGoalRelationTypeNames() {
		Set<String> goalRelationTypeNames = new TreeSet<String>();
		for (GoalRelationType relationType : GoalRelationType.values()) {
			goalRelationTypeNames.add(relationType.toString());
		}
		return goalRelationTypeNames;
	}

	private ProjectOrDomain getProjectOrDomain() {
		ProjectOrDomain pod = null;
		if (getTargetObject() instanceof Goal) {
			pod = getFromGoal().getProjectOrDomain();
		} else if (getTargetObject() instanceof GoalRelation) {
			pod = getGoalRelation().getFromGoal().getProjectOrDomain();
		}
		return pod;
	}

	private Goal getFromGoal() {
		Goal goal = null;
		if (getTargetObject() instanceof Goal) {
			goal = (Goal) getTargetObject();
		} else if (getTargetObject() instanceof GoalRelation) {
			goal = getGoalRelation().getFromGoal();
		}
		return goal;
	}

	private GoalRelation getGoalRelation() {
		if (getTargetObject() instanceof GoalRelation) {
			return (GoalRelation) getTargetObject();
		}
		return null;
	}

	private static class DeleteListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final GoalRelationEditorPanel panel;

		private DeleteListener(GoalRelationEditorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			try {
				Goal toGoalForUpdateEvent = panel.getGoalRelation().getToGoal();
				Goal fromGoalForUpdateEvent = panel.getGoalRelation().getFromGoal();
				DeleteGoalRelationCommand deleteGoalRelationCommand = panel
						.getProjectCommandFactory().newDeleteGoalRelationCommand();
				deleteGoalRelationCommand.setEditedBy(panel.getCurrentUser());
				deleteGoalRelationCommand.setGoalRelation(panel.getGoalRelation());
				deleteGoalRelationCommand = panel.getCommandHandler().execute(
						deleteGoalRelationCommand);
				panel.deleted = true;
				panel.getEventDispatcher().dispatchEvent(
						new DeletedEntityEvent(this, null, toGoalForUpdateEvent));
				panel.getEventDispatcher().dispatchEvent(
						new DeletedEntityEvent(this, panel, fromGoalForUpdateEvent));
			} catch (Exception e) {
				panel.setGeneralMessage("Could not delete goal relation: " + e);
			}
		}
	}

	private static class UpdateListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final GoalRelationEditorPanel panel;

		private UpdateListener(GoalRelationEditorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (panel.deleted) {
				return;
			}
			GoalRelation existingGoalRelation = (GoalRelation) panel.getTargetObject();
			if ((e instanceof UpdateEntityEvent) && (existingGoalRelation != null)) {
				UpdateEntityEvent event = (UpdateEntityEvent) e;
				GoalRelation updatedGoalRelation = null;
				if (event.getObject() instanceof GoalRelation) {
					updatedGoalRelation = (GoalRelation) event.getObject();
					if ((event instanceof DeletedEntityEvent)
							&& existingGoalRelation.equals(updatedGoalRelation)) {
						panel.deleted = true;
						panel.getEventDispatcher().dispatchEvent(
								new DeletedEntityEvent(this, panel, existingGoalRelation));
						return;
					}
				} else if (event.getObject() instanceof Goal) {
					if (event instanceof DeletedEntityEvent) {
						// TODO: if the to/from goal is deleted then this goal
						// relation was probably deleted too.

					} else {
						// update the selection lists
						Component fromGoalComponent = panel.getInput("fromGoal");
						Component toGoalComponent = panel.getInput("toGoal");
						ComponentManipulator cm = ComponentManipulators
								.getManipulator(fromGoalComponent);
						cm.setModel(fromGoalComponent, new CombinedListModel(panel.getGoalNames(),
								existingGoalRelation.getFromGoal().getName(), true));
						cm = ComponentManipulators.getManipulator(toGoalComponent);
						cm.setModel(toGoalComponent, new CombinedListModel(panel.getGoalNames(),
								existingGoalRelation.getToGoal().getName(), true));
					}
				} else if (event.getObject() instanceof Annotation) {
					Annotation updatedAnnotation = (Annotation) event.getObject();
					if (event instanceof DeletedEntityEvent) {
						if (existingGoalRelation.getAnnotations().contains(updatedAnnotation)) {
							existingGoalRelation.getAnnotations().remove(updatedAnnotation);
							updatedGoalRelation = existingGoalRelation;
						}
					} else if (updatedAnnotation.getAnnotatables().contains(existingGoalRelation)) {
						for (Annotatable annotatable : updatedAnnotation.getAnnotatables()) {
							if (annotatable.equals(existingGoalRelation)) {
								updatedGoalRelation = (GoalRelation) annotatable;
								break;
							}
						}
					}
				}
				if ((updatedGoalRelation != null)
						&& updatedGoalRelation.equals(existingGoalRelation)) {
					// TODO: check the input fields to see if the user has made
					// a change before reseting the object and updating the
					// input fields.
					panel.setTargetObject(updatedGoalRelation);
					panel.setInputValue("annotations", updatedGoalRelation);
				}

				// if another goal was added or removed, update the to/from
				// selectors
				if (event.getObject() instanceof Goal) {
					// update the selection lists
					Component fromGoalComponent = panel.getInput("fromGoal");
					Component toGoalComponent = panel.getInput("toGoal");
					ComponentManipulator cm = ComponentManipulators
							.getManipulator(fromGoalComponent);
					cm.setModel(fromGoalComponent, new CombinedListModel(panel.getGoalNames(),
							existingGoalRelation.getFromGoal().getName(), true));
					cm = ComponentManipulators.getManipulator(toGoalComponent);
					cm.setModel(toGoalComponent, new CombinedListModel(panel.getGoalNames(),
							existingGoalRelation.getToGoal().getName(), true));
				}
			}
		}
	}
}
