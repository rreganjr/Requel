/*
 * $Id: $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * 
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
package com.rreganjr.requel.project.impl;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.xml.sax.SAXException;


import com.sun.xml.bind.v2.runtime.unmarshaller.Patcher;
import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext;

import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.NonUserStakeholder;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.utils.jaxb.JAXBCreatedEntityPatcher;
import com.rreganjr.requel.utils.jaxb.UnmarshallerListener;

/**
 * A stakeholder that is not a user.
 * 
 * @author ron
 */
@Entity
@DiscriminatorValue(value = "com.rreganjr.requel.project.NonUserStakeholder")
@XmlRootElement(name = "nonuser-stakeholder", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "nonuser-stakeholder", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public class NonUserStakeholderImpl extends AbstractStakeholder implements NonUserStakeholder {
	static final long serialVersionUID = 0L;

	private String text;

	/**
	 * @param type
	 * @param projectOrDomain
	 * @param createdBy
	 * @param name
	 */
	public NonUserStakeholderImpl(ProjectOrDomain projectOrDomain, User createdBy, String name) {
		super(NonUserStakeholder.class.getName(), projectOrDomain, createdBy, name);
		projectOrDomain.getStakeholders().add(this);
	}

	protected NonUserStakeholderImpl() {
		// for hibernate
	}

	@Override
	@Column(nullable = true, unique = false)
	@XmlElement(name = "name", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	public String getName() {
		return super.getName();
	}

	// hack for JAXB to set the name, for some reason it won't use the inherited
	// method.
	@Override
	public void setName(String name) {
		super.setName(name);
	}

	@Lob
	@XmlElement(name = "text", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	public String getText() {
		return text;
	}

	/**
	 * Set the description of the stakeholder.
	 * 
	 * @param text
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * @see com.rreganjr.requel.project.Stakeholder#isUserStakeholder()
	 */
	@Override
	@Transient
	public boolean isUserStakeholder() {
		return false;
	}

	/**
	 * This is for JAXB to get a unique type specific id for use in an exported
	 * file.
	 */
	@Transient
	@XmlID
	@XmlAttribute(name = "id")
	public String getXmlId() {
		return "STK_" + getId();
	}

	/**
	 * This is for displaying a description in the user interface
	 */
	@XmlTransient
	@Transient
	public String getDescription() {
		return "Stakeholder: " + getName();
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Stakeholder o) {
		if (this == o) {
			return 0;
		}
		return getDescription().compareToIgnoreCase(o.getDescription());
	}

	private Integer tmpHashCode = null;

	@Override
	public int hashCode() {
		if (tmpHashCode == null) {
			if (getId() != null) {
				tmpHashCode = new Integer(getId().hashCode());
			}
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
		if (!getClass().isAssignableFrom(obj.getClass())) {
			return false;
		}
		final NonUserStakeholderImpl other = (NonUserStakeholderImpl) obj;
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

	/**
	 * This is for JAXB to patchup the parent/child relationship and to patchup
	 * existing persistent objects for the objects that are attached directly to
	 * this object.
	 * 
	 * @param userRepository
	 * @param defaultCreatedByUser -
	 *            the user to be set as the created by if no user is supplied.
	 * @see UnmarshallerListener
	 */
	public void afterUnmarshal(final UserRepository userRepository, User defaultCreatedByUser) {
		UnmarshallingContext.getInstance().addPatcher(
				new JAXBCreatedEntityPatcher(userRepository, this, defaultCreatedByUser));
		UnmarshallingContext.getInstance().addPatcher(new Patcher() {
			@Override
			public void run() throws SAXException {
				try {
					// update the references to goals
					for (Goal goal : getGoals()) {
						goal.getReferers().add(NonUserStakeholderImpl.this);
					}
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new SAXException(e);
				}
			}
		});
	}
}
