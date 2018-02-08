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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * @author ron
 */
@Entity
@Table(name = "user_role_permissions", uniqueConstraints = { @UniqueConstraint(columnNames = {
		"name", "role_type" }) })
@XmlRootElement(name = "userPermission", namespace = "http://www.rreganjr.com/requel")
@XmlType(name = "userPermission", namespace = "http://www.rreganjr.com/requel")
public class UserRolePermission implements Comparable<UserRolePermission>, Serializable {
	static final long serialVersionUID = 0L;

	private Long id;
	private String userRoleType;
	private String name;

	/**
	 * @param userRoleType
	 * @param permissionName
	 */
	public UserRolePermission(Class<? extends UserRole> userRoleType, String permissionName) {
		setUserRoleType(userRoleType.getName());
		setName(permissionName);
	}

	protected UserRolePermission() {
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@XmlAttribute(name = "id")
	@XmlJavaTypeAdapter(IdAdapter.class)
	protected Long getId() {
		return id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return The name of the permission for display
	 */
	@Column(name = "name", nullable = false, length = 50)
	@XmlAttribute(name = "name")
	public String getName() {
		return name;
	}

	protected void setName(String name) {
		this.name = name;
	}

	@Column(name = "role_type", nullable = false, length = 255)
	@XmlAttribute(name = "userRoleType")
	protected String getUserRoleType() {
		return userRoleType;
	}

	protected void setUserRoleType(String userRoleType) {
		this.userRoleType = userRoleType;
	}

	private Integer tmpHashCode = null;

	@Override
	public int hashCode() {
		if (tmpHashCode == null) {
			// NOTE: this doesn't use Id so that the static permission
			// constants at the top of this class will match the permissions
			// in the database.
			final int prime = 31;
			int result = 1;
			result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
			tmpHashCode = new Integer(result);
		}
		return tmpHashCode.intValue();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof UserRolePermission)) {
			return false;
		}
		final UserRolePermission other = (UserRolePermission) obj;
		if ((getId() != null) && getId().equals(other.getId())) {
			return true;
		}
		if (getName() == null) {
			if (other.getName() != null) {
				return false;
			}
		} else if (!getName().equals(other.getName())) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return this.getClass().getName() + "[" + getId() + "]: " + getName();
	}

	@Override
	public int compareTo(UserRolePermission o) {
		if ((getId() != null) && getId().equals(o.getId())) {
			return 0;
		}
		return getName().compareTo(o.getName());
	}

	/**
	 * This class is used by JAXB to convert the id of an entity into an xml id
	 * string that will be distinct from other entity xml id strings by the use
	 * of a prefix.
	 * 
	 * @author ron
	 */
	@XmlTransient
	public static class IdAdapter extends XmlAdapter<String, Long> {
		private static final String prefix = "PRM_";

		@Override
		public Long unmarshal(String id) throws Exception {
			return null; // new Long(id.substring(prefix.length()));
		}

		@Override
		public String marshal(Long id) throws Exception {
			if (id != null) {
				return prefix + id.toString();
			}
			return "";
		}
	}
}
