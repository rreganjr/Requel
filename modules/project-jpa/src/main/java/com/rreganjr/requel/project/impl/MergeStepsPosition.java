/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.impl.PositionImpl;

/**
 * Issue #472: a position that merges two steps in a row of a scenario into one: the first step
 * takes {@link #getProposedName()} and the second is removed from the scenario. The steps are
 * identified by the names the merge was built from (step names are unique in a project), and the
 * merge is refused once the scenario no longer has them in that order.
 */
@Entity
@DiscriminatorValue(value = "com.rreganjr.requel.project.impl.MergeStepsPosition")
@XmlRootElement(name = "mergeStepsPosition", namespace = "http://www.rreganjr.com/requel")
@XmlType(name = "mergeStepsPosition", namespace = "http://www.rreganjr.com/requel")
public class MergeStepsPosition extends PositionImpl {
	static final long serialVersionUID = 0L;

	private String proposedName;
	private String expectedName;
	private String expectedSecondName;

	public MergeStepsPosition(String text, User createdBy, String proposedName,
			String expectedName, String expectedSecondName) {
		super(MergeStepsPosition.class.getName(), text, createdBy);
		setProposedName(proposedName);
		setExpectedName(expectedName);
		setExpectedSecondName(expectedSecondName);
	}

	protected MergeStepsPosition() {
		super();
		// for hibernate
	}

	/** The merged name. Stored in the column #459's rewrite keeps its name in. */
	@Column(name = "proposed_word")
	@XmlAttribute(name = "proposedName")
	public String getProposedName() {
		return proposedName;
	}

	public void setProposedName(String proposedName) {
		this.proposedName = proposedName;
	}

	/** The first step's name when the merge was built. */
	@Column(name = "expected_name")
	@XmlAttribute(name = "expectedName")
	public String getExpectedName() {
		return expectedName;
	}

	public void setExpectedName(String expectedName) {
		this.expectedName = expectedName;
	}

	/** The second step's name when the merge was built. */
	@Column(name = "expected_second_name")
	@XmlAttribute(name = "expectedSecondName")
	public String getExpectedSecondName() {
		return expectedSecondName;
	}

	public void setExpectedSecondName(String expectedSecondName) {
		this.expectedSecondName = expectedSecondName;
	}
}
