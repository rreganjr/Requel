/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025, 2026 Ron Regan Jr. All Rights Reserved.
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

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import com.rreganjr.platform.identity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlIDREF;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import org.hibernate.annotations.OptimisticLock;
import org.xml.sax.SAXException;

import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.impl.AbstractAnnotation;
import com.rreganjr.requel.annotation.impl.PositionImpl;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.tagging.Taggable;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.user.Organization;

import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.impl.OrganizationImpl;
import com.rreganjr.requel.user.JAXBOrganizedEntityPatcher;
/**
 * @author ron
 */
@Entity
@DiscriminatorValue(value = "com.rreganjr.requel.project.impl.ProjectImpl")
@XmlRootElement(name = "project", namespace = "http://www.rreganjr.com/requel")
@XmlType(name = "project", namespace = "http://www.rreganjr.com/requel")
public class ProjectImpl extends AbstractProjectOrDomain implements Project, Taggable {
	static final long serialVersionUID = 0L;

	private Organization organization;
	private String status;
	private Set<Annotation> annotations = new TreeSet<Annotation>();

	/**
	 * @param name
	 * @param creator
	 * @param organization
	 */
	public ProjectImpl(String name, User creator, Organization organization) {
		super(ProjectImpl.class.getName(), name, creator);
		setOrganization(organization);
		setStatus("New");
	}

	protected ProjectImpl() {
		super();
		// for JAXB and hibernate
	}

	@Transient
	public String getDescription() {
		return "Project: " + getName();
	}

	@XmlElementRef(type = OrganizationImpl.class)
	@ManyToOne(targetEntity = OrganizationImpl.class, cascade = { CascadeType.PERSIST,
			CascadeType.REFRESH })
	public Organization getOrganization() {
		return organization;
	}

	public void setOrganization(Organization organization) {
		this.organization = organization;
	}

	@XmlElement(name = "status", namespace = "http://www.rreganjr.com/requel")
	public String getStatus() {
		return status;
	}

	protected void setStatus(String status) {
		this.status = status;
	}

	/**
	 * This is for JAXB to only output a single definition for each annotation.
	 * In the export file the entities include references to the annotations
	 * instead of the annotations themselves.
	 * 
	 * @return all the annotations of all project entities in a single set.
	 */
	@XmlElementWrapper(name = "annotations", namespace = "http://www.rreganjr.com/requel")
	@XmlElementRef(type = AbstractAnnotation.class)
	@Transient
	public Set<Annotation> getAllProjectEntityAnnotations() {
		Set<Annotation> annotations = new HashSet<Annotation>();
		annotations.addAll(getAnnotations());
		for (ProjectOrDomainEntity entity : getProjectEntities()) {
			annotations.addAll(entity.getAnnotations());
		}
		return annotations;
	}

	/**
	 * This is for JAXB to only output a single definition for each issue
	 * position. In the export file the issues include references to the
	 * position instead of the position themselves.
	 * 
	 * @return all the annotations of all project entities in a single set.
	 */
	@XmlElementWrapper(name = "positions", namespace = "http://www.rreganjr.com/requel")
	@XmlElementRef(type = PositionImpl.class)
	@Transient
	public Set<Position> getAllProjectEntityIssuePositions() {
		Set<Annotation> annotations = getAllProjectEntityAnnotations();
		Set<Position> positions = new HashSet<Position>();
		for (Annotation annotation : annotations) {
			if (annotation instanceof Issue) {
				positions.addAll(((Issue) annotation).getPositions());
			}
		}
		return positions;
	}

	@XmlElementWrapper(name = "annotations", namespace = "http://www.rreganjr.com/requel", required = false)
	// changed xml mapping to output references to annotations instead of the
	// annotations directly because
	// an annotation may be shared by multiple entities causing duplicates on
	// import. this makes report
	// generating via xslt more complicated because of the indirection.
	@XmlIDREF
	@XmlElement(name = "annotationRef", type = AbstractAnnotation.class, namespace = "http://www.rreganjr.com/requel")
	// @XmlElementRef(type = AbstractAnnotation.class)
	@ManyToMany(targetEntity = AbstractAnnotation.class, cascade = { CascadeType.PERSIST,
			CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@JoinTable(name = "project_annotations")
	@OptimisticLock(excluded = true)
	public Set<Annotation> getAnnotations() {
		return annotations;
	}

	protected void setAnnotations(Set<Annotation> annotations) {
		this.annotations = annotations;
	}

	@Transient
	public UserStakeholder getUserStakeholder(User user) {
	for (Stakeholder stakeholder : getStakeholders()) {
		if (stakeholder.matchesUser(user)) {
			return (UserStakeholder) stakeholder;
		}
	}
		return null;
	}

	@Override
	public int compareTo(Project o) {
		if (getName() == null) {
			return -1;
		} else if ((o == null) || (o.getName() == null)) {
			return 1;
		}
		return getName().compareToIgnoreCase(o.getName());
	}


}