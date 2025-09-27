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

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

/**
 * @author ron
 */
@Entity
@Table(name = "semlinkref")
@XmlRootElement(name = "semlink")
public class Semlinkref implements Comparable<Semlinkref>, Serializable {
	static final long serialVersionUID = 0;

	private SemlinkrefId id;
	private Synset fromSynset;
	private Synset toSynset;
	private Linkdef linkType;

	public Semlinkref() {
	}

	public Semlinkref(SemlinkrefId id) {
		this.id = id;
	}

	@EmbeddedId
	@AttributeOverrides( {
			@AttributeOverride(name = "synset1id", column = @Column(name = "synset1id", nullable = false)),
			@AttributeOverride(name = "synset2id", column = @Column(name = "synset2id", nullable = false)),
			@AttributeOverride(name = "linkid", column = @Column(name = "linkid", nullable = false)),
			@AttributeOverride(name = "distance", column = @Column(name = "distance", nullable = false)) })
	public SemlinkrefId getId() {
		return this.id;
	}

	public void setId(SemlinkrefId id) {
		this.id = id;
	}

	@ManyToOne(targetEntity = Synset.class, cascade = CascadeType.ALL, optional = false)
	@JoinColumn(name = "synset1id", insertable = false, updatable = false)
	@XmlTransient
	public Synset getFromSynset() {
		return fromSynset;
	}

	public void setFromSynset(Synset fromSynset) {
		this.fromSynset = fromSynset;
	}

	@ManyToOne(targetEntity = Synset.class, cascade = CascadeType.ALL, optional = true)
	@JoinColumn(name = "synset2id", insertable = false, updatable = false)
	@XmlTransient
	public Synset getToSynset() {
		return toSynset;
	}

	public void setToSynset(Synset toSynset) {
		this.toSynset = toSynset;
	}

	@ManyToOne(targetEntity = Linkdef.class, cascade = CascadeType.ALL, optional = true)
	@JoinColumn(name = "linkid", insertable = false, updatable = false)
	@XmlTransient
	public Linkdef getLinkType() {
		return linkType;
	}

	public void setLinkType(Linkdef linkType) {
		this.linkType = linkType;
	}

	@Transient
	public int getDistance() {
		return getId().getDistance();
	}

	protected void setDistance(int distance) {
		getId().setDistance(distance);
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
			result = prime * result + ((getFromSynset() == null) ? 0 : getFromSynset().hashCode());
			result = prime * result + ((getToSynset() == null) ? 0 : getToSynset().hashCode());
			result = prime * result + ((getLinkType() == null) ? 0 : getLinkType().hashCode());
			result = prime * result + getDistance();
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
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Semlinkref other = (Semlinkref) obj;
		if ((getId() != null) && (other.getId() != null)) {
			return getId().equals(other.getId());
		}

		if ((getId() != null) && (other.getId() != null)) {
			return getId().equals(other.getId());
		}

		if (getFromSynset() == null) {
			if (other.getFromSynset() != null) {
				return false;
			}
		} else if (!getFromSynset().equals(other.getFromSynset())) {
			return false;

		}

		if (getLinkType() == null) {
			if (other.getLinkType() != null) {
				return false;
			}
		} else if (!getLinkType().equals(other.getLinkType())) {
			return false;
		}

		if (getToSynset() == null) {
			if (other.getToSynset() != null) {
				return false;
			}
		} else if (!getToSynset().equals(other.getToSynset())) {
			return false;
		}

		return true;
	}

	@Override
	public int compareTo(Semlinkref o) {
		return getId().compareTo(o.getId());
	}
}
