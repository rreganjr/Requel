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
import javax.persistence.ManyToOne;
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

import org.hibernate.annotations.SortNatural;
import org.hibernate.validator.NotEmpty;
import org.xml.sax.SAXException;

import com.sun.xml.bind.v2.runtime.unmarshaller.Patcher;
import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext;

import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.JAXBCreatedEntityPatcher;

/**
 * @author ron
 */
@Entity
@Table(name = "usecases", uniqueConstraints = { @UniqueConstraint(columnNames = {
		"projectordomain_id", "name" }) })
@XmlRootElement(name = "usecase", namespace = "http://www.rreganjr.com/requel")
@XmlType(name = "usecase", namespace = "http://www.rreganjr.com/requel")
public class UseCaseImpl extends AbstractTextEntity implements UseCase {
	static final long serialVersionUID = 0L;

	private Actor primaryActor;
	private Set<Goal> goals = new TreeSet<Goal>();
	private Set<Actor> actors = new TreeSet<Actor>();
	private Set<Story> stories = new TreeSet<Story>();
	private Scenario scenario;

	/**
	 * Create a use case.
	 * 
	 * @param projectOrDomain
	 * @param primaryActor
	 * @param createdBy
	 * @param name
	 * @param text -
	 *            short text description\
	 * @param scenario
	 */
	public UseCaseImpl(ProjectOrDomain projectOrDomain, Actor primaryActor, User createdBy,
			String name, String text, Scenario scenario) {
		super(projectOrDomain, createdBy, name, text);
		setPrimaryActor(primaryActor);
		setScenario(scenario);
		projectOrDomain.getUseCases().add(this);
	}

	protected UseCaseImpl() {
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

	/**
	 * @see com.rreganjr.requel.project.ProjectOrDomainEntity#getXmlId()
	 */
	@Transient
	@XmlID
	@XmlAttribute(name = "id")
	@Override
	public String getXmlId() {
		return "UC_" + getId();
	}

	/**
	 * @see com.rreganjr.requel.Describable#getDescription()
	 */
	@XmlTransient
	@Transient
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "UseCase: " + getName();
	}

	/**
	 * @see com.rreganjr.requel.project.UseCase#getPrimaryActor()
	 */
	@XmlIDREF
	@XmlElement(name = "primaryActorRef", type = ActorImpl.class, namespace = "http://www.rreganjr.com/requel")
	@ManyToOne(targetEntity = ActorImpl.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH }, optional = false)
	@Override
	public Actor getPrimaryActor() {
		return primaryActor;
	}

	/**
	 * @param primaryActor
	 */
	public void setPrimaryActor(Actor primaryActor) {
		this.primaryActor = primaryActor;
	}

	/**
	 * @see com.rreganjr.requel.project.GoalContainer#getGoals()
	 */
	@Override
	@XmlElementWrapper(name = "goals", namespace = "http://www.rreganjr.com/requel")
	@XmlIDREF
	@XmlElement(name = "goalRef", type = GoalImpl.class, namespace = "http://www.rreganjr.com/requel")
	@ManyToMany(targetEntity = GoalImpl.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@JoinTable(name = "usecase_goals", joinColumns = { @JoinColumn(name = "usecase_id") }, inverseJoinColumns = { @JoinColumn(name = "goal_id") })
	@SortNatural
	public Set<Goal> getGoals() {
		return goals;
	}

	protected void setGoals(Set<Goal> goals) {
		this.goals = goals;
	}

	/**
	 * @see com.rreganjr.requel.project.ActorContainer#getActors()
	 */
	@Override
	@XmlElementWrapper(name = "actors", namespace = "http://www.rreganjr.com/requel")
	@XmlIDREF
	@XmlElement(name = "actorRef", type = ActorImpl.class, namespace = "http://www.rreganjr.com/requel")
	@ManyToMany(targetEntity = ActorImpl.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@JoinTable(name = "usecase_actors", joinColumns = { @JoinColumn(name = "usecase_id") }, inverseJoinColumns = { @JoinColumn(name = "actor_id") })
	@SortNatural
	public Set<Actor> getActors() {
		return actors;
	}

	protected void setActors(Set<Actor> actors) {
		this.actors = actors;
	}

	/**
	 * @see com.rreganjr.requel.project.StoryContainer#getStories()
	 */
	@Override
	@XmlElementWrapper(name = "stories", namespace = "http://www.rreganjr.com/requel")
	@XmlIDREF
	@XmlElement(name = "storyRef", type = StoryImpl.class, namespace = "http://www.rreganjr.com/requel")
	@ManyToMany(targetEntity = StoryImpl.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@JoinTable(name = "usecase_stories", joinColumns = { @JoinColumn(name = "usecase_id") }, inverseJoinColumns = { @JoinColumn(name = "story_id") })
	@SortNatural
	public Set<Story> getStories() {
		return stories;
	}

	protected void setStories(Set<Story> stories) {
		this.stories = stories;
	}

	/**
	 * 
	 */
	@Override
	@XmlIDREF
	@XmlElement(name = "scenarioRef", type = ScenarioImpl.class, namespace = "http://www.rreganjr.com/requel")
	@ManyToOne(targetEntity = ScenarioImpl.class, cascade = { CascadeType.MERGE,
			CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.LAZY)
	public Scenario getScenario() {
		return scenario;
	}

	protected void setScenario(Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public int compareTo(UseCase o) {
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
					for (Goal goal : getGoals()) {
						goal.getReferrers().add(UseCaseImpl.this);
					}
					for (Actor actor : getActors()) {
						actor.getReferrers().add(UseCaseImpl.this);
					}
					if (getPrimaryActor() != null) {
						getPrimaryActor().getReferrers().add(UseCaseImpl.this);
					}
					for (Story story : getStories()) {
						story.getReferrers().add(UseCaseImpl.this);
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
