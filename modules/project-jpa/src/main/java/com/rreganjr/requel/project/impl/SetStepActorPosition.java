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
 * #468: a position that gives the step (or scenario) its issue is on an actor role: the name
 * becomes {@link #getProposedName()}, the action alone, and the role says who does it. It is
 * refused once the name is no longer {@link #getExpectedName()}, the one it was built from.
 */
@Entity
@DiscriminatorValue(value = "com.rreganjr.requel.project.impl.SetStepActorPosition")
@XmlRootElement(name = "setStepActorPosition", namespace = "http://www.rreganjr.com/requel")
@XmlType(name = "setStepActorPosition", namespace = "http://www.rreganjr.com/requel")
public class SetStepActorPosition extends PositionImpl {
	static final long serialVersionUID = 0L;

	private String proposedName;
	private String expectedName;
	private String actorRoleName;
	private String actorName;

	public SetStepActorPosition(String text, User createdBy, String proposedName,
			String expectedName, String actorRoleName, String actorName) {
		super(SetStepActorPosition.class.getName(), text, createdBy);
		setProposedName(proposedName);
		setExpectedName(expectedName);
		setActorRoleName(actorRoleName);
		setActorName(actorName);
	}

	protected SetStepActorPosition() {
		super();
		// for hibernate
	}

	/** The action the step's name becomes. Stored in the column #459's rewrite uses. */
	@Column(name = "proposed_word")
	@XmlAttribute(name = "proposedName")
	public String getProposedName() {
		return proposedName;
	}

	public void setProposedName(String proposedName) {
		this.proposedName = proposedName;
	}

	/** The step's name when the proposal was built. */
	@Column(name = "expected_name")
	@XmlAttribute(name = "expectedName")
	public String getExpectedName() {
		return expectedName;
	}

	public void setExpectedName(String expectedName) {
		this.expectedName = expectedName;
	}

	/** PRIMARY_ACTOR, SYSTEM or NAMED. */
	@Column(name = "proposed_actor_role", length = 32)
	@XmlAttribute(name = "actorRole")
	public String getActorRoleName() {
		return actorRoleName;
	}

	public void setActorRoleName(String actorRoleName) {
		this.actorRoleName = actorRoleName;
	}

	/** The actor a NAMED role names, by name. */
	@Column(name = "proposed_actor_name")
	@XmlAttribute(name = "actorName")
	public String getActorName() {
		return actorName;
	}

	public void setActorName(String actorName) {
		this.actorName = actorName;
	}
}
