/*
 * $Id: ProjectTeamImpl.java,v 1.16 2009/02/12 11:01:35 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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

import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.hibernate.validator.NotEmpty;

import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectTeam;
import edu.harvard.fas.rregan.requel.project.Stakeholder;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.utils.jaxb.UnmarshallerListener;

/**
 * @author ron
 */
@Entity
@Table(name = "teams", uniqueConstraints = { @UniqueConstraint(columnNames = {
		"projectordomain_id", "name" }) })
@XmlRootElement(name = "team", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "team", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public class ProjectTeamImpl extends AbstractProjectOrDomainEntity implements ProjectTeam {
	static final long serialVersionUID = 0L;

	private Set<Stakeholder> members = new TreeSet<Stakeholder>();

	/**
	 * @param projectOrDomain
	 * @param createdBy
	 * @param name
	 */
	public ProjectTeamImpl(ProjectOrDomain projectOrDomain, User createdBy, String name) {
		super(projectOrDomain, createdBy, name);
		projectOrDomain.getTeams().add(this);
	}

	protected ProjectTeamImpl() {
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
		return "TEM_" + getId();
	}

	@Transient
	public String getDescription() {
		return "Team: " + getName();
	}

	@XmlElementWrapper(name = "members", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@XmlIDREF
	@XmlElement(name = "stakeholderRef", type = StakeholderImpl.class, namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@ManyToMany(targetEntity = StakeholderImpl.class, cascade = { CascadeType.PERSIST,
			CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@JoinTable(name = "team_stakeholders", joinColumns = { @JoinColumn(name = "team_id") }, inverseJoinColumns = { @JoinColumn(name = "stakeholder_id") })
	@Sort(type = SortType.NATURAL)
	public Set<Stakeholder> getMembers() {
		return members;
	}

	protected void setMembers(Set<Stakeholder> members) {
		this.members = members;
	}

	/**
	 * This is for JAXB to fixup the parent child relationship with the
	 * canonical term.
	 * 
	 * @see UnmarshallerListener
	 */
	public void afterUnmarshal() {
		for (Stakeholder stakeholder : getMembers()) {
			((StakeholderImpl) stakeholder).setTeam(this);
		}
	}

	@Override
	public int compareTo(ProjectTeam o) {
		return getName().compareTo(o.getName());
	}

	/**
	 * Adapter for JAXB to convert interface ProjectTeam to class
	 * ProjectTeamImpl and back.
	 * 
	 * @author ron
	 */
	@XmlTransient
	public static class Team2TeamImplAdapter extends XmlAdapter<ProjectTeamImpl, ProjectTeam> {

		/**
		 * @see javax.xml.bind.annotation.adapters.XmlAdapter#marshal(java.lang.Object)
		 */
		@Override
		public ProjectTeamImpl marshal(ProjectTeam team) throws Exception {
			return (ProjectTeamImpl) team;
		}

		/**
		 * @see javax.xml.bind.annotation.adapters.XmlAdapter#unmarshal(java.lang.Object)
		 */
		@Override
		public ProjectTeam unmarshal(ProjectTeamImpl team) throws Exception {
			return team;
		}
	}
}
