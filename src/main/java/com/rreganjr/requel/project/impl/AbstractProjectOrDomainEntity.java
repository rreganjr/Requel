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

import java.io.Serializable;
import java.util.Date;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortNatural;
import org.hibernate.annotations.SortType;

import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.impl.AbstractAnnotation;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.impl.User2UserImplAdapter;
import com.rreganjr.requel.user.impl.UserImpl;
import com.rreganjr.requel.utils.jaxb.DateAdapter;
import com.rreganjr.requel.utils.jaxb.JAXBAnnotatablePatcher;
import com.rreganjr.requel.utils.jaxb.JAXBCreatedEntityPatcher;
import com.rreganjr.requel.utils.jaxb.UnmarshallerListener;

/**
 * @author ron
 */
@MappedSuperclass
@XmlType(namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public abstract class AbstractProjectOrDomainEntity implements ProjectOrDomainEntity, Serializable {
	static final long serialVersionUID = 0L;

	private Long id;
	private String name;
	private ProjectOrDomain projectOrDomain;
	private Set<Annotation> annotations = new TreeSet<Annotation>();
	private SortedSet<GlossaryTerm> terms = new TreeSet<GlossaryTerm>();
	private User createdBy;
	private Date dateCreated = new Date();
	private int version = 1; // start at 1 so hibernate recognizes the new

	// instance as the initial value and not stale.

	protected AbstractProjectOrDomainEntity(ProjectOrDomain projectOrDomain, User createdBy,
			String name) {
		setProjectOrDomain(projectOrDomain);
		setCreatedBy(createdBy);
		setName(name);
		setDateCreated(new Date());
	}

	protected AbstractProjectOrDomainEntity() {
		// for hibernate
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
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

	// NOTE: this is transient and must be over ridden in sub class so that the
	// behavior (required or uniqueness can be specified in the sub class.
	@Transient
	@XmlTransient
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	@XmlTransient
	@ManyToOne(targetEntity = AbstractProjectOrDomain.class, cascade = { CascadeType.PERSIST, CascadeType.REFRESH }, optional = false)
//	@JoinColumn(name="projectordomain_id", insertable = false, updatable = false)
	@JoinColumn(name="projectordomain_id")
	public ProjectOrDomain getProjectOrDomain() {
		return projectOrDomain;
	}

	protected void setProjectOrDomain(ProjectOrDomain projectOrDomain) {
		this.projectOrDomain = projectOrDomain;
	}

	@XmlElementWrapper(name = "annotations", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@XmlIDREF
	@XmlElement(name = "annotationRef", type = AbstractAnnotation.class, namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@ManyToMany(targetEntity = AbstractAnnotation.class, cascade = { CascadeType.PERSIST,
			CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@SortNatural
	public Set<Annotation> getAnnotations() {
		return annotations;
	}

	protected void setAnnotations(Set<Annotation> annotations) {
		this.annotations = annotations;
	}

	@XmlElementWrapper(name = "glossaryTerms", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@XmlIDREF
	@XmlElement(name = "glossaryTermRef", type = GlossaryTermImpl.class, namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@ManyToMany(targetEntity = GlossaryTermImpl.class, cascade = { CascadeType.PERSIST,
			CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@SortNatural
	public Set<GlossaryTerm> getGlossaryTerms() {
		return terms;
	}

	protected void setGlossaryTerms(SortedSet<GlossaryTerm> terms) {
		this.terms = terms;
	}

	@ManyToOne(targetEntity = UserImpl.class, cascade = { CascadeType.PERSIST, CascadeType.REFRESH }, optional = false)
	@XmlIDREF()
	@XmlAttribute(name = "createdBy")
	@XmlJavaTypeAdapter(User2UserImplAdapter.class)
	public User getCreatedBy() {
		return createdBy;
	}

	protected void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
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

	/**
	 * @return The interface of this object that is a subclass of
	 *         ProjectOrDomainEntity interface, basically getting the type of
	 *         domain entity of a subclass.
	 */
	@Transient
	public Class<?> getProjectOrDomainEntityInterface() {
		Class<?> type = getClass();
		while (AbstractProjectOrDomainEntity.class.isAssignableFrom(type)) {
			for (Class<?> face : type.getInterfaces()) {
				if (ProjectOrDomainEntity.class.isAssignableFrom(face)) {
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
		return getProjectOrDomainEntityInterface().toString() + "[" + getId() + "]:" + getName();
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
				result = prime * result + getProjectOrDomainEntityInterface().hashCode();
				result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
				result = prime * result
						+ ((getProjectOrDomain() == null) ? 0 : getProjectOrDomain().hashCode());
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
		final AbstractProjectOrDomainEntity other = (AbstractProjectOrDomainEntity) obj;
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
		if (getProjectOrDomain() == null) {
			if (other.getProjectOrDomain() != null) {
				return false;
			}
		} else if (!getProjectOrDomain().equals(other.getProjectOrDomain())) {
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
	 * @param parent
	 * @see UnmarshallerListener
	 */
	public void afterUnmarshal(UserRepository userRepository, User defaultCreatedByUser,
			Object parent) {
		setProjectOrDomain((ProjectOrDomain) parent);

		UnmarshallingContext.getInstance().addPatcher(new JAXBAnnotatablePatcher(this));
		UnmarshallingContext.getInstance().addPatcher(
				new JAXBCreatedEntityPatcher(userRepository, this, defaultCreatedByUser));
	}
}
