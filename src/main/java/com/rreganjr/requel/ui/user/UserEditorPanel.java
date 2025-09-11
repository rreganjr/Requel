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
package com.rreganjr.requel.ui.user;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import nextapp.echo2.app.Component;
import nextapp.echo2.app.PasswordField;
import nextapp.echo2.app.TextField;

import org.apache.log4j.Logger;
import com.rreganjr.validator.InvalidStateException;
import com.rreganjr.validator.InvalidValue;

import echopointng.ComboBox;
import echopointng.text.StringDocumentEx;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.repository.EntityException;
import com.rreganjr.requel.user.AbstractUserRole;
import com.rreganjr.requel.user.Organization;
import com.rreganjr.requel.user.SystemAdminUserRole;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.UserRole;
import com.rreganjr.requel.user.UserRolePermission;
import com.rreganjr.requel.user.command.EditUserCommand;
import com.rreganjr.requel.user.command.UserCommandFactory;
import net.sf.echopm.login.InitAppEvent;
import net.sf.echopm.navigation.event.UpdateEntityEvent;
import net.sf.echopm.panel.editor.AbstractEditorPanel;
import net.sf.echopm.panel.editor.CheckBoxTreeSet;
import net.sf.echopm.panel.editor.CheckBoxTreeSetModel;
import net.sf.echopm.panel.editor.CombinedTextListModel;

/**
 * @author ron
 */
public class UserEditorPanel extends AbstractEditorPanel {
	private static final Logger log = Logger.getLogger(UserEditorPanel.class);
	static final long serialVersionUID = 0L;

	/**
	 * The name to use in the UserEditorPanel.properties file to set the label
	 * of the username field. If the property is undefined "Username" is used.
	 */
	public static final String PROP_LABEL_USERNAME = "Username.Label";

	/**
	 * The name to use in the UserEditorPanel.properties file to set the label
	 * of the password field. If the property is undefined "Password" is used.
	 */
	public static final String PROP_LABEL_PASSWORD = "Password.Label";

	/**
	 * The name to use in the UserEditorPanel.properties file to set the label
	 * of the second password field. If the property is undefined "Retype
	 * Password" is used.
	 */
	public static final String PROP_LABEL_RE_PASSWORD = "RePassword.Label";

	/**
	 * The name to use in the UserEditorPanel.properties file to set the label
	 * of the name field. If the property is undefined "Name" is used.
	 */
	public static final String PROP_LABEL_NAME = "Name.Label";

	/**
	 * The name to use in the UserEditorPanel.properties file to set the label
	 * of the organization field. If the property is undefined "Organization" is
	 * used.
	 */
	public static final String PROP_LABEL_ORGANIZATION = "Organization.Label";

	/**
	 * The name to use in the UserEditorPanel.properties file to set the label
	 * of the email address field. If the property is undefined "Email Address"
	 * is used.
	 */
	public static final String PROP_LABEL_EMAIL = "EmailAddress.Label";

	/**
	 * The name to use in the UserEditorPanel.properties file to set the label
	 * of the phone number field. If the property is undefined "Phone Number" is
	 * used.
	 */
	public static final String PROP_LABEL_PHONE = "PhoneNumber.Label";

	/**
	 * The name to use in the UserEditorPanel.properties file to set the label
	 * of the user roles checkbox fields. If the property is undefined "User
	 * Roles" is used.
	 */
	public static final String PROP_LABEL_ROLES = "UserRoles.Label";

	/**
	 * The name to use in the UserEditorPanel.properties file to set the label
	 * of the project user permissions checkbox fields. If the property is
	 * undefined "Project User Permissions" is used.
	 */
	public static final String PROP_LABEL_PROJECT_USER_PERMS = "ProjectUserPermissions.Label";

	/**
	 * The name to use in the UserEditorPanel.properties file to set the label
	 * of the checkbox to control whether the user(s) that use this account can
	 * change the values field.
	 */
	public static final String PROP_LABEL_EDITABLE = "CanBeEdited.Label";

	private final UserCommandFactory userCommandFactory;
	private final UserRepository userRepository;
	private final CommandHandler commandHandler;

	/**
	 * @param commandHandler
	 * @param userCommandFactory
	 * @param userRepository
	 */
	public UserEditorPanel(CommandHandler commandHandler, UserCommandFactory userCommandFactory,
			UserRepository userRepository) {
		this(UserEditorPanel.class.getName(), commandHandler, userCommandFactory, userRepository);
	}

	/**
	 * @param resourceBundleName
	 * @param commandHandler
	 * @param userCommandFactory
	 * @param userRepository
	 */
	public UserEditorPanel(String resourceBundleName, CommandHandler commandHandler,
			UserCommandFactory userCommandFactory, UserRepository userRepository) {
		super(resourceBundleName, User.class);
		this.userCommandFactory = userCommandFactory;
		this.userRepository = userRepository;
		this.commandHandler = commandHandler;
	}

	@Override
	protected boolean isShowDelete() {
		return false;
	}

	/**
	 * If the editor is editing an existing user the title specified in the
	 * properties file as PROP_EXISTING_OBJECT_PANEL_TITLE If that property is
	 * not set it then tries the standard PROP_PANEL_TITLE and if that does not
	 * exist it defaults to:<br>
	 * "Edit User: {0}"<br>
	 * Valid variables are:<br>
	 * {0} - username<br>
	 * {1} - name <br>
	 * For new users it first tries PROP_NEW_OBJECT_PANEL_TITLE, then
	 * PROP_PANEL_TITLE and finally defaults to:<br>
	 * "New User"<br>
	 * 
	 * @see AbstractEditorPanel.PROP_EXISTING_OBJECT_PANEL_TITLE
	 * @see AbstractEditorPanel.PROP_NEW_OBJECT_PANEL_TITLE
	 * @see Panel.PROP_PANEL_TITLE
	 * @see net.sf.echopm.panel.AbstractPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		if (getUser() != null) {
			String msgPattern = getResourceBundleHelper(getLocale()).getString(
					PROP_EXISTING_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE,
							"Edit User: {0}"));
			return MessageFormat.format(msgPattern, getUser().getUsername(), getUser().getName());
		} else {
			String msg = getResourceBundleHelper(getLocale()).getString(
					PROP_NEW_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE, "New User"));
			return msg;
		}
	}

	@Override
	public void setup() {
		super.setup();
		User editingUser = (User) getApp().getUser();
		User user;
		if (editingUser.hasRole(SystemAdminUserRole.class)) {
			user = getUser();
		} else {
			// a non-admin user can only edit their own account info.
			user = editingUser;
		}
		if (user != null) {
			Component username = addInput("username", PROP_LABEL_USERNAME, "Username",
					new TextField(), new StringDocumentEx(user.getUsername()));
			addInput("password", PROP_LABEL_PASSWORD, "Password", new PasswordField(),
					new StringDocumentEx());
			addInput("repassword", PROP_LABEL_RE_PASSWORD, "Retype Password", new PasswordField(),
					new StringDocumentEx());
			addInput("name", PROP_LABEL_NAME, "Name", new TextField(), new StringDocumentEx(user
					.getName()));
			addInput("organizationName", PROP_LABEL_ORGANIZATION, "Organization", new ComboBox(),
					new CombinedTextListModel(getUserRepository().getOrganizationNames(), user
							.getOrganization().getName()));
			addInput("emailAddress", PROP_LABEL_EMAIL, "Email Address", new TextField(),
					new StringDocumentEx(user.getEmailAddress()));
			addInput("phoneNumber", PROP_LABEL_PHONE, "Phone Number", new TextField(),
					new StringDocumentEx(user.getPhoneNumber()));
			if (editingUser.hasRole(SystemAdminUserRole.class)) {
				username.setEnabled(true);
				addInput("userRoles", PROP_LABEL_ROLES, "User Roles", new CheckBoxTreeSet(),
						createUserRoleSelectionTreeModel(getUserRepository().findUserRoleTypes(),
								user));
				// addInput("editable", PROP_LABEL_EDITABLE, "Editable User?",
				// new CheckBox(),
				// new ToggleButtonModelEx(user.isEditable()));
			} else {
				username.setEnabled(false);
			}
		} else {
			addInput("username", PROP_LABEL_USERNAME, "Username", new TextField(),
					new StringDocumentEx());
			addInput("password", PROP_LABEL_PASSWORD, "Password", new PasswordField(),
					new StringDocumentEx());
			addInput("repassword", PROP_LABEL_RE_PASSWORD, "Retype Password", new PasswordField(),
					new StringDocumentEx());
			addInput("name", PROP_LABEL_NAME, "Name", new TextField(), new StringDocumentEx());
			addInput("organizationName", PROP_LABEL_ORGANIZATION, "Organization", new ComboBox(),
					new CombinedTextListModel(getUserRepository().getOrganizationNames(), ""));
			addInput("emailAddress", PROP_LABEL_EMAIL, "Email Address", new TextField(),
					new StringDocumentEx());
			addInput("phoneNumber", PROP_LABEL_PHONE, "Phone Number", new TextField(),
					new StringDocumentEx());
			addInput("userRoles", PROP_LABEL_ROLES, "User Roles", new CheckBoxTreeSet(),
					createUserRoleSelectionTreeModel(getUserRepository().findUserRoleTypes(), null));
			// addInput("editable", PROP_LABEL_EDITABLE, "Editable User?", new
			// CheckBox(),
			// new ToggleButtonModelEx(true));
		}
	}

	@Override
	public void cancel() {
		super.cancel();
	}

	@Override
	public void save() {
		try {
			super.save();
			User editedBy = (User) getApp().getUser();
			EditUserCommand command = getUserCommandFactory().newEditUserCommand();
			command.setUser(getUser());
			if (editedBy.hasRole(SystemAdminUserRole.class)) {
				command.setUsername(getInputValue("username", String.class));
				Set<String> userRoles = getInputValue("userRoles", Set.class);
				command.setUserRoleNames(getUserRoleNames(userRoles, getUserRepository()
						.findUserRoleTypes()));
				command.setUserRolePermissionNames(getUserRolePermissionNames(userRoles,
						getUserRepository().findUserRoleTypes()));
				command.setEditable(Boolean.TRUE);
			}
			command.setPassword(getInputValue("password", String.class));
			command.setRepassword(getInputValue("repassword", String.class));
			command.setName(getInputValue("name", String.class));
			command.setEmailAddress(getInputValue("emailAddress", String.class));
			command.setPhoneNumber(getInputValue("phoneNumber", String.class));
			command.setOrganizationName(getInputValue("organizationName", String.class));
			command.setEditedBy(editedBy);
			command = getCommandHandler().execute(command);
			User user = command.getUser();
			setValid(true);
			if (getApp().getUser().equals(user)) {
				getEventDispatcher().dispatchEvent(new InitAppEvent(this, user));
			}
			getEventDispatcher().dispatchEvent(new UpdateEntityEvent(this, user));
		} catch (EntityException e) {
			if ((e.getCause() != null) && (e.getCause() instanceof InvalidStateException)) {
				InvalidStateException ise = (InvalidStateException) e.getCause();
				for (InvalidValue invalidValue : ise.getInvalidValues()) {
					String propertyName = invalidValue.getPropertyName();
					if ("hashedPassword".equals(propertyName)) {
						propertyName = "password";
					}
					if (Organization.class.isAssignableFrom(invalidValue.getBeanClass())
							&& propertyName.equals("name")) {
						propertyName = "organizationName";
					}
					setValidationMessage(propertyName, invalidValue.getMessage());
				}
			} else if ((e.getEntityPropertyNames() != null)
					&& (e.getEntityPropertyNames().length > 0)) {
				for (String propertyName : e.getEntityPropertyNames()) {
					setValidationMessage(propertyName, e.getMessage());
				}
			} else {
				setGeneralMessage(e.toString());
			}
		} catch (Exception e) {
			log.error("could not save the user: " + e, e);
			setGeneralMessage("Could not save: " + e);
		}
	}

	private Set<String> getUserRoleNames(Set<String> selectedPaths,
			Set<Class<? extends UserRole>> userRoleTypes) {
		log.debug("selectedPaths = " + selectedPaths);
		Set<String> userRoleNames = new HashSet<String>();
		for (Class<? extends UserRole> userRoleType : userRoleTypes) {
			String userRoleName = AbstractUserRole.getRoleName(userRoleType);
			log.debug("userRoleName = " + userRoleName);
			if (selectedPaths.contains(userRoleName)) {
				userRoleNames.add(userRoleName);
				log.debug("adding " + userRoleName);
			}
		}
		return userRoleNames;
	}

	private Map<String, Set<String>> getUserRolePermissionNames(Set<String> selectedPaths,
			Set<Class<? extends UserRole>> userRoleTypes) {
		log.debug("selectedPaths = " + selectedPaths);
		Map<String, Set<String>> userRolePermissionNames = new HashMap<String, Set<String>>();
		for (Class<? extends UserRole> userRoleType : userRoleTypes) {
			String userRoleName = AbstractUserRole.getRoleName(userRoleType);
			log.debug("userRoleName = " + userRoleName);
			userRolePermissionNames.put(userRoleName, new HashSet<String>());
			if (selectedPaths.contains(userRoleName)) {
				for (UserRolePermission userRolePermission : getUserRepository()
						.findUserRolePermissions(userRoleType)) {
					log.debug("userRolePermission = " + userRolePermission);
					if (selectedPaths.contains(userRoleName + "/" + userRolePermission.getName())) {
						log.debug("adding " + userRolePermission);
						userRolePermissionNames.get(userRoleName).add(userRolePermission.getName());
					}
				}
			}
		}
		return userRolePermissionNames;
	}

	private CheckBoxTreeSetModel createUserRoleSelectionTreeModel(
			Set<Class<? extends UserRole>> userRoleTypes, User user) {
		Set<String> optionPaths = new TreeSet<String>(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return o2.compareTo(o1);
			}
		});
		Set<String> initialSelection = new HashSet<String>();

		for (Class<? extends UserRole> userRoleType : userRoleTypes) {
			String userRoleTypePath = AbstractUserRole.getRoleName(userRoleType);
			optionPaths.add(userRoleTypePath);
			if ((user != null) && user.hasRole(userRoleType)) {
				initialSelection.add(userRoleTypePath);
			}
			for (UserRolePermission permission : getUserRepository().findUserRolePermissions(
					userRoleType)) {
				String userRolePermissionPath = userRoleTypePath + "/" + permission.getName();
				optionPaths.add(userRolePermissionPath);
				if ((user != null) && user.hasRole(userRoleType)) {
					UserRole role = user.getRoleForType(userRoleType);
					if (role.hasUserRolePermission(permission)) {
						initialSelection.add(userRolePermissionPath);
					}
				}
			}
		}
		return new CheckBoxTreeSetModel(optionPaths, initialSelection);
	}

	private CommandHandler getCommandHandler() {
		return commandHandler;
	}

	private UserCommandFactory getUserCommandFactory() {
		return userCommandFactory;
	}

	private UserRepository getUserRepository() {
		return userRepository;
	}

	private User getUser() {
		return (User) getTargetObject();
	}
}
