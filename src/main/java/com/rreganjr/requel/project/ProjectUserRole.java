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

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import jakarta.persistence.*;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import org.hibernate.annotations.SortNatural;
import org.xml.sax.SAXException;


import org.glassfish.jaxb.runtime.v2.runtime.unmarshaller.Patcher;
import org.glassfish.jaxb.runtime.v2.runtime.unmarshaller.UnmarshallingContext;

import com.rreganjr.requel.project.impl.ProjectImpl;
import com.rreganjr.requel.user.AbstractUserRole;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.UserRolePermission;
import com.rreganjr.requel.user.exception.NoSuchUserException;
import com.rreganjr.requel.user.impl.UserImpl;
import com.rreganjr.requel.utils.jaxb.UnmarshallerListener;

/**
 * A ProjectUserRole represents a user authorized to work with Projects in the
 * system and maintains a user's specific project data.
 * 
 * @author ron
 */
@Entity
@DiscriminatorValue(value = "com.rreganjr.requel.project.ProjectUserRole")
@XmlRootElement(name = "projectUserRole", namespace = "http://www.rreganjr.com/requel")
@XmlType(name = "projectUserRole", namespace = "http://www.rreganjr.com/requel")
public class ProjectUserRole extends AbstractUserRole {
	static final long serialVersionUID = 0L;

	public static final UserRolePermission createProjects = new UserRolePermission(
			ProjectUserRole.class, "createProjects");
	public static final UserRolePermission inviteUsers = new UserRolePermission(
			ProjectUserRole.class, "inviteUsers");

	static {
		AbstractUserRole.userRoleTypes.add(ProjectUserRole.class);
		AbstractUserRole.userRoleTypePermissions.put(ProjectUserRole.class,
				new HashSet<UserRolePermission>());
		AbstractUserRole.userRoleTypePermissions.get(ProjectUserRole.class).add(createProjects);
		// AbstractUserRole.userRoleTypePermissions.get(ProjectUserRole.class).add(inviteUsers);
	}

	private User user;
	private Set<Project> activeProjects = new TreeSet<Project>();

	/**
	 * @param user -
	 *            the user this role is assigned to
	 */
	public ProjectUserRole(User user) {
		super();
		setUser(user);
	}

	protected ProjectUserRole() {
		// for hibernate
	}

	/**
	 * @return the user this role is assigned to
	 */
	@ManyToOne(targetEntity = UserImpl.class, cascade = { CascadeType.PERSIST, CascadeType.REFRESH })
	protected User getUser() {
		return user;
	}

	/**
	 * set the user this role is assigned to
	 * 
	 * @param user -
	 *            the user this role is assigned to
	 */
	protected void setUser(User user) {
		this.user = user;
	}

	/**
	 * @return
	 */
	@ManyToMany(targetEntity = ProjectImpl.class, cascade = { CascadeType.PERSIST,
			CascadeType.REFRESH }, fetch = FetchType.EAGER)
	@SortNatural
	public Set<Project> getActiveProjects() {
		return activeProjects;
	}

	protected void setActiveProjects(Set<Project> activeProjects) {
		this.activeProjects = activeProjects;
	}

	/**
	 * @return true if this user is authorized to create new projects.
	 */
	public boolean canCreateProjects() {
		return hasUserRolePermission(createProjects);
	}

	/**
	 * @return true if this user is authorized to invite non-users to become
	 *         users of the system.
	 */
	public boolean canInviteUsers() {
		return hasUserRolePermission(inviteUsers);
	}

	@Override
	public String toString() {
		return super.toString() + ": " + getUser().getUsername();
	}

	private Integer tmpHashCode = null;

	@Override
	public int hashCode() {
		if (tmpHashCode == null) {
			if (getId() != null) {
				tmpHashCode = Integer.valueOf(getId().hashCode());
			} else {
				final int prime = 31;
				int result = 1;
				result = prime * result + ((getRoleName() == null) ? 0 : getRoleName().hashCode());
				result = prime * result + ((getUser() == null) ? 0 : getUser().hashCode());
				tmpHashCode = Integer.valueOf(result);
			}
		}
		return tmpHashCode.intValue();
	}

	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj)) {
			return false;
		}
		final ProjectUserRole other = (ProjectUserRole) obj;
		if (getUser() == null) {
			if (other.getUser() != null) {
				return false;
			}
		} else if (!getUser().equals(other.getUser())) {
			return false;
		}
		return true;
	}

	/**
	 * This is for JAXB to patchup the parent/child relationship.
	 * 
	 * @param userRepository
	 * @param parent
	 * @see UnmarshallerListener
	 */
	public void afterUnmarshal(final UserRepository userRepository, Object parent) {
		setUser((User) parent);
		UnmarshallingContext.getInstance().addPatcher(new Patcher() {
			@Override
			public void run() throws SAXException {
				if (getUser() != null) {
					try {
						User existingUser = userRepository.findUserByUsername(getUser()
								.getUsername());
						setUser(existingUser);
					} catch (NoSuchUserException e) {
					}
				} else {
					throw new SAXException("ProjectUserRole missing User");
				}
			}
		});
	}
}
