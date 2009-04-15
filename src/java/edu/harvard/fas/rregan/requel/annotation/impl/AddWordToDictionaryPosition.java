/*
 * $Id: AddWordToDictionaryPosition.java,v 1.6 2009/01/08 10:11:23 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.annotation.impl;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import edu.harvard.fas.rregan.requel.user.User;

/**
 * This is a special position such that when it is chosen as the resolution of
 * an issue via resolveIssue() the word is added to the dictionary.
 * 
 * @author ron
 */
@Entity
@DiscriminatorValue(value = "edu.harvard.fas.rregan.requel.annotation.impl.AddWordToDictionaryPosition")
@XmlRootElement(name = "addWordToDictionaryPosition", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "addWordToDictionaryPosition", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public class AddWordToDictionaryPosition extends PositionImpl {
	static final long serialVersionUID = 0L;

	/**
	 * @param text
	 * @param createdBy
	 */
	public AddWordToDictionaryPosition(String text, User createdBy) {
		super(AddWordToDictionaryPosition.class.getName(), text, createdBy);
	}

	protected AddWordToDictionaryPosition() {
		super();
		// for hibernate
	}
}
