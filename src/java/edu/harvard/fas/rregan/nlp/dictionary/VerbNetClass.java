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
package edu.harvard.fas.rregan.nlp.dictionary;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * A VerbNet class. VerbNet frames are organized into a set of classes like
 * admire-31.2 and withdraw-82-1. The classes relate the semantic roles
 * containing selectional restrictions to the frames that use them.
 * 
 * @author ron
 */
@Entity
@Table(name = "vnclass")
public class VerbNetClass {

	private Long id;
	private String name;
	private VerbNetClass parent;
	private Set<VerbNetRoleRef> roleRefs = new HashSet<VerbNetRoleRef>();

	protected VerbNetClass() {
	}

	@Id
	@Column(name = "classid", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "class", length = 32)
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne(targetEntity = VerbNetClass.class, cascade = CascadeType.ALL, optional = true)
	@JoinColumn(name = "parentid", insertable = false, updatable = false, nullable = true, unique = false)
	public VerbNetClass getParent() {
		return parent;
	}

	public void setParent(VerbNetClass parent) {
		this.parent = parent;
	}

	@OneToMany(targetEntity = VerbNetRoleRef.class, cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, mappedBy = "verbNetClass")
	protected Set<VerbNetRoleRef> getRoleRefs() {
		return roleRefs;
	}

	protected void setRoleRefs(Set<VerbNetRoleRef> roleRefs) {
		this.roleRefs = roleRefs;
	}

	@Transient
	public VerbNetRoleRef getRoleRef(String type) {
		for (VerbNetRoleRef ref : getFullRoleRefs()) {
			if (type.toLowerCase().equals(ref.getVerbNetRole().getType().toLowerCase())) {
				return ref;
			}
		}
		return null;
	}

	@Transient
	private Set<VerbNetRoleRef> getFullRoleRefs() {
		Set<VerbNetRoleRef> fullSet = new HashSet<VerbNetRoleRef>();
		fullSet.addAll(getRoleRefs());
		VerbNetClass ancestor = getParent();
		while (ancestor != null) {
			for (VerbNetRoleRef roleRef : ancestor.getRoleRefs()) {
				if (!fullSet.contains(roleRef)) {
					fullSet.add(roleRef);
				}
			}
			ancestor = ancestor.getParent();
		}
		return fullSet;
	}

	private Integer tmpHashCode = null;

	@Override
	public int hashCode() {
		if (tmpHashCode == null) {
			if (getId() != null) {
				tmpHashCode = new Integer(getId().hashCode());
			}
			final int prime = 31;
			int result = 1;
			result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
			tmpHashCode = new Integer(result);
		}
		return tmpHashCode.intValue();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + getName();
	}
}
