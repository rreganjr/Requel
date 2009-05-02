/*
 * $Id: PositionImpl.java,v 1.10 2009/02/15 09:31:37 rregan Exp $
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
package edu.harvard.fas.rregan.requel.annotation.impl;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext;

import edu.harvard.fas.rregan.requel.annotation.Argument;
import edu.harvard.fas.rregan.requel.annotation.Issue;
import edu.harvard.fas.rregan.requel.annotation.Position;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.user.impl.User2UserImplAdapter;
import edu.harvard.fas.rregan.requel.user.impl.UserImpl;
import edu.harvard.fas.rregan.requel.utils.jaxb.DateAdapter;
import edu.harvard.fas.rregan.requel.utils.jaxb.JAXBCreatedEntityPatcher;
import edu.harvard.fas.rregan.requel.utils.jaxb.UnmarshallerListener;

/**
 * @author ron
 */
@Entity
@Table(name = "positions")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "position_type", discriminatorType = DiscriminatorType.STRING, length = 255)
@DiscriminatorValue(value = "edu.harvard.fas.rregan.requel.annotation.impl.PositionImpl")
@XmlRootElement(name = "position", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "position", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public class PositionImpl implements Position, Serializable {
	static final long serialVersionUID = 0L;

	private Long id;
	private String type;
	private Set<Issue> issues = new TreeSet<Issue>();
	private String text;
	private Set<Argument> arguments = new TreeSet<Argument>();
	private User createdBy;
	private Date dateCreated = new Date();
	// start at 1 so hibernate recognizes the new
	// instance as the initial value and not stale.
	private int version = 1;

	/**
	 * @param text
	 * @param createdBy
	 */
	public PositionImpl(String text, User createdBy) {
		this(PositionImpl.class.getName(), text, createdBy);
	}

	/**
	 * Create a new position for the issue with the supplied text and the user.
	 * 
	 * @param type -
	 *            the class name of the issue used as the type discriminator in
	 *            the database.
	 * @param text
	 * @param createdBy
	 */
	protected PositionImpl(String type, String text, User createdBy) {
		setType(type);
		setText(text);
		setCreatedBy(createdBy);
		setDateCreated(new Date());
	}

	protected PositionImpl() {
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

	@Column(name = "position_type", insertable = false, updatable = false)
	protected String getType() {
		return type;
	}

	protected void setType(String type) {
		this.type = type;
	}

	@Version
	protected int getVersion() {
		return version;
	}

	protected void setVersion(int version) {
		this.version = version;
	}

	@XmlTransient
	@ManyToMany(targetEntity = IssueImpl.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@JoinTable(name = "position_issue", joinColumns = { @JoinColumn(name = "position_id") }, inverseJoinColumns = { @JoinColumn(name = "issue_id") })
	public Set<Issue> getIssues() {
		return issues;
	}

	protected void setIssues(Set<Issue> issues) {
		this.issues = issues;
	}

	@XmlElement(name = "text", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@XmlElementWrapper(name = "arguments", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@XmlElementRef(type = ArgumentImpl.class)
	@OneToMany(targetEntity = ArgumentImpl.class, mappedBy = "position", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	public Set<Argument> getArguments() {
		return arguments;
	}

	protected void setArguments(Set<Argument> arguments) {
		this.arguments = arguments;
	}

	@Override
	public void resolveIssue(Issue issue, User resolvedByUser) throws Exception {
		issue.resolve(this, resolvedByUser);
	}

	@Override
	public void resolveAllIssue(User resolvedByUser) throws Exception {
		for (Issue issue : getIssues()) {
			issue.resolve(this, resolvedByUser);
		}
	}

	@ManyToOne(targetEntity = UserImpl.class, cascade = { CascadeType.REFRESH }, optional = false)
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
	public int compareTo(Position o) {
		PositionImpl other = (PositionImpl) o;
		int dateCompare = (getDateCreated() == null ? -1 : getDateCreated().compareTo(
				other.getDateCreated()));
		int createdByCompare = (getCreatedBy() == null ? -1 : getCreatedBy().compareTo(
				other.getCreatedBy()));
		return (dateCompare != 0 ? dateCompare : (createdByCompare != 0 ? createdByCompare
				: (getText() == null ? -1 : getText().compareTo(other.getText()))));
	}

	private Integer tmpHashCode = null;

	@Override
	public int hashCode() {
		final int prime = 31;
		if (tmpHashCode == null) {
			if (getId() != null) {
				tmpHashCode = new Integer(getId().hashCode());
			}
			int result = 1;
			result = prime * result
					+ ((getDateCreated() == null) ? 0 : getDateCreated().hashCode());
			result = prime * result + ((getCreatedBy() == null) ? 0 : getCreatedBy().hashCode());
			result = prime * result + ((getText() == null) ? 0 : getText().hashCode());
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
		final PositionImpl other = (PositionImpl) obj;
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

	/**
	 * This is for JAXB to patchup the type
	 * 
	 * @see UnmarshallerListener
	 */
	public void beforeUnmarshal() {
		setType(getClass().getName());
	}

	/**
	 * This is for JAXB to patchup the parent/child relationship.
	 * 
	 * @param userRepository
	 * @param defaultCreatedByUser -
	 *            the user to be set as the created by if no user is supplied.
	 * @param parent
	 * @see UnmarshallerListener
	 */
	public void afterUnmarshal(UserRepository userRepository, User defaultCreatedByUser) {
		UnmarshallingContext.getInstance().addPatcher(
				new JAXBCreatedEntityPatcher(userRepository, this, defaultCreatedByUser));
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
		private static final String prefix = "POS_";

		@Override
		public Long unmarshal(String id) throws Exception {
			return null; // new Long(id.substring(prefix.length()));
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
	 * Adapter for JAXB to convert interface Position to class PositionImpl and
	 * back.
	 */
	@XmlTransient
	public static class Position2PositionImplAdapter extends XmlAdapter<PositionImpl, Position> {
		private static final String prefix = "POS_";

		@Override
		public Position unmarshal(PositionImpl entity) throws Exception {
			return entity;
		}

		@Override
		public PositionImpl marshal(Position entity) throws Exception {
			return (PositionImpl) entity;
		}
	}
}
