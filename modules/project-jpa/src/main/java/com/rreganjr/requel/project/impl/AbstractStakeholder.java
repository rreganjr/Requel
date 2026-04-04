/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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

import java.util.Set;
import java.util.TreeSet;


import com.rreganjr.requel.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlIDREF;
import jakarta.xml.bind.annotation.XmlType;

import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.user.impl.UserImpl;

/**
 * @author ron
 */
@Entity
@Table(name = "stakeholders", uniqueConstraints = { @UniqueConstraint(columnNames = {
		"projectordomain_id", "name", "user_id" }) })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "stakeholder_type", discriminatorType = DiscriminatorType.STRING, length = 255)
@XmlType(namespace = "http://www.rreganjr.com/requel")
public abstract class AbstractStakeholder extends AbstractProjectOrDomainEntity implements
		Stakeholder {
	static final long serialVersionUID = 0L;

	private User user;
	private String type;
	private Set<Goal> goals = new TreeSet<Goal>();

	/**
	 * Create a stakeholder for a non-user entity.
	 * 
	 * @param projectOrDomain
	 * @param createdBy
	 * @param name
	 */
	protected AbstractStakeholder(String type, ProjectOrDomain projectOrDomain, com.rreganjr.platform.identity.User createdBy,
			String name) {
		super(projectOrDomain, createdBy, name);
		setType(type);
	}

	protected AbstractStakeholder() {
		// for hibernate
	}

	@Column(name = "stakeholder_type", insertable = false, updatable = false)
	protected String getType() {
		return type;
	}

	protected void setType(String type) {
		this.type = type;
	}

	@ManyToOne(targetEntity = UserImpl.class, cascade = { CascadeType.REFRESH }, optional = true)
	protected User getUser() {
		return user;
	}

	protected void setUser(User user) {
		this.user = user;
	}

	/**
	 * @see com.rreganjr.requel.project.GoalContainer#getGoals()
	 */
	@Override
	@OneToMany(targetEntity = GoalImpl.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@XmlElementWrapper(name = "goals", namespace = "http://www.rreganjr.com/requel")
	@XmlIDREF
	@XmlElement(name = "goalRef", type = GoalImpl.class, namespace = "http://www.rreganjr.com/requel")
	public Set<Goal> getGoals() {
		return goals;
	}

	protected void setGoals(Set<Goal> goals) {
		this.goals = goals;
	}

	private static boolean hasText(String value) {
		return (value != null) && !value.trim().isEmpty();
	}

	@Override
	@Transient
	public String getDisplayName() {
		if (hasText(getName())) {
			return getName();
		}
		User user = getUser();
		if (user != null) {
			return user.getDisplayName();
		}
		return null;
	}

	@Override
	@Transient
	public String getDisplayUsername() {
		User user = getUser();
		return (user != null) ? user.getUsername() : null;
	}

	@Override
	@Transient
	public String getDisplayEmailAddress() {
		User user = getUser();
		return (user != null) ? user.getEmailAddress() : null;
	}

	@Override
	@Transient
	public String getDisplayPhoneNumber() {
		User user = getUser();
		return (user != null) ? user.getPhoneNumber() : null;
	}
}
