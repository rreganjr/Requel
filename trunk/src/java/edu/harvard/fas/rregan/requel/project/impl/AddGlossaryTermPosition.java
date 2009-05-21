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
package edu.harvard.fas.rregan.requel.project.impl;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import edu.harvard.fas.rregan.requel.annotation.impl.PositionImpl;
import edu.harvard.fas.rregan.requel.annotation.impl.command.AnnotationCommandFactoryImpl;
import edu.harvard.fas.rregan.requel.project.impl.command.ResolveIssueWithAddGlossaryTermPositionCommandImpl;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * This is a special position such that when it is chosen as the resolution of
 * an issue via resolveIssue() the word is added to the project glossary.
 * 
 * @author ron
 */
@Entity
@DiscriminatorValue(value = "edu.harvard.fas.rregan.requel.project.impl.AddGlossaryTermPosition")
@XmlRootElement(name = "addGlossaryTermPosition", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "addGlossaryTermPosition", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public class AddGlossaryTermPosition extends PositionImpl {
	static final long serialVersionUID = 0L;

	// TODO: use spring to register the command for the positions, or make the
	// position class implement the command
	static {
		AnnotationCommandFactoryImpl.addPositionResolverCommand(AddGlossaryTermPosition.class,
				ResolveIssueWithAddGlossaryTermPositionCommandImpl.class);
	}

	/**
	 * @param text
	 * @param createdBy
	 */
	public AddGlossaryTermPosition(String text, User createdBy) {
		super(AddGlossaryTermPosition.class.getName(), text, createdBy);
	}

	protected AddGlossaryTermPosition() {
		super();
		// for hibernate
	}
}
