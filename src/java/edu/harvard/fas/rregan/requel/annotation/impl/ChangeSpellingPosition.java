/*
 * $Id: ChangeSpellingPosition.java,v 1.14 2009/01/08 10:11:23 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
