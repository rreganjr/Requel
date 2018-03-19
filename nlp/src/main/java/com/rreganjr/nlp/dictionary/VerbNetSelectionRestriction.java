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

package com.rreganjr.nlp.dictionary;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * A restriction includes or excludes a restriction type.
 * 
 * @author ron
 */
@Entity
@Table(name = "vnroleselres")
public class VerbNetSelectionRestriction {

	private Long id;
	private VerbNetRoleRef parent;
	private VerbNetSelectionRestrictionType type;
	private String include;

	/**
	 * @param parent
	 * @param type
	 * @param include
	 */
	public VerbNetSelectionRestriction(VerbNetRoleRef parent, VerbNetSelectionRestrictionType type,
			String include) {
		setParent(parent);
		setType(type);
		setInclude(include);
	}

	protected VerbNetSelectionRestriction() {
		// for hibernate
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "roleselresid", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(targetEntity = VerbNetRoleRef.class, cascade = CascadeType.ALL, optional = false)
	@JoinColumn(name = "rolerefid", nullable = false, unique = false)
	public VerbNetRoleRef getParent() {
		return parent;
	}

	protected void setParent(VerbNetRoleRef parent) {
		this.parent = parent;
	}

	@ManyToOne(targetEntity = VerbNetSelectionRestrictionType.class, cascade = CascadeType.ALL, optional = false)
	@JoinColumn(name = "roletypeid", nullable = false, unique = false)
	public VerbNetSelectionRestrictionType getType() {
		return type;
	}

	protected void setType(VerbNetSelectionRestrictionType type) {
		this.type = type;
	}

	public String getInclude() {
		return include;
	}

	protected void setInclude(String include) {
		this.include = include;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + getInclude() + getType();
	}
}
