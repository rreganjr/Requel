/*
 * $Id: SystemAdminUserRole.java,v 1.10 2009/01/10 11:08:59 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.user;

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
@DiscriminatorValue(value = "edu.harvard.fas.rregan.requel.user.SystemAdminUserRole")
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
