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

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import com.rreganjr.platform.identity.User;

/**
 * Issue #459: a position that, when chosen, replaces the name of the entity the issue is on with
 * {@link #getProposedName()} (a step written as a command, rewritten to name its actor). It is
 * refused when the name is no longer {@link #getExpectedName()}, the name it was built from, so a
 * stale proposal can't overwrite a name the user has since changed.
 */
@Entity
@DiscriminatorValue(value = "com.rreganjr.requel.annotation.impl.RewriteNamePosition")
@XmlRootElement(name = "rewriteNamePosition", namespace = "http://www.rreganjr.com/requel")
@XmlType(name = "rewriteNamePosition", namespace = "http://www.rreganjr.com/requel")
public class RewriteNamePosition extends PositionImpl {
	static final long serialVersionUID = 0L;

	private String proposedName;
	private String expectedName;

	public RewriteNamePosition(String text, User createdBy, String proposedName,
			String expectedName) {
		super(RewriteNamePosition.class.getName(), text, createdBy);
		setProposedName(proposedName);
		setExpectedName(expectedName);
	}

	protected RewriteNamePosition() {
		super();
		// for hibernate
	}

	/** The new name. Stored in the column {@link ChangeSpellingPosition} keeps its word in. */
	@Column(name = "proposed_word")
	@XmlAttribute(name = "proposedName")
	public String getProposedName() {
		return proposedName;
	}

	public void setProposedName(String proposedName) {
		this.proposedName = proposedName;
	}

	/** The name the proposal was built from. */
	@Column(name = "expected_name")
	@XmlAttribute(name = "expectedName")
	public String getExpectedName() {
		return expectedName;
	}

	public void setExpectedName(String expectedName) {
		this.expectedName = expectedName;
	}
}
