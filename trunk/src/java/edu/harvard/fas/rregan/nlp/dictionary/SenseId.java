/*
 * $Id: SenseId.java,v 1.1 2008/12/13 00:40:36 rregan Exp $
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

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Wordnet Sense composite id based on a Word and a Synset.
 */
@Embeddable
public class SenseId implements Serializable {
	static final long serialVersionUID = 0;

	private Long synsetid;
	private Long wordid;

	public SenseId() {
	}

	public SenseId(Long synsetid, Long wordid) {
		this.synsetid = synsetid;
		this.wordid = wordid;
	}

	@Column(name = "synsetid", nullable = false)
	public Long getSynsetid() {
		return this.synsetid;
	}

	public void setSynsetid(Long synsetid) {
		this.synsetid = synsetid;
	}

	@Column(name = "wordid", nullable = false)
	public Long getWordid() {
		return this.wordid;
	}

	public void setWordid(Long wordid) {
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
		if (!(other instanceof SenseId)) {
			return false;
		}
		SenseId castOther = (SenseId) other;

		return (this.getSynsetid() == castOther.getSynsetid())
				&& (this.getWordid() == castOther.getWordid());
	}

	private Integer tmpHashCode = null;

	@Override
	public int hashCode() {
		if (tmpHashCode == null) {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((getSynsetid() == null) ? 0 : getSynsetid().hashCode());
			result = prime * result + ((getWordid() == null) ? 0 : getWordid().hashCode());
			tmpHashCode = new Integer(result);
		}
		return tmpHashCode.intValue();
	}
}
