/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.ManyToAny;

import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.user.impl.UserImpl;

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
	@GeneratedValue(strategy = GenerationType.IDENTITY)
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
	@Override
	public int getVersion() {
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
    public User getCreatedBy() {
        return createdBy;
    }

	protected void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

    @XmlAttribute(name = "dateCreated")
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
	 * @see com.rreganjr.requel.utils.jaxb.UnmarshallerListener
	 */
	public void beforeUnmarshal() {
		setType(getClass().getName());
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
