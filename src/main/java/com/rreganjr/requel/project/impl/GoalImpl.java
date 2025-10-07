/*
 * $Id$
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

import java.util.Set;
import java.util.TreeSet;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import jakarta.persistence.DiscriminatorType;

import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.SortComparator;
import jakarta.validation.constraints.NotEmpty;

import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalContainer;
import com.rreganjr.requel.project.GoalRelation;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.user.User;

/**
 * @author ron
 */
@Entity
@Table(name = "goals", uniqueConstraints = { @UniqueConstraint(columnNames = {
		"projectordomain_id", "name" }) })
@XmlRootElement(name = "goal", namespace = "http://www.rreganjr.com/requel")
@XmlType(name = "goal", namespace = "http://www.rreganjr.com/requel")
public class GoalImpl extends AbstractTextEntity implements Goal {
	static final long serialVersionUID = 0L;

	private Set<GoalRelation> relationsFromThisGoal = new TreeSet<GoalRelation>();
	private Set<GoalRelation> relationsToThisGoal = new TreeSet<GoalRelation>();
	private Set<GoalContainer> referersToThisGoal = new TreeSet<GoalContainer>(
			GoalContainer.COMPARATOR);

	/**
	 * @param projectOrDomain
	 * @param name
	 * @param createdBy
	 * @param text
	 */
	public GoalImpl(ProjectOrDomain projectOrDomain, User createdBy, String name, String text) {
		super(projectOrDomain, createdBy, name, text);
		// add to collection last so that sorting in the collection by entity
		// properties has access to all the properties.
		projectOrDomain.getGoals().add(this);
	}

	protected GoalImpl() {
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
		return "GOL_" + getId();
	}

	@Transient
	public String getDescription() {
		return "Goal: " + getName();
	}

	@XmlElementWrapper(name = "goalRelations", namespace = "http://www.rreganjr.com/requel")
	@XmlElementRef(type = GoalRelationImpl.class)
	@OneToMany(targetEntity = GoalRelationImpl.class, cascade = { CascadeType.REFRESH }, fetch = FetchType.LAZY, mappedBy = "fromGoalInternal")
	//@JoinColumn(name = "fromGoalInternal_id")
	public Set<GoalRelation> getRelationsFromThisGoal() {
		return relationsFromThisGoal;
	}

	protected void setRelationsFromThisGoal(Set<GoalRelation> relationsFromThisGoal) {
		this.relationsFromThisGoal = relationsFromThisGoal;
	}

	@XmlTransient
	@OneToMany(targetEntity = GoalRelationImpl.class, cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, mappedBy = "toGoalInternal")
	//@JoinColumn(name = "toGoalInternal_id")
	public Set<GoalRelation> getRelationsToThisGoal() {
		return relationsToThisGoal;
	}

	protected void setRelationsToThisGoal(Set<GoalRelation> relationsToThisGoal) {
		this.relationsToThisGoal = relationsToThisGoal;
	}

	@XmlTransient
	@Column(name = "goalcontainer_type", length = 255, nullable = false)
	@ManyToAny(fetch = FetchType.LAZY)
	@AnyDiscriminator(DiscriminatorType.STRING)
	@AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.Project", entity = ProjectImpl.class)
	@AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.UseCase", entity = UseCaseImpl.class)
	@AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.Scenario", entity = ScenarioImpl.class)
	@AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.Story", entity = StoryImpl.class)
	@AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.Actor", entity = ActorImpl.class)
	@AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.NonUserStakeholder", entity = NonUserStakeholderImpl.class)
	@AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.UserStakeholder", entity = UserStakeholderImpl.class)
	@AnyKeyJavaClass(Long.class)
	@JoinTable(name = "goals_goalcontainers",
			joinColumns = @JoinColumn(name = "goal_id"),
			inverseJoinColumns = @JoinColumn(name = "goalcontainer_id")
	)
	@SortComparator(GoalContainer.GoalContainerComparator.class)
	public Set<GoalContainer> getReferers() {
		return referersToThisGoal;
	}

	protected void setReferers(Set<GoalContainer> referersToThisGoal) {
		this.referersToThisGoal = referersToThisGoal;
	}

	@Override
	public int compareTo(Goal o) {
		return getName().compareToIgnoreCase(o.getName());
	}
}
