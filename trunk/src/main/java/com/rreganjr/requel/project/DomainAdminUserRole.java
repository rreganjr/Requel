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
package com.rreganjr.requel.project;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.rreganjr.requel.user.AbstractUserRole;

/**
 * @author ron
 */
@Entity
@DiscriminatorValue(value = "com.rreganjr.requel.project.DomainAdminUserRole")
@XmlRootElement(name = "domainAdminUserRole", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "domainAdminUserRole", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public class DomainAdminUserRole extends AbstractUserRole {
	static final long serialVersionUID = 0L;

	static {
		// TODO: domain administration is disabled, to add it back in uncomment
		// the following
		/*
		 * AbstractUserRole.userRoleTypes.add(DomainAdminUserRole.class);
		 * AbstractUserRole.userRoleTypePermissions.put(DomainAdminUserRole.class,
		 * new HashSet<UserRolePermission>());
		 */
	}

	/**
	 * 
	 */
	public DomainAdminUserRole() {
		super();
	}
}
