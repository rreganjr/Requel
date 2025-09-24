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
import java.util.TreeSet;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.hibernate.annotations.SortNatural;

import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext;

import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.impl.AbstractAnnotation;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalRelation;
import com.rreganjr.requel.project.GoalRelationType;
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
@Entity
@Table(name = "goal_relations", uniqueConstraints = { @UniqueConstraint(columnNames = {"toGoalInternal_id", "fromGoalInternal_id" }) })
@XmlRootElement(name = "goalRelation", namespace = "http://www.rreganjr.com/requel")
@XmlType(name = "goalRelation", namespace = "http://www.rreganjr.com/requel")
public class GoalRelationImpl implements GoalRelation, Serializable {
	static final long serialVersionUID = 0L;

	private Long id;
	private Goal fromGoal;
	private Goal toGoal;
	private GoalRelationType relationType;
	private Set<Annotation> annotations = new TreeSet<Annotation>();
	private User createdBy;
	private Date dateCreated = new Date();
	// start at 1 so hibernate recognizes the new instance as the initial value
	// and not stale.
	private int version = 1;

	/**
	 * @param fromGoal -
	 *            the origin of the relationship, this goal has the relationship
	 *            with the toGoal
	 * @param toGoal -
	 *            the target of the relationship
	 * @param relationType -
	 *            the type of relationship, see GoalRelationType
	 * @param createdBy -
	 *            the user that created the relationship
	 */
	public GoalRelationImpl(Goal fromGoal, Goal toGoal, GoalRelationType relationType,
			User createdBy) {
		setFromGoal(fromGoal);
		setToGoal(toGoal);
		setRelationType(relationType);
		setCreatedBy(createdBy);
		setDateCreated(new Date());
	}

	protected GoalRelationImpl() {
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
	protected int getVersion() {
		return version;
	}

	protected void setVersion(int version) {
		this.version = version;
	}

	@Override
	@XmlTransient
	@Transient
	public String getDescription() {
		return "Goal Relation: " + getFromGoal().getName() + " " + getRelationType().name() + " "
				+ getToGoal().getName();
	}

	@Override
	@Enumerated(EnumType.STRING)
	@XmlAttribute(name = "relationType")
	@XmlJavaTypeAdapter(GoalRelationTypeAdapter.class)
	public GoalRelationType getRelationType() {
		return relationType;
	}

	public void setRelationType(GoalRelationType relationType) {
		this.relationType = relationType;
	}

	@Transient
	@XmlTransient
	public Goal getFromGoal() {
		return getFromGoalInternal();
	}

	public void setFromGoal(Goal fromGoal) {
		if (getFromGoalInternal() != null) {
			getFromGoalInternal().getRelationsFromThisGoal().remove(this);
		}
		setFromGoalInternal(fromGoal);
		fromGoal.getRelationsFromThisGoal().add(this);
	}

	@ManyToOne(targetEntity = GoalImpl.class, cascade = { CascadeType.PERSIST, CascadeType.REFRESH }, optional = false)
	@JoinColumn(name="fromGoalInternal_id")
	protected Goal getFromGoalInternal() {
		return fromGoal;
	}

	protected void setFromGoalInternal(Goal fromGoal) {
		this.fromGoal = fromGoal;
	}

	@Transient
	@XmlIDREF
	@XmlAttribute(name = "toGoal")
	@XmlJavaTypeAdapter(Goal2GoalImplAdapter.class)
	public Goal getToGoal() {
		return getToGoalInternal();
	}

	public void setToGoal(Goal toGoal) {
		if (getToGoalInternal() != null) {
			getToGoalInternal().getRelationsToThisGoal().remove(this);
		}
		setToGoalInternal(toGoal);
		toGoal.getRelationsToThisGoal().add(this);
	}

	@XmlTransient
	@ManyToOne(targetEntity = GoalImpl.class, cascade = { CascadeType.REFRESH }, optional = false)
	@JoinColumn(name="toGoalInternal_id")
	public Goal getToGoalInternal() {
		return toGoal;
	}

	public void setToGoalInternal(Goal toGoal) {
		this.toGoal = toGoal;
	}

	@XmlElementWrapper(name = "annotations", namespace = "http://www.rreganjr.com/requel")
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
	@SortNatural
	public Set<Annotation> getAnnotations() {
		return annotations;
	}

	protected void setAnnotations(Set<Annotation> annotations) {
		this.annotations = annotations;
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

	@Override
	public int compareTo(GoalRelation o) {
		int compareFromGoal = (getFromGoal() == null ? -1 : getFromGoal()
				.compareTo(o.getFromGoal()));
		int compareToGoal = (getToGoal() == null ? -1 : getToGoal().compareTo(o.getToGoal()));
		int compareRelationType = (getRelationType() == null ? -1 : getRelationType().compareTo(
				o.getRelationType()));
		return (compareFromGoal != 0 ? compareFromGoal : (compareToGoal != 0 ? compareToGoal
				: compareRelationType));
	}

	private Integer tmpHashCode = null;

	@Override
	public int hashCode() {
		if (tmpHashCode == null) {
			if (getId() != null) {
				tmpHashCode = Integer.valueOf(getId().hashCode());
			}
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((getFromGoalInternal() == null) ? 0 : getFromGoalInternal().hashCode());
			result = prime * result
					+ ((getToGoalInternal() == null) ? 0 : getToGoalInternal().hashCode());
			result = prime * result
					+ ((getRelationType() == null) ? 0 : getRelationType().hashCode());
			tmpHashCode = Integer.valueOf(result);
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
		final GoalRelationImpl other = (GoalRelationImpl) obj;
		if ((getId() != null) && getId().equals(other.getId())) {
			return true;
		}
		if (getFromGoalInternal() == null) {
			if (other.getFromGoalInternal() != null) {
				return false;
			}
		} else if (!getFromGoalInternal().equals(other.getFromGoalInternal())) {
			return false;
		}
		if (getToGoalInternal() == null) {
			if (other.getToGoalInternal() != null) {
				return false;
			}
		} else if (!getToGoalInternal().equals(other.getToGoalInternal())) {
			return false;
		}
		if (getRelationType() == null) {
			if (other.getRelationType() != null) {
				return false;
			}
		} else if (!getRelationType().equals(other.getRelationType())) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return GoalRelation.class.getName() + "[" + getId() + "]: " + getFromGoal().toString()
				+ " " + getRelationType().toString() + " " + getToGoal().toString();
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
		private static final String prefix = "GLR_";

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
	public void afterUnmarshal(final UserRepository userRepository, User defaultCreatedByUser,
			Object parent) {
		setFromGoal((Goal) parent);
		UnmarshallingContext.getInstance().addPatcher(new JAXBAnnotatablePatcher(this));
		UnmarshallingContext.getInstance().addPatcher(
				new JAXBCreatedEntityPatcher(userRepository, this, defaultCreatedByUser));
	}

	/**
	 * This class is used by JAXB to convert the GoalRelationType of a
	 * GoalRelation into a string for an attribute in the xml file and the
	 * reverse when unmartialling.
	 * 
	 * @author ron
	 */
	@XmlTransient
	public static class GoalRelationTypeAdapter extends XmlAdapter<String, GoalRelationType> {

		@Override
		public GoalRelationType unmarshal(String typeString) throws Exception {
			return GoalRelationType.valueOf(typeString);
		}

		@Override
		public String marshal(GoalRelationType type) throws Exception {
			return type.toString();
		}
	}
}
