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
import jakarta.persistence.ManyToMany;
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

import org.hibernate.annotations.SortNatural;
import jakarta.validation.constraints.NotEmpty;

import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectTeam;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.utils.jaxb.UnmarshallerListener;

/**
 * @author ron
 */
@Entity
@Table(name = "teams", uniqueConstraints = { @UniqueConstraint(columnNames = {
		"projectordomain_id", "name" }) })
@XmlRootElement(name = "team", namespace = "http://www.rreganjr.com/requel")
@XmlType(name = "team", namespace = "http://www.rreganjr.com/requel")
public class ProjectTeamImpl extends AbstractProjectOrDomainEntity implements ProjectTeam {
	static final long serialVersionUID = 0L;

	private Set<UserStakeholder> members = new TreeSet<UserStakeholder>();

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
		return "TEM_" + getId();
	}

	@Transient
	public String getDescription() {
		return "Team: " + getName();
	}

	@XmlElementWrapper(name = "members", namespace = "http://www.rreganjr.com/requel")
	@XmlIDREF
	@XmlElement(name = "stakeholderRef", type = UserStakeholderImpl.class, namespace = "http://www.rreganjr.com/requel")
	@ManyToMany(targetEntity = AbstractStakeholder.class, cascade = { CascadeType.PERSIST,
			CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@JoinTable(name = "team_stakeholders", joinColumns = { @JoinColumn(name = "team_id") }, inverseJoinColumns = { @JoinColumn(name = "stakeholder_id") })
	@SortNatural
	public Set<UserStakeholder> getMembers() {
		return members;
	}

	protected void setMembers(Set<UserStakeholder> members) {
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
			if (stakeholder.isUserStakeholder()) {
				((UserStakeholderImpl) stakeholder).setTeam(this);
			}
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
		 * @see jakarta.xml.bind.annotation.adapters.XmlAdapter#marshal(java.lang.Object)
		 */
		@Override
		public ProjectTeamImpl marshal(ProjectTeam team) throws Exception {
			return (ProjectTeamImpl) team;
		}

		/**
		 * @see jakarta.xml.bind.annotation.adapters.XmlAdapter#unmarshal(java.lang.Object)
		 */
		@Override
		public ProjectTeam unmarshal(ProjectTeamImpl team) throws Exception {
			return team;
		}
	}
}
