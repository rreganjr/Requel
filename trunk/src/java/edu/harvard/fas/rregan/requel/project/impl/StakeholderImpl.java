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

import java.util.HashSet;
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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.xml.sax.SAXException;

import com.sun.istack.SAXException2;
import com.sun.xml.bind.v2.runtime.unmarshaller.Patcher;
import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext;

import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectTeam;
import edu.harvard.fas.rregan.requel.project.Stakeholder;
import edu.harvard.fas.rregan.requel.project.StakeholderPermission;
import edu.harvard.fas.rregan.requel.project.StakeholderPermissionType;
import edu.harvard.fas.rregan.requel.project.impl.ProjectTeamImpl.Team2TeamImplAdapter;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.user.exception.NoSuchUserException;
import edu.harvard.fas.rregan.requel.user.impl.UserImpl;
import edu.harvard.fas.rregan.requel.utils.jaxb.JAXBCreatedEntityPatcher;
import edu.harvard.fas.rregan.requel.utils.jaxb.UnmarshallerListener;

/**
 * @author ron
 */
@Entity
@Table(name = "stakeholders", uniqueConstraints = { @UniqueConstraint(columnNames = {
		"projectordomain_id", "name", "user_id" }) })
@XmlRootElement(name = "stakeholder", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "stakeholder", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public class StakeholderImpl extends AbstractProjectOrDomainEntity implements Stakeholder {
	static final long serialVersionUID = 0L;

	private Set<StakeholderPermission> stakeholderPermission = new HashSet<StakeholderPermission>();
	private Set<Goal> goals = new TreeSet<Goal>();
	private User user;
	private ProjectTeam team;

	/**
	 * Create a stakeholder for a user.
	 * 
	 * @param projectOrDomain
	 * @param user
	 * @param createdBy
	 * @param name
	 */
	public StakeholderImpl(ProjectOrDomain projectOrDomain, User createdBy, User user) {
		super(projectOrDomain, createdBy, null);
		this.user = user;
		// add to collection last so that sorting in the collection by entity
		// properties has access to all the properties.
		projectOrDomain.getStakeholders().add(this);
	}

	/**
	 * Create a stakeholder for a non-user entity.
	 * 
	 * @param projectOrDomain
	 * @param createdBy
	 * @param name
	 */
	public StakeholderImpl(ProjectOrDomain projectOrDomain, User createdBy, String name) {
		super(projectOrDomain, createdBy, name);
		projectOrDomain.getStakeholders().add(this);
	}

	protected StakeholderImpl() {
		// for hibernate
	}

	@Override
	@Column(nullable = true, unique = false)
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

	/**
	 * This is for JAXB to get a unique type specific id for use in an exported
	 * file.
	 */
	@Transient
	@XmlID
	@XmlAttribute(name = "id")
	public String getXmlId() {
		return "STK_" + getId();
	}

	/**
	 * This is for displaying a description in the user interface
	 */
	@XmlTransient
	@Transient
	public String getDescription() {
		if (isUserStakeholder()) {
			return "Stakeholder: " + getUser().getUsername();
		}
		return "Stakeholder: " + getName();
	}

	@Transient
	@XmlTransient
	@Override
	public boolean isUserStakeholder() {
		return (getUser() != null);
	}

	// TODO: there should be some way to mark this as optional in the xml schema
	@XmlElement(name = "user", type = UserImpl.class, nillable = true, required = false, namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	// @XmlElementRef(type = UserImpl.class)
	@ManyToOne(targetEntity = UserImpl.class, cascade = { CascadeType.REFRESH }, optional = true)
	public User getUser() {
		return user;
	}

	/**
	 * @param user
	 */
	public void setUser(User user) {
		this.user = user;
	}

	@XmlIDREF()
	@XmlAttribute(name = "team")
	@XmlJavaTypeAdapter(Team2TeamImplAdapter.class)
	@Transient
	public ProjectTeam getTeam() {
		return getTeamInternal();
	}

	public void setTeam(ProjectTeam team) {
		if (getTeamInternal() != null) {
			getTeamInternal().getMembers().remove(this);
		}
		setTeamInternal(team);
		if (team != null) {
			team.getMembers().add(this);
		}
	}

	@ManyToOne(targetEntity = ProjectTeamImpl.class, cascade = { CascadeType.MERGE,
			CascadeType.PERSIST, CascadeType.REFRESH }, optional = true)
	protected ProjectTeam getTeamInternal() {
		return team;
	}

	public void setTeamInternal(ProjectTeam team) {
		this.team = team;
	}

	@XmlElementWrapper(name = "projectPermissions", namespace = "http://www.people.fas.harvard.edu/~rregan/requel", required = false)
	@XmlElementRef(type = StakeholderPermissionImpl.class)
	@ManyToMany(targetEntity = StakeholderPermissionImpl.class, cascade = { CascadeType.MERGE,
			CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@JoinTable(name = "stakeholders_permissions", joinColumns = { @JoinColumn(name = "stakeholder_id") }, inverseJoinColumns = { @JoinColumn(name = "stakeholder_permission_id") })
	public Set<StakeholderPermission> getStakeholderPermissions() {
		return stakeholderPermission;
	}

	protected void setStakeholderPermissions(Set<StakeholderPermission> stakeholderPermission) {
		this.stakeholderPermission = stakeholderPermission;
	}

	public boolean hasPermission(Class<?> entityType, StakeholderPermissionType permissionType) {
		for (StakeholderPermission stakeholderPermission : getStakeholderPermissions()) {
			if (stakeholderPermission.getPermissionKey().equals(
					StakeholderPermissionImpl.generatePermissionKey(entityType, permissionType))) {
				return true;
			}
		}
		return false;
	}

	public boolean hasPermission(StakeholderPermission stakeholderPermission) {
		return getStakeholderPermissions().contains(stakeholderPermission);
	}

	@Override
	public void grantStakeholderPermission(StakeholderPermission stakeholderPermission) {
		getStakeholderPermissions().add(stakeholderPermission);
	}

	@Override
	public void revokeStakeholderPermission(StakeholderPermission stakeholderPermission) {
		getStakeholderPermissions().remove(stakeholderPermission);
	}

	/**
	 * @see edu.harvard.fas.rregan.requel.project.GoalContainer#getGoals()
	 */
	@Override
	@OneToMany(targetEntity = GoalImpl.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@XmlElementWrapper(name = "goals", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@XmlIDREF
	@XmlElement(name = "goalRef", type = GoalImpl.class, namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	public Set<Goal> getGoals() {
		return goals;
	}

	protected void setGoals(Set<Goal> goals) {
		this.goals = goals;
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Stakeholder o) {
		if (this == o) {
			return 0;
		}
		String thisName = (getUser() != null ? getUser().getUsername() : getName());
		String thatName = (o.getUser() != null ? o.getUser().getUsername() : o.getName());
		if (thisName == null) {
			return -1;
		}
		return thisName.compareToIgnoreCase(thatName);
	}

	private Integer tmpHashCode = null;

	@Override
	public int hashCode() {
		if (tmpHashCode == null) {
			if (getId() != null) {
				tmpHashCode = new Integer(getId().hashCode());
			}
			final int prime = 31;
			int result = 1;
			result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
			result = prime * result + ((getUser() == null) ? 0 : getUser().hashCode());
			tmpHashCode = new Integer(result);
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
		if (!getClass().isAssignableFrom(obj.getClass())) {
			return false;
		}
		final StakeholderImpl other = (StakeholderImpl) obj;
		if ((getId() != null) && getId().equals(other.getId())) {
			return true;
		}
		if (getName() == null) {
			if (other.getName() != null) {
				return false;
			}
		} else if (!getName().equals(other.getName())) {
			return false;
		}
		if (getUser() == null) {
			if (other.getUser() != null) {
				return false;
			}
		} else if (!getUser().equals(other.getUser())) {
			return false;
		}
		return true;
	}

	/**
	 * This is for JAXB to patchup the parent/child relationship and to patchup
	 * existing persistent objects for the objects that are attached directly to
	 * this object.
	 * 
	 * @param userRepository
	 * @param defaultCreatedByUser -
	 *            the user to be set as the created by if no user is supplied.
	 * @see UnmarshallerListener
	 */
	public void afterUnmarshal(final UserRepository userRepository, User defaultCreatedByUser) {
		UnmarshallingContext.getInstance().addPatcher(
				new JAXBCreatedEntityPatcher(userRepository, this, defaultCreatedByUser));
		UnmarshallingContext.getInstance().addPatcher(new Patcher() {
			@Override
			public void run() throws SAXException {
				try {
					if (StakeholderImpl.this.getUser() != null) {
						try {
							User existingUser = userRepository
									.findUserByUsername(StakeholderImpl.this.getUser()
											.getUsername());
							StakeholderImpl.this.setUser(existingUser);
						} catch (NoSuchUserException e) {
							// new organization
							userRepository.persist(StakeholderImpl.this.getUser());
						}
					}

					// update the references to goals
					for (Goal goal : getGoals()) {
						goal.getReferers().add(StakeholderImpl.this);
					}
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new SAXException2(e);
				}
			}
		});
	}
}
