/*
 * $Id: OrganizationImpl.java,v 1.9 2009/01/10 11:08:58 rregan Exp $
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
package edu.harvard.fas.rregan.requel.user.impl;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hibernate.validator.NotEmpty;

import edu.harvard.fas.rregan.requel.user.Organization;

/**
 * @author ron
 */
@Entity
@Table(name = "organizations")
@XmlRootElement(name = "organization", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "organization", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public class OrganizationImpl implements Organization, Serializable {
	static final long serialVersionUID = 0L;

	private Long id;
	private String name;
	private int version = 1; // start at 1 so hibernate recognizes the new

	// instance as the initial value and not stale.

	/**
	 * @param name
	 */
	public OrganizationImpl(String name) {
		setName(name);
	}

	protected OrganizationImpl() {
		// for hibernate
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	protected Long getId() {
		return id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	@Version
	protected int getVersion() {
		return version;
	}

	protected void setVersion(int version) {
		this.version = version;
	}

	@Column(unique = true, nullable = false)
	@NotEmpty(message = "organization name is required.")
	@XmlAttribute(name = "name")
	public String getName() {
		return name;
	}

	protected void setName(String name) {
		this.name = name;
	}

	@Override
	public int compareTo(Organization o) {
		return getName().compareTo(o.getName());
	}

	@Override
	public String toString() {
		return Organization.class.getName() + "[" + getId() + "]: " + getName();
	}

	private Integer tmpHashCode = null;

	@Override
	public int hashCode() {
		if (tmpHashCode == null) {
			if (getId() != null) {
				tmpHashCode = getId().hashCode();
			} else {
				final int prime = 31;
				int result = 1;
				result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
				tmpHashCode = result;
			}
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
		// NOTE: getClass().equals(obj.getClass()) fails when the obj is a proxy
		if (!getClass().isAssignableFrom(obj.getClass())) {
			return false;
		}
		// TODO: will this work for proxies?
		final OrganizationImpl other = (OrganizationImpl) obj;
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
}
