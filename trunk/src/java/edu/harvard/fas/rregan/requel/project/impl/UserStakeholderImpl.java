/*
 * $Id: $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * 
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

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
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
import edu.harvard.fas.rregan.requel.project.UserStakeholder;
import edu.harvard.fas.rregan.requel.project.impl.ProjectTeamImpl.Team2TeamImplAdapter;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.user.exception.NoSuchUserException;
import edu.harvard.fas.rregan.requel.user.impl.UserImpl;
import edu.harvard.fas.rregan.requel.utils.jaxb.JAXBCreatedEntityPatcher;
import edu.harvard.fas.rregan.requel.utils.jaxb.UnmarshallerListener;

/**
 * A stakeholder that is a user.
 * 
 * @author ron
 */
@Entity
@DiscriminatorValue(value = "edu.harvard.fas.rregan.requel.project.UserStakeholder")
@XmlRootElement(name = "user-stakeholder", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "user-stakeholder", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public class UserStakeholderImpl extends AbstractStakeholder implements UserStakeholder {
	static final long serialVersionUID = 0L;

	private Set<StakeholderPermission> stakeholderPermission = new HashSet<StakeholderPermission>();
	private ProjectTeam team;

	/**
	 * Create a stakeholder for a user.
	 * 
	 * @param projectOrDomain
	 * @param user
	 * @param createdBy
	 * @param name
	 */
	public UserStakeholderImpl(ProjectOrDomain projectOrDomain, User createdBy, User user) {
		super(UserStakeholder.class.getName(), projectOrDomain, createdBy, null);
		setUser(user);
		// add to collection last so that sorting in the collection by entity
		// properties has access to all the properties.
		projectOrDomain.getStakeholders().add(this);
	}

	protected UserStakeholderImpl() {
		// for hibernate
	}

	@Override
	@Transient
	public boolean isUserStakeholder() {
		return true;
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
		return "Stakeholder: " + getUser().getDescriptiveName();
	}

	@XmlElement(name = "user", type = UserImpl.class, nillable = false, required = true, namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@Transient
	public User getUser() {
		return super.getUser();
	}

	/**
	 * @param user
	 */
	public void setUser(User user) {
		super.setUser(user);
	}

	@XmlIDREF()
	@XmlAttribute(name = "team")
	@XmlJavaTypeAdapter(Team2TeamImplAdapter.class)
	@Transient
	public ProjectTeam getTeam() {
		return getTeamInternal();
	}

	/**
	 * @param team
	 */
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

	protected void setTeamInternal(ProjectTeam team) {
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
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Stakeholder o) {
		if (this == o) {
			return 0;
		}
		return getDescription().compareToIgnoreCase(o.getDescription());
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
		final UserStakeholderImpl other = (UserStakeholderImpl) obj;
		if ((getId() != null) && getId().equals(other.getId())) {
			return true;
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
					if (UserStakeholderImpl.this.getUser() != null) {
						try {
							User existingUser = userRepository
									.findUserByUsername(UserStakeholderImpl.this.getUser()
											.getUsername());
							UserStakeholderImpl.this.setUser(existingUser);
						} catch (NoSuchUserException e) {
							// new organization
							userRepository.persist(UserStakeholderImpl.this.getUser());
						}
					}

					// update the references to goals
					for (Goal goal : getGoals()) {
						goal.getReferers().add(UserStakeholderImpl.this);
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
