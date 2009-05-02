/*
 * $Id: VerbNetFrameRef.java,v 1.3 2009/02/09 10:12:30 rregan Exp $
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

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * A reference between a VerbNet frame and a WordNet sense.
 * 
 * @author ron
 */
@Entity
@Table(name = "vnframeref")
public class VerbNetFrameRef implements Comparable<VerbNetFrameRef>, Serializable {
	static final long serialVersionUID = 0;
	private Long id;
	private Sense sense;
	private VerbNetFrame frame;
	private VerbNetClass vnClass;

	protected VerbNetFrameRef() {
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "framerefid", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return The VerbNet frame of this reference.
	 */
	@ManyToOne(targetEntity = VerbNetFrame.class, cascade = CascadeType.ALL, optional = false)
	@JoinColumn(name = "frameid", insertable = false, updatable = false, nullable = true, unique = false)
	public VerbNetFrame getFrame() {
		return frame;
	}

	protected void setFrame(VerbNetFrame frame) {
		this.frame = frame;
	}

	@ManyToOne(targetEntity = VerbNetClass.class, cascade = CascadeType.ALL, optional = false)
	@JoinColumn(name = "classid", insertable = false, updatable = false, nullable = false, unique = false)
	public VerbNetClass getVnClass() {
		return vnClass;
	}

	protected void setVnClass(VerbNetClass vnClass) {
		this.vnClass = vnClass;
	}

	/**
	 * @return The WordNet sense of this frame reference
	 */
	@ManyToOne
	@JoinColumns( {
			@JoinColumn(name = "synsetid", referencedColumnName = "synsetid", insertable = false, updatable = false, nullable = false, unique = false),
			@JoinColumn(name = "wordid", referencedColumnName = "wordid", insertable = false, updatable = false, nullable = false, unique = false) })
	public Sense getSense() {
		return sense;
	}

	protected void setSense(Sense sense) {
		this.sense = sense;
	}

	@Override
	public int compareTo(VerbNetFrameRef o) {
		return getId().compareTo(o.getId());
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
			result = prime * result + ((getSense() == null) ? 0 : getSense().hashCode());
			result = prime * result + ((getFrame() == null) ? 0 : getFrame().hashCode());
			tmpHashCode = new Integer(result);
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
		final VerbNetFrameRef other = (VerbNetFrameRef) obj;
		if ((getId() != null) && getId().equals(other.getId())) {
			return true;
		}
		if (getSense() == null) {
			if (other.getSense() != null) {
				return false;
			}
		} else if (!getSense().equals(other.getSense())) {
			return false;
		}
		if (getFrame() == null) {
			if (other.getFrame() != null) {
				return false;
			}
		} else if (!getFrame().equals(other.getFrame())) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return getClass().getSimpleName() + "(" + getSense() + ":" + getVnClass() + "): "
				+ getFrame();
	}
}
