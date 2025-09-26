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
package com.rreganjr.requel.annotation.impl;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import com.rreganjr.requel.user.User;

/**
 * An issue related to a word used in the text of an entity.
 * 
 * @author ron
 */
@Entity
@DiscriminatorValue(value = "com.rreganjr.requel.annotation.impl.LexicalIssue")
@XmlRootElement(name = "lexicalIssue", namespace = "http://www.rreganjr.com/requel")
@XmlType(name = "lexicalIssue", namespace = "http://www.rreganjr.com/requel")
public class LexicalIssue extends IssueImpl {
	static final long serialVersionUID = 0L;

	private String annotatableEntityPropertyName;
	private String word;

	/**
	 * @param groupingObject -
	 *            An object used as the "owner" of a group of annotations.
	 * @param text -
	 *            the text message of the annotation
	 * @param mustBeResolved -
	 *            flag indicating if this issue must be resolved before the
	 *            project can be complete.
	 * @param createdBy -
	 *            the user that created the issue
	 * @param annotatableEntityPropertyName -
	 *            the name of the property in the annotatable entity that
	 *            contains the word in question.
	 * @param word -
	 *            the word the issue is about.
	 */
	public LexicalIssue(Object groupingObject, String text, boolean mustBeResolved, User createdBy,
			String annotatableEntityPropertyName, String word) {
		super(LexicalIssue.class.getName(), groupingObject, text, mustBeResolved, createdBy);
		setWord(word);
		setAnnotatableEntityPropertyName(annotatableEntityPropertyName);
	}

	protected LexicalIssue() {
		// for hibernate
	}

	/**
	 * @return The word the issue is about.
	 */
	@XmlAttribute(name = "word")
	public String getWord() {
		return word;
	}

	/**
	 * set the word the issue is about.
	 */
	public void setWord(String word) {
		this.word = word;
	}

	/**
	 * @return The name of the property on the annotatable issue where the word
	 *         occurs.
	 */
	@XmlAttribute(name = "propertyName")
	public String getAnnotatableEntityPropertyName() {
		return annotatableEntityPropertyName;
	}

	/**
	 * @param annotatableEntityPropertyName
	 */
	public void setAnnotatableEntityPropertyName(String annotatableEntityPropertyName) {
		this.annotatableEntityPropertyName = annotatableEntityPropertyName;
	}
}
