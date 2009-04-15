/*
 * $Id: StepImpl.java,v 1.8 2009/02/11 11:00:15 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.impl;

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
import org.hibernate.annotations.SortType;
import org.hibernate.validator.NotEmpty;

import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.project.Scenario;
import edu.harvard.fas.rregan.requel.project.ScenarioType;
import edu.harvard.fas.rregan.requel.project.Step;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.utils.jaxb.UnmarshallerListener;

/**
 * @author ron
 */
@Entity
@Table(name = "scenarios", uniqueConstraints = { @UniqueConstraint(columnNames = {
		"projectordomain_id", "name" }) })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING, length = 255)
@DiscriminatorValue(value = "edu.harvard.fas.rregan.requel.project.Step")
@XmlRootElement(name = "step", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "step", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
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
	@JoinTable(name = "scenario_steps", joinColumns = { @JoinColumn(name = "step_id") }, inverseJoinColumns = { @JoinColumn(name = "scenario_id") })
	@Sort(type = SortType.NATURAL)
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
	 * @see edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity#getXmlId()
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
