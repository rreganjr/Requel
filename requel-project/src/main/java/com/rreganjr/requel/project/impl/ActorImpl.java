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
package com.rreganjr.requel.project.impl;

import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.hibernate.annotations.*;
import org.hibernate.validator.NotEmpty;
import org.xml.sax.SAXException;


import com.sun.xml.bind.v2.runtime.unmarshaller.Patcher;
import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext;

import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.ActorContainer;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.JAXBCreatedEntityPatcher;

/**
 * @author ron
 */
@Entity
@Table(name = "actors", uniqueConstraints = { @UniqueConstraint(columnNames = {
		"projectordomain_id", "name" }) })
@XmlRootElement(name = "actor", namespace = "http://www.rreganjr.com/requel")
@XmlType(name = "actor", namespace = "http://www.rreganjr.com/requel")
public class ActorImpl extends AbstractTextEntity implements Actor {
	static final long serialVersionUID = 0L;

	private Set<ActorContainer> referers = new TreeSet<ActorContainer>(ActorContainer.COMPARATOR);
	private Set<Goal> goals = new TreeSet<Goal>();

	/**
	 * @param projectOrDomain
	 * @param createdBy
	 * @param name
	 * @param description
	 */
	public ActorImpl(ProjectOrDomain projectOrDomain, User createdBy, String name,
			String description) {
		super(projectOrDomain, createdBy, name, description);
	}

	protected ActorImpl() {
		// for hibernate
	}

	@Override
	@Column(nullable = false, unique = false)
	@NotEmpty(message = "a unique name is required.")
	@XmlElement(name = "name", namespace = "http://www.rreganjr.com/requel")
	public String getName() {
		return super.getName();
	}

	// hack for JAXB to set the name, for some reason it won't use the inherited
	// method.
	@Override
	public void setName(String name) {
		super.setName(name);
	}

	@Transient
	@XmlID
	@XmlAttribute(name = "id")
	public String getXmlId() {
		return "ACT_" + getId();
	}

	@Transient
	public String getDescription() {
		return "Actor: " + getName();
	}

	@XmlTransient
	@ManyToAny(fetch = FetchType.LAZY, metaColumn = @Column(name = "actorcontainer_type", length = 255, nullable = false))
	@AnyMetaDef(idType = "long", metaType = "string", metaValues = {
			@MetaValue(value = "Project", targetEntity = ProjectImpl.class),
			@MetaValue(value = "UseCase", targetEntity = UseCaseImpl.class),
			@MetaValue(value = "Goal", targetEntity = GoalImpl.class),
			@MetaValue(value = "Story", targetEntity = StoryImpl.class) })
	@JoinTable(name = "actor_actorcontainers", joinColumns = { @JoinColumn(name = "actor_id") }, inverseJoinColumns = {
			@JoinColumn(name = "actorcontainer_type"), @JoinColumn(name = "actorcontainer_id") })
	@Sort(type = SortType.COMPARATOR, comparator = ActorContainer.ActorContainerComparator.class)
	public Set<ActorContainer> getReferers() {
		return referers;
	}

	protected void setReferers(Set<ActorContainer> referers) {
		this.referers = referers;
	}

	@Override
	@XmlElementWrapper(name = "goals", namespace = "http://www.rreganjr.com/requel")
	@XmlIDREF
	@XmlElement(name = "goalRef", type = GoalImpl.class, namespace = "http://www.rreganjr.com/requel")
	@ManyToMany(targetEntity = GoalImpl.class, cascade = { CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@JoinTable(name = "actor_goals", joinColumns = { @JoinColumn(name = "actor_id") }, inverseJoinColumns = { @JoinColumn(name = "goal_id") })
	@SortNatural
	public Set<Goal> getGoals() {
		return goals;
	}

	protected void setGoals(Set<Goal> goals) {
		this.goals = goals;
	}

	@Override
	public int compareTo(Actor o) {
		return getName().compareToIgnoreCase(o.getName());
	}

	/**
	 * This is for JAXB to patchup the parent/child relationship and to patchup
	 * existing persistent objects for the objects that are attached directly to
	 * this object.
	 * 
	 * @param userRepository
	 * @param defaultCreatedByUser -
	 *            the user to be set as the created by if no user is supplied.
	 * see UnmarshallerListener
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
						goal.getReferers().add(ActorImpl.this);
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
