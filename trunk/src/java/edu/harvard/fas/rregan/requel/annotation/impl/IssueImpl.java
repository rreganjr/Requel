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
package edu.harvard.fas.rregan.requel.annotation.impl;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.xml.sax.SAXException;

import com.sun.xml.bind.v2.runtime.unmarshaller.Patcher;
import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext;

import edu.harvard.fas.rregan.requel.annotation.Issue;
import edu.harvard.fas.rregan.requel.annotation.Position;
import edu.harvard.fas.rregan.requel.annotation.impl.PositionImpl.Position2PositionImplAdapter;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.user.exception.NoSuchUserException;
import edu.harvard.fas.rregan.requel.user.impl.User2UserImplAdapter;
import edu.harvard.fas.rregan.requel.user.impl.UserImpl;
import edu.harvard.fas.rregan.requel.utils.jaxb.DateAdapter;
import edu.harvard.fas.rregan.requel.utils.jaxb.UnmarshallerListener;

/**
 * @author ron
 */
@Entity
@DiscriminatorValue(value = "edu.harvard.fas.rregan.requel.annotation.Issue")
@XmlRootElement(name = "issue", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "issue", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public class IssueImpl extends AbstractAnnotation implements Issue {
	static final long serialVersionUID = 0L;

	private Set<Position> positions = new TreeSet<Position>();
	private boolean mustBeResolved;
	private Position resolvedByPosition;
	private User resolvedByUser;
	private Date resolvedDate;

	/**
	 * @param groupingObject -
	 *            An object used as the "owner" of a group of annotations.
	 * @param text -
	 *            the text message of the annotation
	 * @param mustBeResolved -
	 *            flag indicating if this issue must be resolved before the
	 *            project can be complete.
	 * @param createdBy -
	 *            the user that created the issue
	 */
	public IssueImpl(Object groupingObject, String text, boolean mustBeResolved, User createdBy) {
		this(Issue.class.getName(), groupingObject, text, mustBeResolved, createdBy);
	}

	/**
	 * @param type -
	 *            the class name of this annotation
	 * @param groupingObject -
	 *            An object used as the "owner" of a group of annotations.
	 * @param text -
	 *            the text message of the annotation
	 * @param mustBeResolved -
	 *            flag indicating if this issue must be resolved before the
	 *            project can be complete.
	 * @param createdBy -
	 *            the user that created the issue
	 */
	protected IssueImpl(String type, Object groupingObject, String text, boolean mustBeResolved,
			User createdBy) {
		super(type, groupingObject, text, createdBy);
		setMustBeResolved(mustBeResolved);
	}

	protected IssueImpl() {
		// for hibernate
	}

	@Transient
	public String getTypeName() {
		return "Issue";
	}

	// changed xml mapping to output references to positions instead of the
	// positions directly because a position may be shared by multiple
	// issues causing duplicates on import. this makes report generating via
	// xslt more complicated because of the indirection.
	@XmlElementWrapper(name = "positions", namespace = "http://www.people.fas.harvard.edu/~rregan/requel", required = false)
	@XmlIDREF
	@XmlElement(name = "positionRef", type = PositionImpl.class, namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@ManyToMany(targetEntity = PositionImpl.class, mappedBy = "issues", cascade = {
			CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@JoinTable(name = "position_issue", joinColumns = { @JoinColumn(name = "issue_id") }, inverseJoinColumns = { @JoinColumn(name = "position_id") })
	public Set<Position> getPositions() {
		return positions;
	}

	protected void setPositions(Set<Position> positions) {
		this.positions = positions;
	}

	@XmlAttribute(name = "mustBeResolved")
	public boolean isMustBeResolved() {
		return mustBeResolved;
	}

	/**
	 * @param mustBeResolved
	 */
	public void setMustBeResolved(boolean mustBeResolved) {
		this.mustBeResolved = mustBeResolved;
	}

	@Transient
	public boolean isResolved() {
		return (resolvedByPosition != null);
	}

	@Transient
	public String getStatusMessage() {
		if (resolvedByPosition != null) {
			return "Resolution: " + resolvedByPosition.getText();
		}
		return "Unresolved";
	}

	@ManyToOne(targetEntity = PositionImpl.class, cascade = { CascadeType.MERGE,
			CascadeType.PERSIST, CascadeType.REFRESH }, optional = true)
	@XmlIDREF
	@XmlAttribute(name = "resolvedByPosition")
	@XmlJavaTypeAdapter(Position2PositionImplAdapter.class)
	public Position getResolvedByPosition() {
		return resolvedByPosition;
	}

	protected void setResolvedByPosition(Position resolvedByPosition) {
		this.resolvedByPosition = resolvedByPosition;
	}

	@Override
	public void resolve(Position resolvedByPosition, User resolvedByUser) {
		setResolvedByPosition(resolvedByPosition);
		setResolvedByUser(resolvedByUser);
		setResolvedDate(new Date());
	}

	@Override
	public void unresolve() {
		setResolvedByPosition(null);
		setResolvedByUser(null);
		setResolvedDate(null);
	}

	@ManyToOne(targetEntity = UserImpl.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH }, optional = true)
	@XmlIDREF
	@XmlAttribute(name = "resolvedByUser")
	@XmlJavaTypeAdapter(User2UserImplAdapter.class)
	public User getResolvedByUser() {
		return resolvedByUser;
	}

	protected void setResolvedByUser(User resolvedByUser) {
		this.resolvedByUser = resolvedByUser;
	}

	@XmlAttribute(name = "dateResolved")
	@XmlJavaTypeAdapter(DateAdapter.class)
	@Temporal(TemporalType.TIMESTAMP)
	public Date getResolvedDate() {
		return resolvedDate;
	}

	protected void setResolvedDate(Date resolvedDate) {
		this.resolvedDate = resolvedDate;
	}

	/**
	 * This is for JAXB to patchup the parent/child relationship and swap the
	 * resolved by user with an existing user.
	 * 
	 * @param userRepository
	 * @param annotatable
	 * @see UnmarshallerListener
	 */
	public void afterUnmarshal(final UserRepository userRepository) {
		UnmarshallingContext.getInstance().addPatcher(new Patcher() {
			@Override
			public void run() throws SAXException {
				try {
					if (getResolvedByUser() != null) {
						User existingUser = userRepository.findUserByUsername(getResolvedByUser()
								.getUsername());
						setResolvedByUser(existingUser);
					}
					// fix up the positions reference to the issue
					for (Position position : getPositions()) {
						position.getIssues().add(IssueImpl.this);
					}
				} catch (NoSuchUserException e) {
					// the new user will be persisted automatically
				}
			}
		});
	}
}
