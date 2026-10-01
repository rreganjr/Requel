/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.user.impl;

import java.util.HashSet;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import com.rreganjr.platform.identity.AssistantRole;
import com.rreganjr.requel.user.UserRolePermission;

/**
 * Issue #260: marks a user as an assistant identity - the account one assistant writes its
 * annotations as. {@link com.rreganjr.platform.identity.User#isAssistant} recognises it, and such a
 * user cannot log in ({@link UserImpl#isPassword}).
 */
@Entity
@DiscriminatorValue(value = "com.rreganjr.requel.user.AssistantUserRole")
@XmlRootElement(name = "assistantUserRole", namespace = "http://www.rreganjr.com/requel")
@XmlType(name = "assistantUserRole", namespace = "http://www.rreganjr.com/requel")
public class AssistantUserRole extends AbstractUserRole implements AssistantRole {
	static final long serialVersionUID = 0L;

	static {
		AbstractUserRole.userRoleTypes.add(AssistantUserRole.class);
		AbstractUserRole.userRoleTypePermissions.put(AssistantUserRole.class,
				new HashSet<UserRolePermission>());
	}

	public AssistantUserRole() {
		super();
	}
}
