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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import nextapp.echo2.app.SelectField;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;

import org.apache.log4j.Logger;
import org.hibernate.validator.InvalidStateException;
import org.hibernate.validator.InvalidValue;

import echopointng.ComboBox;
import echopointng.text.StringDocumentEx;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.GoalContainer;
import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.ProjectTeam;
import edu.harvard.fas.rregan.requel.project.ProjectUserRole;
import edu.harvard.fas.rregan.requel.project.Stakeholder;
import edu.harvard.fas.rregan.requel.project.StakeholderPermission;
import edu.harvard.fas.rregan.requel.project.StakeholderPermissionType;
import edu.harvard.fas.rregan.requel.project.UserStakeholder;
import edu.harvard.fas.rregan.requel.project.command.DeleteStakeholderCommand;
import edu.harvard.fas.rregan.requel.project.command.EditUserStakeholderCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.ui.annotation.AnnotationsTable;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.user.UserSet;
import edu.harvard.fas.rregan.uiframework.navigation.event.DeletedEntityEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.UpdateEntityEvent;
import edu.harvard.fas.rregan.uiframework.panel.editor.CheckBoxTreeSet;
import edu.harvard.fas.rregan.uiframework.panel.editor.CheckBoxTreeSetModel;
import edu.harvard.fas.rregan.uiframework.panel.editor.CombinedListModel;
import edu.harvard.fas.rregan.uiframework.panel.editor.CombinedTextListModel;

/**
 * Panel for creating or editing a user stakeholder.
 * 
 * @author ron
 */
public class UserStakeholderEditorPanel extends AbstractRequelProjectEditorPanel {
	private static final Logger log = Logger.getLogger(NonUserStakeholderEditorPanel.class);

	static final long serialVersionUID = 0L;

	/**
	 * The name to use in the StakeholderEditorPanel.properties file to set the
	 * label of the name field. If the property is undefined "Name" is used.
	 */
	public static final String PROP_LABEL_NAME = "Name.Label";

	/**
	 * The name to use in the StakeholderEditorPanel.properties file to set the
	 * label of the user field. If the property is undefined "User" is used.
	 */
	public static final String PROP_LABEL_USER = "User.Label";

	/**
	 * The name to use in the StakeholderEditorPanel.properties file to set the
	 * label of the team field. If the property is undefined "Team" is used.
	 */
	public static final String PROP_LABEL_TEAM = "Team.Label";

	/**
	 * The name to use in the StakeholderEditorPanel.properties file to set the
	 * label of the team field. If the property is undefined "Team" is used.
	 */
	public static final String PROP_LABEL_PERMISSIONS = "Permissions.Label";

	private final UserRepository userRepository;
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
	public UserStakeholderEditorPanel(CommandHandler commandHandler,
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
	public UserStakeholderEditorPanel(String resourceBundleName, CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, UserRepository userRepository,
			ProjectRepository projectRepository) {
		super(resourceBundleName, Stakeholder.class, commandHandler, projectCommandFactory,
				projectRepository);
		this.userRepository = userRepository;
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
	 * @see edu.harvard.fas.rregan.uiframework.panel.AbstractPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		if (getStakeholder() != null) {
			String msgPattern = getResourceBundleHelper(getLocale()).getString(
					PROP_EXISTING_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE,
							"Stakeholder: {0}"));
			return MessageFormat.format(msgPattern, getStakeholder().getName(),
					getProjectOrDomain().getName());
		} else {
			String msg = getResourceBundleHelper(getLocale()).getString(
					PROP_NEW_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE,
							"New Stakeholder"));
			return msg;
		}
	}

	@Override
	public void setup() {
		super.setup();
		UserStakeholder stakeholder = getStakeholder();
		if (stakeholder != null) {
			addInput("name", PROP_LABEL_NAME, "Name", new TextField(), new StringDocumentEx(
					stakeholder.getName()));
			addInput("user", PROP_LABEL_USER, "User", new SelectField(), new CombinedListModel(
					getProjectUsersUsernames(), (stakeholder.getUser() != null ? stakeholder
							.getUser().getUsername() : ""), true));
			addInput("teamName", PROP_LABEL_TEAM, "Team", new ComboBox(),
					new CombinedTextListModel(getProjectTeamNames(),
							(stakeholder.getTeam() != null ? stakeholder.getTeam().getName() : "")));
			addMultiRowInput("goals", GoalsTable.PROP_LABEL_GOALS, "Goals", new GoalsTable(this,
					getResourceBundleHelper(getLocale()), getProjectCommandFactory(),
					getCommandHandler()), stakeholder);
			addInput("stakeholderPermissions", PROP_LABEL_PERMISSIONS, "Stakeholder Permissions",
					new CheckBoxTreeSet(), createStakeholderPermissionsSelectionTreeModel(
							getProjectRepository().findAvailableStakeholderPermissions(),
							stakeholder));
			addMultiRowInput("annotations", AnnotationsTable.PROP_LABEL_ANNOTATIONS, "Annotations",
					new AnnotationsTable(this, getResourceBundleHelper(getLocale())), stakeholder);
		} else {
			addInput("name", PROP_LABEL_NAME, "Name", new TextField(), new StringDocumentEx());
			addInput("user", PROP_LABEL_USER, "User", new SelectField(), new CombinedListModel(
					getProjectUsersUsernames(), "", true));
			addInput("teamName", PROP_LABEL_TEAM, "Team", new ComboBox(),
					new CombinedTextListModel(getProjectTeamNames(), ""));
			addMultiRowInput("goals", GoalsTable.PROP_LABEL_GOALS, "Goals", new GoalsTable(this,
					getResourceBundleHelper(getLocale()), getProjectCommandFactory(),
					getCommandHandler()), null);
			addInput("stakeholderPermissions", PROP_LABEL_PERMISSIONS, "Stakeholder Permissions",
					new CheckBoxTreeSet(), createStakeholderPermissionsSelectionTreeModel(
							getProjectRepository().findAvailableStakeholderPermissions(), null));
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
			EditUserStakeholderCommand command = getProjectCommandFactory()
					.newEditUserStakeholderCommand();
			command.setStakeholder(getStakeholder());
			command.setProjectOrDomain(getProjectOrDomain());
			command.setEditedBy(getCurrentUser());
			command.setName(getInputValue("name", String.class));
			command.setUsername(getInputValue("user", String.class));
			command.setTeamName(getInputValue("teamName", String.class));
			command.setStakeholderPermissions(getStakeholderPermissionKeys(getInputValue(
					"stakeholderPermissions", Set.class), getProjectRepository()
					.findAvailableStakeholderPermissions()));
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

	private Set<String> getProjectTeamNames() {
		Set<String> teamNames = new TreeSet<String>();
		teamNames.add("");
		for (ProjectTeam team : getProjectOrDomain().getTeams()) {
			teamNames.add(team.getName());
		}
		return teamNames;
	}

	private Set<String> getProjectUsersUsernames() {
		Set<String> usernames = new TreeSet<String>();
		usernames.add("");
		UserSet projectUsers = getUserRepository().findUsersForRole(ProjectUserRole.class);
		for (User user : projectUsers) {
			usernames.add(user.getUsername());
		}
		return usernames;
	}

	private CheckBoxTreeSetModel createStakeholderPermissionsSelectionTreeModel(
			Set<StakeholderPermission> availableStakeholderPermissions, UserStakeholder stakeholder) {
		Set<String> optionPaths = new TreeSet<String>(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return o2.compareTo(o1);
			}
		});
		Set<String> selections = new HashSet<String>();

		for (StakeholderPermission permission : availableStakeholderPermissions) {
			optionPaths.add(generatePermissionPath(permission));
			if ((stakeholder != null) && stakeholder.hasPermission(permission)) {
				selections.add(generatePermissionPath(permission));
			}
		}
		return new CheckBoxTreeSetModel(optionPaths,
				getGrantablePermissionPaths(getProjectRepository()
						.findAvailableStakeholderPermissions()), selections, true);
	}

	private Set<String> getGrantablePermissionPaths(
			Set<StakeholderPermission> availableStakeholderPermissions) {
		Set<Class<?>> grantable = new HashSet<Class<?>>();
		Set<String> grantablePermissionPaths = new HashSet<String>();
		User user = (User) getApp().getUser();
		if (getProjectOrDomain() instanceof Project) {
			Project project = (Project) getProjectOrDomain();
			UserStakeholder currentStakeholder = project.getUserStakeholder(user);
			if (currentStakeholder != null) {
				for (StakeholderPermission permission : availableStakeholderPermissions) {
					if (StakeholderPermissionType.Grant.equals(permission.getPermissionType())) {
						if (currentStakeholder.hasPermission(permission)) {
							grantable.add(permission.getEntityType());
						}
					}
				}
				for (StakeholderPermission permission : availableStakeholderPermissions) {
					if (grantable.contains(permission.getEntityType())
							|| project.getCreatedBy().equals(user)) {
						grantablePermissionPaths.add(generatePermissionPath(permission));
					}
				}
			}
		}
		return grantablePermissionPaths;
	}

	private Set<String> getStakeholderPermissionKeys(Set<String> selectedPaths,
			Set<StakeholderPermission> availableStakeholderPermissions) {
		log.debug("selectedPaths = " + selectedPaths);
		Set<String> stakeholderPermissionKeys = new HashSet<String>();
		for (StakeholderPermission permission : availableStakeholderPermissions) {
			String permissionPath = generatePermissionPath(permission);
			if (selectedPaths.contains(permissionPath)) {
				stakeholderPermissionKeys.add(permission.getPermissionKey());
				log.debug("adding " + permission);
			}
		}
		return stakeholderPermissionKeys;
	}

	private String generatePermissionPath(StakeholderPermission permission) {
		String entityTypeName = permission.getEntityType().getSimpleName();
		String permissionTypeName = permission.getPermissionType().toString();
		return (entityTypeName + "/" + permissionTypeName);
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

	private UserStakeholder getStakeholder() {
		if (getTargetObject() instanceof UserStakeholder) {
			return (UserStakeholder) getTargetObject();
		}
		return null;
	}

	private UserRepository getUserRepository() {
		return userRepository;
	}

	private static class UpdateListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final UserStakeholderEditorPanel panel;

		private UpdateListener(UserStakeholderEditorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (panel.deleted) {
				return;
			}
			UserStakeholder existingStakeholder = panel.getStakeholder();
			if ((e instanceof UpdateEntityEvent) && (existingStakeholder != null)) {
				UpdateEntityEvent event = (UpdateEntityEvent) e;
				UserStakeholder updatedStakeholder = null;
				if (event.getObject() instanceof Stakeholder) {
					updatedStakeholder = (UserStakeholder) event.getObject();
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
								updatedStakeholder = (UserStakeholder) gc;
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
								updatedStakeholder = (UserStakeholder) annotatable;
								break;
							}
						}
					}
				}
				if ((updatedStakeholder != null) && updatedStakeholder.equals(existingStakeholder)) {
					// TODO: check the input fields to see if the user has made
					// a change before resetting the object and updating the
					// input fields.
					panel.setInputValue("user",
							(updatedStakeholder.getUser() != null ? updatedStakeholder.getUser()
									.getUsername() : ""));
					panel.setInputValue("teamName",
							(updatedStakeholder.getTeam() != null ? updatedStakeholder.getTeam()
									.getName() : ""));
					// TODO: this causes an error for echo2 rendering of the
					// tree
					// panel.setInputValue("stakeholderPermissions", panel
					// .getStakeholderPermissionKeys(updatedStakeholder));
					panel.setInputValue("goals", updatedStakeholder);
					panel.setInputValue("annotations", updatedStakeholder);
					panel.setTargetObject(updatedStakeholder);
				}
			}
		}
	}
}
