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

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import com.rreganjr.requel.annotation.impl.PositionImpl;
import com.rreganjr.requel.annotation.impl.command.AnnotationCommandFactoryImpl;
import com.rreganjr.requel.project.impl.command.ResolveIssueWithAddActorPositionCommandImpl;
import com.rreganjr.requel.user.User;

/**
 * This is a special position such that when it is chosen as the resolution of
 * an issue via resolveIssue() the word is added as an actor to the project.
 * 
 * @author ron
 */
@Entity
@DiscriminatorValue(value = "com.rreganjr.requel.project.impl.AddActorPosition")
@XmlRootElement(name = "addActorPosition", namespace = "http://www.rreganjr.com/requel")
@XmlType(name = "addActorPosition", namespace = "http://www.rreganjr.com/requel")
public class AddActorPosition extends PositionImpl {
	static final long serialVersionUID = 0L;

	// TODO: use spring to register the command for the positions, or make the
	// position class implement the command
	static {
		AnnotationCommandFactoryImpl.addPositionResolverCommand(AddActorPosition.class,
				ResolveIssueWithAddActorPositionCommandImpl.class);
	}

	/**
	 * @param text
	 * @param createdBy
	 */
	public AddActorPosition(String text, User createdBy) {
		super(AddActorPosition.class.getName(), text, createdBy);
	}

	protected AddActorPosition() {
		super();
		// for hibernate
	}
}
