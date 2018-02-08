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
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortNatural;
import org.hibernate.annotations.SortType;
import org.hibernate.validator.NotEmpty;

import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.ScenarioType;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.utils.jaxb.UnmarshallerListener;

/**
 * @author ron
 */
@Entity
@Table(name = "scenarios", uniqueConstraints = { @UniqueConstraint(columnNames = {
		"projectordomain_id", "name" }) })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING, length = 255)
@DiscriminatorValue(value = "com.rreganjr.requel.project.Step")
@XmlRootElement(name = "step", namespace = "http://www.rreganjr.com/requel")
@XmlType(name = "step", namespace = "http://www.rreganjr.com/requel")
public class StepImpl extends AbstractTextEntity implements Step {
	static final long serialVersionUID = 0L;

	private ScenarioType scenarioType = ScenarioType.Primary;
	private Set<Scenario> usedBy = new TreeSet<Scenario>();
	private String type;

	/**
	 * @param projectOrDomain
	 * @param createdBy
	 * @param name
	 * @param text
	 * @param scenarioType
	 */
	public StepImpl(ProjectOrDomain projectOrDomain, User createdBy, String name, String text,
			ScenarioType scenarioType) {
		this(Step.class.getName(), projectOrDomain, createdBy, name, text, scenarioType);
	}

	protected StepImpl(String type, ProjectOrDomain projectOrDomain, User createdBy, String name,
			String text, ScenarioType scenarioType) {
		super(projectOrDomain, createdBy, name, text);
		setInstanceType(type);
		setType(scenarioType);
	}

	protected StepImpl() {
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

	// access to the descriminator
	@Column(name = "type", insertable = false, updatable = false)
	protected String getInstanceType() {
		return type;
	}

	protected void setInstanceType(String type) {
		this.type = type;
	}

	@Override
	@XmlTransient
	@ManyToMany(targetEntity = ScenarioImpl.class, cascade = { CascadeType.MERGE,
			CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.LAZY, mappedBy = "steps")
	//@JoinTable(name = "scenario_steps", joinColumns = { @JoinColumn(name = "step_id") }, inverseJoinColumns = { @JoinColumn(name = "scenario_id") })
	@SortNatural
	public Set<Scenario> getUsingScenarios() {
		return usedBy;
	}

	protected void setUsingScenarios(Set<Scenario> usedBy) {
		this.usedBy = usedBy;
	}

	@Override
	@Enumerated(EnumType.STRING)
	@XmlAttribute(name = "scenarioType")
	@Column(name = "scenario_type")
	@XmlJavaTypeAdapter(ScenarioTypeAdapter.class)
	public ScenarioType getType() {
		return scenarioType;
	}

	public void setType(ScenarioType scenarioType) {
		this.scenarioType = scenarioType;
	}

	/**
	 * @see com.rreganjr.requel.project.ProjectOrDomainEntity#getXmlId()
	 */
	@Transient
	@XmlID
	@XmlAttribute(name = "id")
	@Override
	public String getXmlId() {
		// TODO Auto-generated method stub
		return "SCN_" + getId();
	}

	@Override
	@Transient
	@XmlTransient
	public String getDescription() {
		// TODO Auto-generated method stub
		return "Step: " + getName();
	}

	@Override
	public int compareTo(Step o) {
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
	 * @param parent
	 * @see UnmarshallerListener
	 */
	@Override
	public void afterUnmarshal(UserRepository userRepository, User defaultCreatedByUser,
			Object parent) {
		if (parent instanceof ProjectOrDomainEntity) {
			super.afterUnmarshal(userRepository, defaultCreatedByUser, null);
		} else if (parent instanceof ProjectOrDomain) {
			super.afterUnmarshal(userRepository, defaultCreatedByUser, parent);
		} else {
			throw new RuntimeException("Unexpected parent type "
					+ parent.getClass().getSimpleName() + " for " + getClass().getSimpleName()
					+ " named: " + getName());
		}
	}

	/**
	 * This class is used by JAXB to convert the ScenarioType of a Scenario into
	 * a string for an attribute in the xml file and the reverse when
	 * unmartialling.
	 * 
	 * @author ron
	 */
	@XmlTransient
	public static class ScenarioTypeAdapter extends XmlAdapter<String, ScenarioType> {

		@Override
		public ScenarioType unmarshal(String typeString) throws Exception {
			return ScenarioType.valueOf(typeString);
		}

		@Override
		public String marshal(ScenarioType type) throws Exception {
			return type.toString();
		}
	}
}
