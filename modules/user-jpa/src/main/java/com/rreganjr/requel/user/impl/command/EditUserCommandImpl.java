/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025, 2026 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.user.impl.command;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresSystemRole;
import com.rreganjr.validator.EntityValidationException;
import com.rreganjr.requel.user.impl.AbstractUserRole;
import com.rreganjr.requel.user.Organization;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.UserRole;
import com.rreganjr.requel.user.UserRolePermission;
import com.rreganjr.requel.user.command.EditUserCommand;
import com.rreganjr.requel.user.exception.NoSuchOrganizationException;
import com.rreganjr.requel.user.impl.OrganizationImpl;
import com.rreganjr.requel.user.impl.UserImpl;

/**
 * @author ron
 */
@Component("editUserCommand")
@Scope("prototype")
public class EditUserCommandImpl extends AbstractUserCommand implements EditUserCommand, AuthorizableCommand {

	private User user;
	private String username;
	private String password;
	private String repassword;
	private String name;
	private String emailAddress;
	private String phoneNumber;
	private String organizationName;
	private Boolean editable = Boolean.TRUE;
	private Set<String> userRoleNames = new HashSet<String>();
	private boolean userRolesProvided = false;
	private Map<String, Set<String>> userRolePermissionNames = new HashMap<String, Set<String>>();
	private User editedBy;

	/**
	 * @param userRepository
	 */
	@Autowired
	public EditUserCommandImpl(UserRepository userRepository) {
		super(userRepository);
	}

	/**
	 * @see com.rreganjr.command.Command#execute()
	 */
	@Override
	public void execute() {
		boolean isAdmin = getEditedBy() != null && getEditedBy().hasRole(SystemAdminUserRole.class);
		UserImpl userImpl = (UserImpl) getUser();
		if (userImpl == null) {
			// Only admins can create new users; null editedBy = system bootstrap (initializers)
			if (!isAdmin && getEditedBy() != null) {
				throw new AuthorizationException("Only administrators can create user accounts.");
			}
			userImpl = createUser();
		} else {
			// Non-admins can only edit their own account
			if (!isAdmin && !userImpl.getUsername().equals(getEditedBy().getUsername())) {
				throw new AuthorizationException("You can only edit your own account.");
			}
			userImpl = updateUser(userImpl, isAdmin);
		}
		setUser(userImpl);
	}

	protected void validate() {
		if ((getPassword() != null) && !getPassword().equals(getRepassword())) {
			throw EntityValidationException.passwordAndRePasswordDontMatch(User.class);
		}
	}

	private UserImpl createUser() {
		// TODO: probably shouldn't be creating a user with a password, or should be required to change it on first login
		if (getPassword().equals(getRepassword())) {
			Organization organization = getOrCreateOrganization(getOrganizationName());
			UserImpl userImpl = new UserImpl(getUsername(), getPassword(),
					getName(), getEmailAddress(), getPhoneNumber(), organization, getEditable());
			updateRoles(userImpl);
			return getUserRepository().persist(userImpl);
		} else {
			throw EntityValidationException.passwordAndRePasswordDontMatch(User.class);
		}
	}

	private UserImpl updateUser(UserImpl userImpl, boolean isAdmin) {
		Organization organization = getOrCreateOrganization(getOrganizationName());
		userImpl.setName(getName());
		if (!StringUtils.isEmpty(getPassword())) {
			if (getPassword().equals(getRepassword())) {
				userImpl.resetPassword(getPassword());
			} else {
				throw EntityValidationException.passwordAndRePasswordDontMatch(User.class);
			}
		}
		userImpl.setEmailAddress(getEmailAddress());
		userImpl.setPhoneNumber(getPhoneNumber());
		userImpl.setOrganization(organization);
		if (isAdmin) {
			userImpl.setUsername(getUsername());
			userImpl.setEditable(getEditable());
			if (userRolesProvided) {
				updateRoles(userImpl);
			}
		}
		return getUserRepository().merge(userImpl);
	}

	private Organization getOrCreateOrganization(String organizationName) {
		if (organizationName == null || organizationName.isBlank()) {
			return null;
		}
		try {
			return getUserRepository().findOrganizationByName(organizationName);
		} catch (NoSuchOrganizationException e) {
			return getUserRepository().persist(new OrganizationImpl(organizationName));
		}
	}

	private void updateRoles(UserImpl userImpl) {
		for (Class<? extends UserRole> userRoleType : getUserRepository().findUserRoleTypes()) {
			if (getUserRoleNames().contains(userRoleType.getSimpleName())) {
				userImpl.grantRole(userRoleType);
				AbstractUserRole role = (AbstractUserRole) userImpl.getRoleForType(userRoleType);
				for (UserRolePermission permission : getUserRepository().findUserRolePermissions(userRoleType)) {
					Set<String> permissionNames = getUserRolePermissionNames().get(role.getRoleName());
					if (permissionNames != null) {
						if (permissionNames.contains(permission.getName())) {
							role.grantUserRolePermission(permission);
						} else {
							role.revokeUserRolePermission(permission);
						}
					}
				}
			} else if (userImpl.hasRole(userRoleType)) {
				userImpl.revokeRole(userRoleType);
			}
		}
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	protected String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	protected String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	protected String getRepassword() {
		return repassword;
	}

	public void setRepassword(String repassword) {
		this.repassword = repassword;
	}

	protected String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	protected String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	protected String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	protected String getOrganizationName() {
		return organizationName;
	}

	public void setOrganizationName(String organizationName) {
		this.organizationName = organizationName;
	}

	protected Boolean getEditable() {
		return editable;
	}

	public void setEditable(Boolean editable) {
		this.editable = editable;
	}

	protected Set<String> getUserRoleNames() {
		return userRoleNames;
	}

	public void setUserRoleNames(Set<String> userRoleNames) {
		this.userRoleNames = userRoleNames;
		this.userRolesProvided = true;
	}

	public void addUserRoleName(String userRoleName) {
		userRoleNames.add(userRoleName);
		this.userRolesProvided = true;
	}

	protected Map<String, Set<String>> getUserRolePermissionNames() {
		return userRolePermissionNames;
	}

	public void setUserRolePermissionNames(Map<String, Set<String>> userRolePermissionNames) {
		this.userRolePermissionNames = userRolePermissionNames;
	}

	public void addUserRolePermissionName(String userRoleName, String userRolePermissionName) {
		if (!userRolePermissionNames.containsKey(userRoleName)) {
			userRolePermissionNames.put(userRoleName, new HashSet<String>());
		}
		userRolePermissionNames.get(userRoleName).add(userRolePermissionName);
	}

	@Override
	public void setEditedBy(User editedBy) {
		this.editedBy = editedBy;
	}

	/**
	 * Satisfies {@link com.rreganjr.platform.command.EditCommand#setEditedBy} via
	 * {@link AuthorizableCommand}. The cast is safe: the HTTP dispatch path always
	 * sets a {@code com.rreganjr.requel.user.User} via CurrentUserCommandHandler.
	 */
	@Override
	public void setEditedBy(com.rreganjr.platform.identity.User editedBy) {
		this.editedBy = (User) editedBy;
	}

	public User getEditedBy() {
		return editedBy;
	}

	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		// Bootstrap: null editedBy = system initializer call (AdminUserInitializer, etc.) — skip auth.
		// execute() enforces the same rule: null editedBy is the only non-admin path allowed to create users.
		if (editedBy == null) {
			return null;
		}
		// Own-account edits require only authentication — return null to skip the role check.
		// Editing another account (or creating a new user) requires SystemAdminUserRole.
		if (user != null && user.getUsername().equals(editedBy.getUsername())) {
			return null;
		}
		return new RequiresSystemRole(SystemAdminUserRole.class);
	}
}
