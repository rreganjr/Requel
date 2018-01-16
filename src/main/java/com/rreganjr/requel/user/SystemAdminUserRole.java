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
package com.rreganjr.requel.user;

import java.util.HashSet;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Represents a user with system administration privilges. This role has no user
 * specific properties so a single instance can be shared by all users.
 * 
 * @author ron
 */
@Entity
@DiscriminatorValue(value = "com.rreganjr.requel.user.SystemAdminUserRole")
@XmlRootElement(name = "systemAdminUserRole", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "systemAdminUserRole", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public class SystemAdminUserRole extends AbstractUserRole {
	static final long serialVersionUID = 0L;

	static {
		AbstractUserRole.userRoleTypes.add(SystemAdminUserRole.class);
		AbstractUserRole.userRoleTypePermissions.put(SystemAdminUserRole.class,
				new HashSet<UserRolePermission>());
	}

	/**
	 * @param roleName
	 */
	public SystemAdminUserRole() {
		super();
	}
}
