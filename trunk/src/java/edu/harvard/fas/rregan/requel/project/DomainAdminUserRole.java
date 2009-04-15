/*
 * $Id: DomainAdminUserRole.java,v 1.11 2009/03/29 02:08:32 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import edu.harvard.fas.rregan.requel.user.AbstractUserRole;

/**
 * @author ron
 */
@Entity
@DiscriminatorValue(value = "edu.harvard.fas.rregan.requel.project.DomainAdminUserRole")
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
