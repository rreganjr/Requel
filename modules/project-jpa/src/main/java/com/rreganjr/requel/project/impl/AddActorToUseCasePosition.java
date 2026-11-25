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
 * #468: a position that adds an actor to a use case, both by name. Offered on the
 * "actor-not-in-usecase" issue a step raises when it names an actor its use case doesn't have.
 */
@Entity
@DiscriminatorValue(value = "com.rreganjr.requel.project.impl.AddActorToUseCasePosition")
@XmlRootElement(name = "addActorToUseCasePosition", namespace = "http://www.rreganjr.com/requel")
@XmlType(name = "addActorToUseCasePosition", namespace = "http://www.rreganjr.com/requel")
public class AddActorToUseCasePosition extends PositionImpl {
	static final long serialVersionUID = 0L;

	private String actorName;
	private String useCaseName;

	public AddActorToUseCasePosition(String text, User createdBy, String actorName,
			String useCaseName) {
		super(AddActorToUseCasePosition.class.getName(), text, createdBy);
		setActorName(actorName);
		setUseCaseName(useCaseName);
	}

	protected AddActorToUseCasePosition() {
		super();
		// for hibernate
	}

	@Column(name = "proposed_actor_name")
	@XmlAttribute(name = "actorName")
	public String getActorName() {
		return actorName;
	}

	public void setActorName(String actorName) {
		this.actorName = actorName;
	}

	@Column(name = "proposed_use_case_name")
	@XmlAttribute(name = "useCaseName")
	public String getUseCaseName() {
		return useCaseName;
	}

	public void setUseCaseName(String useCaseName) {
		this.useCaseName = useCaseName;
	}
}
