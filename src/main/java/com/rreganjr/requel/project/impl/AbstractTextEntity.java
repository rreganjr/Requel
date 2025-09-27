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

import jakarta.persistence.Lob;
import jakarta.persistence.MappedSuperclass;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.TextEntity;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.utils.jaxb.UnmarshallerListener;

/**
 * @author ron
 */
@MappedSuperclass
@XmlType(namespace = "http://www.rreganjr.com/requel")
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
	 * @see com.rreganjr.requel.project.TextEntity#getText()
	 */
	@XmlElement(name = "text", namespace = "http://www.rreganjr.com/requel")
	@Lob
	public String getText() {
		return text;
	}

	/**
	 * @see com.rreganjr.requel.project.TextEntity#setText(java.lang.String)
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
