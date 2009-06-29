/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * 
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
package edu.harvard.fas.rregan.requel.project.impl;

import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;

import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.Stakeholder;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.impl.UserImpl;

/**
 * @author ron
 */
@Entity
@Table(name = "stakeholders", uniqueConstraints = { @UniqueConstraint(columnNames = {
		"projectordomain_id", "name", "user_id" }) })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "stakeholder_type", discriminatorType = DiscriminatorType.STRING, length = 255)
@XmlType(namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
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
	protected AbstractStakeholder(String type, ProjectOrDomain projectOrDomain, User createdBy,
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
	 * @see edu.harvard.fas.rregan.requel.project.GoalContainer#getGoals()
	 */
	@Override
	@OneToMany(targetEntity = GoalImpl.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@XmlElementWrapper(name = "goals", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@XmlIDREF
	@XmlElement(name = "goalRef", type = GoalImpl.class, namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	public Set<Goal> getGoals() {
		return goals;
	}

	protected void setGoals(Set<Goal> goals) {
		this.goals = goals;
	}
}
