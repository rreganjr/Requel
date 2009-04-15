/*
 * $Id: NoteImpl.java,v 1.10 2009/02/15 09:31:37 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
