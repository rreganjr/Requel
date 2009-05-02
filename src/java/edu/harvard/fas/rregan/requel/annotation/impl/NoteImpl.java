/*
 * $Id: NoteImpl.java,v 1.10 2009/02/15 09:31:37 rregan Exp $
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

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import edu.harvard.fas.rregan.requel.annotation.Note;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * @author ron
 */
@Entity
@DiscriminatorValue(value = "edu.harvard.fas.rregan.requel.annotation.Note")
@XmlRootElement(name = "note", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "note", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public class NoteImpl extends AbstractAnnotation implements Note {
	static final long serialVersionUID = 0L;

	/**
	 * @param groupingObject -
	 *            An object used as the "owner" of a group of annotations.
	 * @param text -
	 *            the text of the note.
	 * @param createdBy -
	 *            the user that added the note to the annotatable object.
	 */
	public NoteImpl(Object groupingObject, String text, User createdBy) {
		super(Note.class.getName(), groupingObject, text, createdBy);
	}

	protected NoteImpl() {
		// for hibernate
	}

	@Transient
	public String getStatusMessage() {
		return "Informational";
	}

	@Transient
	public String getTypeName() {
		return "Note";
	}

	@Transient
	public boolean isMustBeResolved() {
		return false;
	}

	@Transient
	public boolean isResolved() {
		return true;
	}
}
