/*
 * $Id: AbstractTextEntity.java,v 1.10 2009/01/07 02:22:10 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.impl;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.TextEntity;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.utils.jaxb.UnmarshallerListener;

/**
 * @author ron
 */
@MappedSuperclass
@XmlType(namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public abstract class AbstractTextEntity extends AbstractProjectOrDomainEntity implements
		TextEntity {
	static final long serialVersionUID = 0L;

	private String text;

	/**
	 * @param projectOrDomain
	 * @param name
	 * @param text
	 */
	protected AbstractTextEntity(ProjectOrDomain projectOrDomain, User createdBy, String name,
			String text) {
		super(projectOrDomain, createdBy, name);
		setText(text);

	}

	protected AbstractTextEntity() {
		// for hibernate
	}

	/**
	 * @see edu.harvard.fas.rregan.requel.project.TextEntity#getText()
	 */
	@XmlElement(name = "text", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@Lob
	public String getText() {
		return text;
	}

	/**
	 * @see edu.harvard.fas.rregan.requel.project.TextEntity#setText(java.lang.String)
	 */
	@Override
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * This is for JAXB to cleanup the text of extra whitespace at the start and
	 * end of the text.
	 * 
	 * @see UnmarshallerListener
	 */
	public void afterUnmarshal() {
		if (getText() != null) {
			setText(getText().trim());
		}
	}
}
