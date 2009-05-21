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

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import edu.harvard.fas.rregan.requel.user.User;

/**
 * This is a special position such that when it is chosen as the resolution of
 * an issue via resolveIssue() the annotatable that the issue is assigned to is
 * updated based on the description of what the issue problem was.
 * 
 * @author ron
 */
@Entity
@DiscriminatorValue(value = "edu.harvard.fas.rregan.requel.annotation.impl.ChangeSpellingPosition")
@XmlRootElement(name = "changeSpellingPosition", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "changeSpellingPosition", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public class ChangeSpellingPosition extends PositionImpl {
	static final long serialVersionUID = 0L;

	private String proposedWord;

	/**
	 * @param text
	 * @param createdBy
	 * @param proposedWord
	 */
	public ChangeSpellingPosition(String text, User createdBy, String proposedWord) {
		super(ChangeSpellingPosition.class.getName(), text, createdBy);
		setProposedWord(proposedWord);
	}

	protected ChangeSpellingPosition() {
		super();
		// for hibernate
	}

	/**
	 * @return the proposed word for changing the spelling of the issue word.
	 */
	@XmlAttribute(name = "proposedWord")
	public String getProposedWord() {
		return proposedWord;
	}

	/**
	 * @param proposedWord -
	 *            the suggested word that should replace the misspelled word in
	 *            the annotatable object.
	 */
	public void setProposedWord(String proposedWord) {
		this.proposedWord = proposedWord;
	}
}
