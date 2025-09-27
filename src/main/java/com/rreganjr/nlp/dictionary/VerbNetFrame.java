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

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A frame from VerbNET that assigns thematic roles to syntax structures. The
 * assignment is made through psuedo-XML of the form:<br>
 * <NP value="Agent"><SYNRESTRS/></NP><VERB/><NP value="Theme"><SYNRESTRS/></NP>
 * 
 * @author ron
 */
@Entity
@Table(name = "vnframedef")
public class VerbNetFrame implements Comparable<VerbNetFrame>, Serializable {
	static final long serialVersionUID = 0;

	private Long id;
	private String name;
	private String xtagRef;
	private String description1;
	private String description2;
	private String syntax;
	private String semantics;

	protected VerbNetFrame() {
	}

	@Id
	@Column(name = "frameid", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	@Column(name = "number", length = 16)
	public String getName() {
		return this.name;
	}

	protected void setName(String name) {
		this.name = name;
	}

	@Column(name = "xtag", length = 16)
	public String getXtagRef() {
		return xtagRef;
	}

	protected void setXtagRef(String xtagRef) {
		this.xtagRef = xtagRef;
	}

	@Column(name = "description1", length = 64)
	public String getDescription1() {
		return description1;
	}

	public void setDescription1(String description1) {
		this.description1 = description1;
	}

	@Column(name = "description2", length = 64)
	public String getDescription2() {
		return description2;
	}

	protected void setDescription2(String description2) {
		this.description2 = description2;
	}

	@Column(name = "syntax", nullable = false, length = 16277215)
	public String getSyntax() {
		return syntax;
	}

	protected void setSyntax(String syntax) {
		this.syntax = syntax;
	}

	@Column(name = "semantics", nullable = false, length = 16277215)
	public String getSemantics() {
		return semantics;
	}

	public void setSemantics(String semantics) {
		this.semantics = semantics;
	}

	@Override
	public int compareTo(VerbNetFrame o) {
		return getId().compareTo(o.getId());
	}

	private Integer tmpHashCode = null;

	@Override
	public int hashCode() {
		if (tmpHashCode == null) {
			if (getId() != null) {
				tmpHashCode = Integer.valueOf(getId().hashCode());
			}
			final int prime = 31;
			int result = 1;
			result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
			result = prime * result + ((getSyntax() == null) ? 0 : getSyntax().hashCode());
			result = prime * result
					+ ((getDescription1() == null) ? 0 : getDescription1().hashCode());
			result = prime * result
					+ ((getDescription2() == null) ? 0 : getDescription2().hashCode());
			tmpHashCode = Integer.valueOf(result);
		}
		return tmpHashCode.intValue();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!getClass().isAssignableFrom(obj.getClass())) {
			return false;
		}
		final VerbNetFrame other = (VerbNetFrame) obj;
		if ((getId() != null) && getId().equals(other.getId())) {
			return true;
		}
		if (getName() == null) {
			if (other.getName() != null) {
				return false;
			}
		} else if (!getName().equals(other.getName())) {
			return false;
		}
		if (getSyntax() == null) {
			if (other.getSyntax() != null) {
				return false;
			}
		} else if (!getSyntax().equals(other.getSyntax())) {
			return false;
		}
		if (getDescription1() == null) {
			if (other.getDescription1() != null) {
				return false;
			}
		} else if (!getDescription1().equals(other.getDescription1())) {
			return false;
		}
		if (getDescription2() == null) {
			if (other.getDescription2() != null) {
				return false;
			}
		} else if (!getDescription2().equals(other.getDescription2())) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return getClass().getSimpleName() + "(" + getName() + "): " + getSyntax();
	}

}
