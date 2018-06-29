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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.hibernate.annotations.*;
import org.hibernate.validator.NotEmpty;
import org.hibernate.validator.NotNull;
import org.xml.sax.SAXException;

import com.sun.xml.bind.v2.runtime.unmarshaller.Patcher;
import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext;

import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.StoryContainer;
import com.rreganjr.requel.project.StoryType;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.JAXBCreatedEntityPatcher;

/**
 * A story describes an interaction with the system as prose.
 * 
 * @author ron
 */
@Entity
@Table(name = "stories", uniqueConstraints = { @UniqueConstraint(columnNames = {
		"projectordomain_id", "name" }) })
@XmlRootElement(name = "story", namespace = "http://www.rreganjr.com/requel")
@XmlType(name = "story", namespace = "http://www.rreganjr.com/requel")
public class StoryImpl extends AbstractTextEntity implements Story {
	static final long serialVersionUID = 0;

	private Set<StoryContainer> referrers = new TreeSet<StoryContainer>(StoryContainer.COMPARATOR);
	private StoryType storyType;
	private Set<Goal> goals = new TreeSet<Goal>();
	private Set<Actor> actors = new TreeSet<Actor>();

	/**
	 * @param projectOrDomain
	 * @param name
	 * @param createdBy
	 * @param text
	 * @param storyType
	 */
	public StoryImpl(ProjectOrDomain projectOrDomain, User createdBy, String name, String text,
			StoryType storyType) {
		super(projectOrDomain, createdBy, name, text);
		// add to collection last so that sorting in the collection by entity
		// properties has access to all the properties.
		projectOrDomain.getStories().add(this);
		setStoryType(storyType);
	}

	protected StoryImpl() {
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
		return "STRY_" + getId();
	}

	@Transient
	public String getDescription() {
		return "Story: " + getName();
	}

	@XmlTransient
	@ManyToAny(fetch = FetchType.LAZY, metaColumn = @Column(name = "storycontainer_type", length = 255, nullable = false))
	@AnyMetaDef(idType = "long", metaType = "string", metaValues = {
			@MetaValue(value = "Project", targetEntity = ProjectImpl.class),
			@MetaValue(value = "Actor", targetEntity = ActorImpl.class),
			@MetaValue(value = "Goal", targetEntity = GoalImpl.class),
			@MetaValue(value = "UseCase", targetEntity = UseCaseImpl.class) })
	@JoinTable(name = "story_storycontainers", joinColumns = { @JoinColumn(name = "story_id") }, inverseJoinColumns = {
			@JoinColumn(name = "storycontainer_type"), @JoinColumn(name = "storycontainer_id") })
	@Sort(type = SortType.COMPARATOR, comparator = StoryContainer.StoryContainerComparator.class)
	public Set<StoryContainer> getReferrers() {
		return referrers;
	}

	protected void setReferrers(Set<StoryContainer> referrers) {
		this.referrers = referrers;
	}

	@Override
	@Enumerated(EnumType.STRING)
	@XmlAttribute(name = "storyType")
	@XmlJavaTypeAdapter(StoryTypeAdapter.class)
	@Column(nullable = false)
	@NotNull(message = "a type is required.")
	public StoryType getStoryType() {
		return storyType;
	}

	@Override
	public void setStoryType(StoryType storyType) {
		this.storyType = storyType;
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
	@JoinTable(name = "story_goals", joinColumns = { @JoinColumn(name = "story_id") }, inverseJoinColumns = { @JoinColumn(name = "goal_id") })
	@SortNatural
	public Set<Goal> getGoals() {
		return goals;
	}

	protected void setGoals(Set<Goal> goals) {
		this.goals = goals;
	}

	@XmlElementWrapper(name = "actors", namespace = "http://www.rreganjr.com/requel")
	@XmlIDREF
	@XmlElement(name = "actorRef", type = ActorImpl.class, namespace = "http://www.rreganjr.com/requel")
	@ManyToMany(targetEntity = ActorImpl.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@JoinTable(name = "story_actors", joinColumns = { @JoinColumn(name = "story_id") }, inverseJoinColumns = { @JoinColumn(name = "actor_id") })
	@SortNatural
	public Set<Actor> getActors() {
		return actors;
	}

	protected void setActors(Set<Actor> actors) {
		this.actors = actors;
	}

	@Override
	public int compareTo(Story o) {
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
						goal.getReferrers().add(StoryImpl.this);
					}
					for (Actor actor : getActors()) {
						actor.getReferrers().add(StoryImpl.this);
					}
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new SAXException(e);
				}
			}
		});
	}

	/**
	 * This class is used by JAXB to convert the StoryType of a Story into a
	 * string for an attribute in the xml file and the reverse when
	 * unmartialling.
	 * 
	 * @author ron
	 */
	@XmlTransient
	public static class StoryTypeAdapter extends XmlAdapter<String, StoryType> {

		@Override
		public StoryType unmarshal(String typeString) throws Exception {
			return StoryType.valueOf(typeString);
		}

		@Override
		public String marshal(StoryType type) throws Exception {
			return type.toString();
		}
	}
}
