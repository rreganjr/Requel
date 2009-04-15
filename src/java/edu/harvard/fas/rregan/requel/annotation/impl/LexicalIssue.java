/*
 * $Id: LexicalIssue.java,v 1.7 2009/03/22 11:08:23 rregan Exp $
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
 * An issue related to a word used in the text of an entity.
 * 
 * @author ron
 */
@Entity
@DiscriminatorValue(value = "edu.harvard.fas.rregan.requel.annotation.impl.LexicalIssue")
@XmlRootElement(name = "lexicalIssue", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "lexicalIssue", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
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
