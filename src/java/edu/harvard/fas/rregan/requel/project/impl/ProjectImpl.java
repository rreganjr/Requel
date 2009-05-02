/*
 * $Id: ProjectImpl.java,v 1.31 2009/01/10 11:08:58 rregan Exp $
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
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.xml.sax.SAXException;

import com.sun.istack.SAXException2;
import com.sun.xml.bind.v2.runtime.unmarshaller.Patcher;
import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext;

import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.annotation.Issue;
import edu.harvard.fas.rregan.requel.annotation.Position;
import edu.harvard.fas.rregan.requel.annotation.impl.AbstractAnnotation;
import edu.harvard.fas.rregan.requel.annotation.impl.PositionImpl;
import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.project.ProjectUserRole;
import edu.harvard.fas.rregan.requel.project.Stakeholder;
import edu.harvard.fas.rregan.requel.user.Organization;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.user.impl.OrganizationImpl;
import edu.harvard.fas.rregan.requel.utils.jaxb.JAXBOrganizedEntityPatcher;
import edu.harvard.fas.rregan.requel.utils.jaxb.UnmarshallerListener;

/**
 * @author ron
 */
@Entity
@DiscriminatorValue(value = "edu.harvard.fas.rregan.requel.project.impl.ProjectImpl")
@XmlRootElement(name = "project", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "project", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public class ProjectImpl extends AbstractProjectOrDomain implements Project {
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

	@XmlElement(name = "status", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
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
	@XmlElementWrapper(name = "annotations", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
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
	@XmlElementWrapper(name = "positions", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
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

	@XmlElementWrapper(name = "annotations", namespace = "http://www.people.fas.harvard.edu/~rregan/requel", required = false)
	// changed xml mapping to output references to annotations instead of the
	// annotations directly because
	// an annotation may be shared by multiple entities causing duplicates on
	// import. this makes report
	// generating via xslt more complicated because of the indirection.
	@XmlIDREF
	@XmlElement(name = "annotationRef", type = AbstractAnnotation.class, namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	// @XmlElementRef(type = AbstractAnnotation.class)
	@ManyToMany(targetEntity = AbstractAnnotation.class, cascade = { CascadeType.PERSIST,
			CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@JoinTable(name = "project_annotations")
	public Set<Annotation> getAnnotations() {
		return annotations;
	}

	protected void setAnnotations(Set<Annotation> annotations) {
		this.annotations = annotations;
	}

	@Transient
	public Stakeholder getUserStakeholder(User user) {
		for (Stakeholder stakeholder : getStakeholders()) {
			if (user.equals(stakeholder.getUser())) {
				return stakeholder;
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

	/**
	 * This is for JAXB to patchup existing persistent objects for the objects
	 * that are attached directly to this object.
	 * 
	 * @param userRepository
	 * @param defaultCreatedByUser -
	 *            the user to be set as the created by if no user is supplied.
	 * @param parent
	 * @see UnmarshallerListener
	 */
	@Override
	public void afterUnmarshal(UserRepository userRepository, User defaultCreatedByUser) {
		super.afterUnmarshal(userRepository, defaultCreatedByUser);
		UnmarshallingContext.getInstance().addPatcher(
				new JAXBOrganizedEntityPatcher(userRepository, this));
		UnmarshallingContext.getInstance().addPatcher(new Patcher() {
			@Override
			public void run() throws SAXException {
				try {
					if (ProjectImpl.this.getCreatedBy() != null) {
						ProjectUserRole projectUserRole = ProjectImpl.this.getCreatedBy()
								.getRoleForType(ProjectUserRole.class);
						if (projectUserRole != null) {
							projectUserRole.getActiveProjects().add(ProjectImpl.this);
						}
					}
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new SAXException2(e);
				}
			}
		});
	}

	/**
	 * @param name -
	 *            project name that over rides the name in the xml file.
	 */
	public void afterUnmarshal(String name) {
		if ((name != null) && !name.trim().equals("")) {
			setName(name);
		}
	}
}
