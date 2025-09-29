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
package com.rreganjr.requel.annotation.impl;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlIDREF;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.ManyToAny;

import org.glassfish.jaxb.runtime.v2.runtime.unmarshaller.UnmarshallingContext;

import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.project.impl.ActorImpl;
import com.rreganjr.requel.project.impl.GlossaryTermImpl;
import com.rreganjr.requel.project.impl.GoalImpl;
import com.rreganjr.requel.project.impl.GoalRelationImpl;
import com.rreganjr.requel.project.impl.NonUserStakeholderImpl;
import com.rreganjr.requel.project.impl.ProjectImpl;
import com.rreganjr.requel.project.impl.ProjectTeamImpl;
import com.rreganjr.requel.project.impl.ScenarioImpl;
import com.rreganjr.requel.project.impl.StepImpl;
import com.rreganjr.requel.project.impl.StoryImpl;
import com.rreganjr.requel.project.impl.UseCaseImpl;
import com.rreganjr.requel.project.impl.UserStakeholderImpl;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.impl.User2UserImplAdapter;
import com.rreganjr.requel.user.impl.UserImpl;
import com.rreganjr.requel.utils.jaxb.DateAdapter;
import com.rreganjr.requel.utils.jaxb.JAXBAnnotationGroupedByPatcher;
import com.rreganjr.requel.utils.jaxb.JAXBCreatedEntityPatcher;
import com.rreganjr.requel.utils.jaxb.UnmarshallerListener;

/**
 * @author ron
 */
@Entity
@Table(name = "annotations")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "annotation_type", discriminatorType = DiscriminatorType.STRING, length = 255)
@XmlType(namespace = "http://www.rreganjr.com/requel")
public abstract class AbstractAnnotation implements Annotation, Serializable {
	static final long serialVersionUID = 0L;

	private Long id;
	private Object groupingObject;
	private String text;
	private String type;
	private Set<Annotatable> annotatables = new HashSet<Annotatable>();
	// private Set<Annotation> annotations = new TreeSet<Annotation>();
	private User createdBy;
	private Date dateCreated = new Date();
	private int version = 1; // start at 1 so hibernate recognizes the new

	// instance as the initial value and not stale.

	protected AbstractAnnotation(String type, Object groupingObject, String text, User createdBy) {
		setType(type);
		setGroupingObject(groupingObject);
		setText(text);
		setCreatedBy(createdBy);
		setDateCreated(new Date());
	}

	protected AbstractAnnotation() {
		// for hibernate
	}

	@Override
	@Transient
	public String getDescription() {
		return getTypeName() + ":" + getId();
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@XmlID
	@XmlAttribute(name = "id")
	@XmlJavaTypeAdapter(IdAdapter.class)
	public Long getId() {
		return id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	@Column(name = "annotation_type", insertable = false, updatable = false)
	protected String getType() {
		return type;
	}

	protected void setType(String type) {
		this.type = type;
	}

	/**
	 * @return An object used as the "owner" of a group of annotations.
	 */
    @Column(name = "grouping_object_type", length = 255)
    @Any(optional = false)
    @AnyDiscriminator(DiscriminatorType.STRING)
    @AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.Project", entity = ProjectImpl.class)
    @AnyKeyJavaClass(Long.class)
	@JoinColumn(name = "grouping_object_id")
	@XmlTransient
	public Object getGroupingObject() {
		return groupingObject;
	}

	/**
	 * Set the object used as the "owner" of a group of annotations.
	 * 
	 * @param groupingObject
	 */
	// this needs to be public for JAXB import
	public void setGroupingObject(Object groupingObject) {
		this.groupingObject = groupingObject;
	}

	@Version
	protected int getVersion() {
		return version;
	}

	protected void setVersion(int version) {
		this.version = version;
	}

	/**
	 * @return the entity that is annotated by this annotation.
	 */
	// TODO: it would be better if this wasn't dependent on the classes being
	// mapped.
    @Column(name = "annotatable_type", length = 255, nullable = false)
    @ManyToAny(fetch = FetchType.LAZY)
    @AnyDiscriminator(DiscriminatorType.STRING)
    @AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.annotation.Annotation", entity = AbstractAnnotation.class)
    @AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.Project", entity = ProjectImpl.class)
    @AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.ProjectTeam", entity = ProjectTeamImpl.class)
    @AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.Goal", entity = GoalImpl.class)
    @AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.GoalRelation", entity = GoalRelationImpl.class)
    @AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.UseCase", entity = UseCaseImpl.class)
    @AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.Scenario", entity = ScenarioImpl.class)
    @AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.Step", entity = StepImpl.class)
    @AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.Story", entity = StoryImpl.class)
    @AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.Actor", entity = ActorImpl.class)
    @AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.GlossaryTerm", entity = GlossaryTermImpl.class)
    @AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.NonUserStakeholder", entity = NonUserStakeholderImpl.class)
    @AnyDiscriminatorValue(discriminator = "com.rreganjr.requel.project.UserStakeholder", entity = UserStakeholderImpl.class)
    @AnyKeyJavaClass(Long.class)
	@JoinTable(name = "annotation_annotatable",
			joinColumns = @JoinColumn(name = "annotation_id"),
			inverseJoinColumns = @JoinColumn(name = "annotatable_id")
	)
	public Set<Annotatable> getAnnotatables() {
		return annotatables;
	}

	protected void setAnnotatables(Set<Annotatable> annotatables) {
		this.annotatables = annotatables;
	}

	@Lob
	@XmlElement(name = "text", namespace = "http://www.rreganjr.com/requel")
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public int compareTo(Annotation o) {
		AbstractAnnotation other = (AbstractAnnotation) o;
		int typeCompare = (getType() == null ? -1 : getType().compareTo(other.getType()));
		return (typeCompare != 0 ? typeCompare : getText().compareTo(other.getText()));
	}

	private Integer tmpHashCode = null;

	@Override
	public int hashCode() {
		final int prime = 31;
		if (tmpHashCode == null) {
			if (getId() != null) {
				tmpHashCode = getId().hashCode();
			} else {
				int result = 1;
				result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
				result = prime * result + ((getText() == null) ? 0 : getText().hashCode());
				tmpHashCode = Integer.valueOf(result);
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
		if (!getClass().isAssignableFrom(obj.getClass())) {
			return false;
		}
		final AbstractAnnotation other = (AbstractAnnotation) obj;
		if ((getId() != null) && getId().equals(other.getId())) {
			return true;
		}
		if (getText() == null) {
			if (other.getText() != null) {
				return false;
			}
		} else if (!getText().equals(other.getText())) {
			return false;
		}
		return true;
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
	 * This is for JAXB to patchup the type
	 * 
	 * @see UnmarshallerListener
	 */
	public void beforeUnmarshal() {
		setType(getClass().getName());
	}

	/**
	 * This is for JAXB to patchup the parent/child relationship and swap the
	 * creator with an existing user.
	 * 
	 * @param userRepository
	 * @param defaultCreatedByUser -
	 *            the user to be set as the created by if no user is supplied.
	 * @param annotatable
	 * @see UnmarshallerListener
	 */
	public void afterUnmarshal(UserRepository userRepository, User defaultCreatedByUser,
			Object annotatable) {
		UnmarshallingContext.getInstance().addPatcher(
				new JAXBCreatedEntityPatcher(userRepository, this, defaultCreatedByUser));
		UnmarshallingContext.getInstance().addPatcher(
				new JAXBAnnotationGroupedByPatcher(this, (Annotatable) annotatable));
	}

	/**
	 * This class is used by JAXB to convert the id of an entity into an xml id
	 * string that will be distinct from other entity xml id strings by the use
	 * of a prefix.
	 * 
	 * @author ron
	 */
	@XmlTransient
	protected static class IdAdapter extends XmlAdapter<String, Long> {
		private static final String prefix = "ANN_";

		@Override
		public Long unmarshal(String id) throws Exception {
			return null; // Long.valueOf(id.substring(prefix.length()));
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
