/*
 * $Id: AbstractProjectOrDomain.java,v 1.48 2009/03/05 08:50:46 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.impl;

import java.io.Serializable;
import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.hibernate.annotations.Where;
import org.hibernate.validator.NotEmpty;

import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext;

import edu.harvard.fas.rregan.requel.project.Actor;
import edu.harvard.fas.rregan.requel.project.GlossaryTerm;
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.project.ProjectTeam;
import edu.harvard.fas.rregan.requel.project.ReportGenerator;
import edu.harvard.fas.rregan.requel.project.Scenario;
import edu.harvard.fas.rregan.requel.project.Stakeholder;
import edu.harvard.fas.rregan.requel.project.Step;
import edu.harvard.fas.rregan.requel.project.Story;
import edu.harvard.fas.rregan.requel.project.UseCase;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.user.impl.User2UserImplAdapter;
import edu.harvard.fas.rregan.requel.user.impl.UserImpl;
import edu.harvard.fas.rregan.requel.utils.jaxb.DateAdapter;
import edu.harvard.fas.rregan.requel.utils.jaxb.JAXBCreatedEntityPatcher;
import edu.harvard.fas.rregan.requel.utils.jaxb.UnmarshallerListener;

/**
 * @author ron
 */
@Entity
@Table(name = "pods", uniqueConstraints = { @UniqueConstraint(columnNames = { "type", "name" }) })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING, length = 255)
@XmlType(namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public abstract class AbstractProjectOrDomain implements ProjectOrDomain, Serializable {
	static final long serialVersionUID = 0L;

	private Long id;
	private String name;
	private String description;
	private User createdBy;
	private Date dateCreated = new Date();
	private Set<Actor> actors = new TreeSet<Actor>();
	private Set<Goal> goals = new TreeSet<Goal>();
	private Set<Story> stories = new TreeSet<Story>();
	private Set<UseCase> useCases = new TreeSet<UseCase>();
	private Set<Scenario> scenarios = new TreeSet<Scenario>();
	private Set<Stakeholder> stakeholders = new TreeSet<Stakeholder>();
	private Set<ProjectTeam> teams = new TreeSet<ProjectTeam>();
	private SortedSet<GlossaryTerm> terms = new TreeSet<GlossaryTerm>();
	private Set<ReportGenerator> reportGenerators = new TreeSet<ReportGenerator>();

	private String type;
	private int version = 1;

	protected AbstractProjectOrDomain(String type, String name, User createdBy) {
		setType(type);
		setName(name);
		setCreatedBy(createdBy);
		setDateCreated(new Date());
	}

	protected AbstractProjectOrDomain() {
		// for hibernate
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@XmlID
	@XmlAttribute(name = "id")
	@XmlJavaTypeAdapter(IdAdapter.class)
	protected Long getId() {
		return id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	@Version
	@XmlAttribute(name = "revision", required = false)
	protected int getVersion() {
		return version;
	}

	protected void setVersion(int version) {
		this.version = version;
	}

	@Column(name = "type", insertable = false, updatable = false)
	protected String getType() {
		return type;
	}

	protected void setType(String type) {
		this.type = type;
	}

	@XmlIDREF()
	@XmlAttribute(name = "createdBy")
	@XmlJavaTypeAdapter(User2UserImplAdapter.class)
	@ManyToOne(targetEntity = UserImpl.class, cascade = { CascadeType.REFRESH }, fetch = FetchType.LAZY)
	public User getCreatedBy() {
		return createdBy;
	}

	/**
	 * @param createdBy
	 */
	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

	@Column(nullable = false)
	@NotEmpty(message = "a name is required.")
	@XmlElement(name = "name", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElement(name = "description", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@Lob
	public String getText() {
		return description;
	}

	public void setText(String description) {
		this.description = description;
	}

	@XmlAttribute(name = "dateCreated")
	@XmlJavaTypeAdapter(DateAdapter.class)
	@Column(updatable = false)
	@Temporal(TemporalType.TIMESTAMP)
	public Date getDateCreated() {
		return dateCreated;
	}

	protected void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	@XmlElementWrapper(name = "glossary", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@XmlElementRef(type = GlossaryTermImpl.class)
	@OneToMany(targetEntity = GlossaryTermImpl.class, cascade = { CascadeType.PERSIST,
			CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@Sort(type = SortType.NATURAL)
	@Override
	public SortedSet<GlossaryTerm> getGlossaryTerms() {
		return terms;
	}

	protected void setGlossaryTerms(SortedSet<GlossaryTerm> terms) {
		this.terms = terms;
	}

	@XmlElementWrapper(name = "actors", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@XmlElementRef(type = ActorImpl.class)
	@OneToMany(targetEntity = ActorImpl.class, cascade = { CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.LAZY, mappedBy = "projectOrDomain")
	@Sort(type = SortType.NATURAL)
	public Set<Actor> getActors() {
		return actors;
	}

	protected void setActors(Set<Actor> actors) {
		this.actors = actors;
	}

	@XmlElementWrapper(name = "goals", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@XmlElementRef(type = GoalImpl.class)
	@OneToMany(targetEntity = GoalImpl.class, cascade = { CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.LAZY, mappedBy = "projectOrDomain")
	@Sort(type = SortType.NATURAL)
	public Set<Goal> getGoals() {
		return goals;
	}

	protected void setGoals(Set<Goal> goals) {
		this.goals = goals;
	}

	@XmlElementWrapper(name = "stories", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@XmlElementRef(type = StoryImpl.class)
	@OneToMany(targetEntity = StoryImpl.class, cascade = { CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.LAZY, mappedBy = "projectOrDomain")
	@Sort(type = SortType.NATURAL)
	public Set<Story> getStories() {
		return stories;
	}

	protected void setStories(Set<Story> stories) {
		this.stories = stories;
	}

	@XmlElementWrapper(name = "usecases", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@XmlElementRef(type = UseCaseImpl.class)
	@OneToMany(targetEntity = UseCaseImpl.class, cascade = { CascadeType.PERSIST,
			CascadeType.REFRESH }, fetch = FetchType.LAZY, mappedBy = "projectOrDomain")
	@Sort(type = SortType.NATURAL)
	public Set<UseCase> getUseCases() {
		return useCases;
	}

	protected void setUseCases(Set<UseCase> useCases) {
		this.useCases = useCases;
	}

	@XmlTransient
	@OneToMany(targetEntity = ScenarioImpl.class, cascade = { CascadeType.PERSIST,
			CascadeType.REFRESH }, fetch = FetchType.LAZY, mappedBy = "projectOrDomain")
	@Where(clause = "type like '%Scenario'")
	@Sort(type = SortType.NATURAL)
	public Set<Scenario> getScenarios() {
		return scenarios;
	}

	protected void setScenarios(Set<Scenario> scenarios) {
		this.scenarios = scenarios;
	}

	/**
	 * This is a helper for JAXB to export all the steps and scenarios at one
	 * level and scenarios to use id references to identify the steps
	 * internally. This allows for shared sub-scenarios and steps to be exported
	 * without duplication.
	 * 
	 * @return a set of all the scenarios and steps in the project
	 */
	@XmlElementWrapper(name = "scenarios", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@XmlElementRef(type = StepImpl.class)
	@Transient
	public Set<Step> getAllScenariosAndSteps() {
		Set<Step> scenariosAndSteps = new HashSet<Step>(getScenarios());
		Deque<Scenario> scenariosToExamine = new LinkedList<Scenario>(getScenarios());
		while (!scenariosToExamine.isEmpty()) {
			Scenario scenario = scenariosToExamine.pop();
			for (Step step : scenario.getSteps()) {
				scenariosAndSteps.add(step);
				if (step instanceof Scenario) {
					scenariosToExamine.add((Scenario) step);
				}
			}
		}
		return scenariosAndSteps;
	}

	@XmlElementWrapper(name = "stakeholders", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@XmlElementRef(name = "stakeholder", type = StakeholderImpl.class)
	@OneToMany(targetEntity = StakeholderImpl.class, cascade = { CascadeType.PERSIST,
			CascadeType.REFRESH }, fetch = FetchType.LAZY, mappedBy = "projectOrDomain")
	@Sort(type = SortType.NATURAL)
	public Set<Stakeholder> getStakeholders() {
		return stakeholders;
	}

	protected void setStakeholders(Set<Stakeholder> stakeholders) {
		this.stakeholders = stakeholders;
	}

	@XmlElementWrapper(name = "teams", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@XmlElementRef(name = "team", type = ProjectTeamImpl.class)
	@OneToMany(targetEntity = ProjectTeamImpl.class, cascade = { CascadeType.PERSIST,
			CascadeType.REFRESH }, fetch = FetchType.LAZY, mappedBy = "projectOrDomain")
	@Sort(type = SortType.NATURAL)
	public Set<ProjectTeam> getTeams() {
		return teams;
	}

	protected void setTeams(Set<ProjectTeam> teams) {
		this.teams = teams;
	}

	@Override
	@XmlElementWrapper(name = "reports", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@XmlElementRef(name = "report", type = ReportGeneratorImpl.class)
	@OneToMany(targetEntity = ReportGeneratorImpl.class, cascade = { CascadeType.PERSIST,
			CascadeType.REFRESH }, fetch = FetchType.LAZY, mappedBy = "projectOrDomain")
	@Sort(type = SortType.NATURAL)
	public Set<ReportGenerator> getReportGenerators() {
		return reportGenerators;
	}

	protected void setReportGenerators(Set<ReportGenerator> reportGenerators) {
		this.reportGenerators = reportGenerators;
	}

	/**
	 * @return all the entities of a project or domain in a single set.
	 */
	@Override
	@Transient
	@XmlTransient
	public Set<ProjectOrDomainEntity> getProjectEntities() {
		Set<ProjectOrDomainEntity> projectEntities = new HashSet<ProjectOrDomainEntity>();
		projectEntities.addAll(getActors());
		projectEntities.addAll(getGoals());
		projectEntities.addAll(getScenarios());
		for (Scenario scenario : getScenarios()) {
			projectEntities.addAll(scenario.getSteps());
		}
		projectEntities.addAll(getStakeholders());
		projectEntities.addAll(getStories());
		projectEntities.addAll(getUseCases());
		projectEntities.addAll(getGlossaryTerms());
		projectEntities.addAll(getReportGenerators());
		projectEntities.addAll(getTeams());
		return projectEntities;
	}

	/**
	 * @return The interface of this object that is a subclass of
	 *         ProjectOrDomain, basically getting the type of domain entity of a
	 *         subclass.
	 */
	@Transient
	public Class<?> getProjectOrDomainInterface() {
		Class<?> type = getClass();
		while (AbstractProjectOrDomain.class.isAssignableFrom(type)) {
			for (Class<?> face : type.getInterfaces()) {
				if (ProjectOrDomain.class.isAssignableFrom(face)) {
					return face;
				}
			}
			type = type.getSuperclass();
		}
		throw new RuntimeException("The class " + type.getSimpleName()
				+ " is not a descendent of ProjectOrDomainEntity.");
	}

	@Override
	public String toString() {
		return getProjectOrDomainInterface().toString() + "[" + getId() + "]:" + getName();
	}

	private Integer tmpHashCode = null;

	@Override
	public int hashCode() {
		if (tmpHashCode == null) {
			if (getId() != null) {
				tmpHashCode = getId().hashCode();
			} else {
				final int prime = 31;
				int result = 1;
				result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
				result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
				tmpHashCode = result;
			}
		}
		return tmpHashCode.intValue();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		// NOTE: getClass().equals(obj.getClass()) fails when the obj is a proxy
		if (!getClass().isAssignableFrom(obj.getClass())) {
			return false;
		}
		final AbstractProjectOrDomain other = (AbstractProjectOrDomain) obj;
		if (getName() == null) {
			if (other.getName() != null) {
				return false;
			}
		} else if (!getName().equals(other.getName())) {
			return false;
		}
		if (getType() == null) {
			if (other.getType() != null) {
				return false;
			}
		} else if (!getType().equals(other.getType())) {
			return false;
		}
		return true;
	}

	/**
	 * This is for JAXB to patchup the type
	 * 
	 * @see UnmarshallerListener
	 */
	public void beforeUnmarshal() {
		setType(getClass().getName());
	}

	/**
	 * This is for JAXB to patchup existing persistent objects for the objects
	 * that are attached directly to this object.
	 * 
	 * @param userRepository
	 * @param defaultCreatedByUser -
	 *            the user to be set as the created by if no user is supplied.
	 * @see UnmarshallerListener
	 */
	public void afterUnmarshal(UserRepository userRepository, User defaultCreatedByUser) {
		UnmarshallingContext.getInstance().addPatcher(
				new JAXBCreatedEntityPatcher(userRepository, this, defaultCreatedByUser));
		// update the references to goals
		for (Goal goal : getGoals()) {
			goal.getReferers().add(AbstractProjectOrDomain.this);
		}
	}

	/**
	 * This class is used by JAXB to convert the id of an entity into an xml id
	 * string that will be distinct from other entity xml id strings by the use
	 * of a prefix.
	 * 
	 * @author ron
	 */
	@XmlTransient
	public static class IdAdapter extends XmlAdapter<String, Long> {
		private static final String prefix = "POD_";

		@Override
		public Long unmarshal(String id) throws Exception {
			return null; // new Long(id.substring(prefix.length()));
		}

		@Override
		public String marshal(Long id) throws Exception {
			if (id != null) {
				return prefix + id.toString();
			}
			return "";
		}
	}
}