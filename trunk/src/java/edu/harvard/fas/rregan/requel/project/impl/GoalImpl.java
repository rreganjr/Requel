/*
 * $Id$
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
package edu.harvard.fas.rregan.requel.project.impl;

import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.MetaValue;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.hibernate.validator.NotEmpty;

import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.GoalContainer;
import edu.harvard.fas.rregan.requel.project.GoalRelation;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * @author ron
 */
@Entity
@Table(name = "goals", uniqueConstraints = { @UniqueConstraint(columnNames = {
		"projectordomain_id", "name" }) })
@XmlRootElement(name = "goal", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "goal", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
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

	@XmlElementWrapper(name = "goalRelations", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@XmlElementRef(type = GoalRelationImpl.class)
	@OneToMany(targetEntity = GoalRelationImpl.class, cascade = { CascadeType.REFRESH }, fetch = FetchType.LAZY, mappedBy = "fromGoalInternal")
	@JoinColumn(name = "fromGoalInternal_id")
	public Set<GoalRelation> getRelationsFromThisGoal() {
		return relationsFromThisGoal;
	}

	protected void setRelationsFromThisGoal(Set<GoalRelation> relationsFromThisGoal) {
		this.relationsFromThisGoal = relationsFromThisGoal;
	}

	@XmlTransient
	@OneToMany(targetEntity = GoalRelationImpl.class, cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, mappedBy = "toGoalInternal")
	@JoinColumn(name = "toGoalInternal_id")
	public Set<GoalRelation> getRelationsToThisGoal() {
		return relationsToThisGoal;
	}

	protected void setRelationsToThisGoal(Set<GoalRelation> relationsToThisGoal) {
		this.relationsToThisGoal = relationsToThisGoal;
	}

	@XmlTransient
	@ManyToAny(fetch = FetchType.LAZY, metaColumn = @Column(name = "goalcontainer_type", length = 255, nullable = false))
	@AnyMetaDef(idType = "long", metaType = "string", metaValues = {
			@MetaValue(value = "edu.harvard.fas.rregan.requel.project.Project", targetEntity = ProjectImpl.class),
			@MetaValue(value = "edu.harvard.fas.rregan.requel.project.UseCase", targetEntity = UseCaseImpl.class),
			@MetaValue(value = "edu.harvard.fas.rregan.requel.project.Scenario", targetEntity = ScenarioImpl.class),
			@MetaValue(value = "edu.harvard.fas.rregan.requel.project.Story", targetEntity = StoryImpl.class),
			@MetaValue(value = "edu.harvard.fas.rregan.requel.project.Actor", targetEntity = ActorImpl.class),
			@MetaValue(value = "edu.harvard.fas.rregan.requel.project.Stakeholder", targetEntity = StakeholderImpl.class) })
	@JoinTable(name = "goals_goalcontainers", joinColumns = { @JoinColumn(name = "goal_id") }, inverseJoinColumns = {
			@JoinColumn(name = "goalcontainer_type"), @JoinColumn(name = "goalcontainer_id") })
	@Sort(type = SortType.COMPARATOR, comparator = GoalContainer.GoalContainerComparator.class)
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
