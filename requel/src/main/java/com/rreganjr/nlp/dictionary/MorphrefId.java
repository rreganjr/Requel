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

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * @author ron
 */
@Embeddable
public class MorphrefId implements Serializable {
	static final long serialVersionUID = 0;

	private int morphid;
	private String pos;
	private int wordid;

	public MorphrefId() {
	}

	public MorphrefId(int morphid, String pos, int wordid) {
		this.morphid = morphid;
		this.pos = pos;
		this.wordid = wordid;
	}

	@Column(name = "morphid", nullable = false)
	public int getMorphid() {
		return this.morphid;
	}

	public void setMorphid(int morphid) {
		this.morphid = morphid;
	}

	@Column(name = "pos", nullable = false)
	public String getPos() {
		return this.pos;
	}

	public void setPos(String pos) {
		this.pos = pos;
	}

	@Column(name = "wordid", nullable = false)
	public int getWordid() {
		return this.wordid;
	}

	public void setWordid(int wordid) {
		this.wordid = wordid;
	}

	@Override
	public boolean equals(Object other) {
		if ((this == other)) {
			return true;
		}
		if ((other == null)) {
			return false;
		}
		if (!(other instanceof MorphrefId)) {
			return false;
		}
		MorphrefId castOther = (MorphrefId) other;

		return (this.getMorphid() == castOther.getMorphid())
				&& ((this.getPos() == castOther.getPos()) || ((this.getPos() != null)
						&& (castOther.getPos() != null) && this.getPos().equals(castOther.getPos())))
				&& (this.getWordid() == castOther.getWordid());
	}

	@Override
	public int hashCode() {
		int result = 17;

		result = 37 * result + this.getMorphid();
		result = 37 * result + (getPos() == null ? 0 : this.getPos().hashCode());
		result = 37 * result + this.getWordid();
		return result;
	}

}
