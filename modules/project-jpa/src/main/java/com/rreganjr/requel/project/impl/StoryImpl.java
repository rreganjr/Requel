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

import com.rreganjr.platform.identity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlIDREF;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import jakarta.persistence.DiscriminatorType;

import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.SortNatural;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.tagging.Taggable;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.StoryContainer;
import com.rreganjr.requel.project.StoryType;
import com.rreganjr.validator.ValidationLimits;
import jakarta.validation.constraints.Size;

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
public class StoryImpl extends AbstractTextEntity implements Story, Taggable {
	static final long serialVersionUID = 0;

	private Set<StoryContainer> referers = new TreeSet<StoryContainer>(StoryContainer.COMPARATOR);
	private StoryType storyType;
	private Actor primaryActor;
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
	@Size(max = ValidationLimits.ARTIFACT_NAME_MAX, message = ValidationLimits.LENGTH_MESSAGE)
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
	@Column(name = "storycontainer_type", length = 255, nullable = false)
	@ManyToAny(fetch = FetchType.LAZY)
	@AnyDiscriminator(DiscriminatorType.STRING)
	@AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.Project", entity = ProjectImpl.class)
	@AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.Actor", entity = ActorImpl.class)
	@AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.Goal", entity = GoalImpl.class)
	@AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.UseCase", entity = UseCaseImpl.class)
	@AnyKeyJavaClass(Long.class)
	@JoinTable(name = "story_storycontainers",
			joinColumns = @JoinColumn(name = "story_id"),
			inverseJoinColumns =  @JoinColumn(name = "storycontainer_id")
	)
	@SortComparator(StoryContainer.StoryContainerComparator.class)
	public Set<StoryContainer> getReferers() {
		return referers;
	}

	protected void setReferers(Set<StoryContainer> referers) {
		this.referers = referers;
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

	@Override
	@XmlIDREF
	@XmlElement(name = "primaryActorRef", type = ActorImpl.class, namespace = "http://www.rreganjr.com/requel")
	@ManyToOne(targetEntity = ActorImpl.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH }, optional = true, fetch = FetchType.LAZY)
	@JoinColumn(name = "primary_actor_id", nullable = true)
	public Actor getPrimaryActor() {
		return primaryActor;
	}

	@Override
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
