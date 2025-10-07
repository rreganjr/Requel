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
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

/**
 * @author ron
 */
@Entity
@Table(name = "lexlinkref")
@XmlRootElement(name = "lexlink")
public class Lexlinkref implements Comparable<Lexlinkref>, Serializable {
	static final long serialVersionUID = 0;

	private LexlinkrefId id;
	private Synset fromSynset;
	private Word fromWord;
	private Synset toSynset;
	private Word toWord;
	private Linkdef linkType;

	public Lexlinkref() {
	}

	public Lexlinkref(LexlinkrefId id) {
		this.id = id;
	}

	@EmbeddedId
	@AttributeOverrides( {
			@AttributeOverride(name = "word1id", column = @Column(name = "word1id", nullable = false, precision = 6, scale = 0)),
			@AttributeOverride(name = "synset1id", column = @Column(name = "synset1id", nullable = false, precision = 9, scale = 0)),
			@AttributeOverride(name = "word2id", column = @Column(name = "word2id", nullable = false, precision = 6, scale = 0)),
			@AttributeOverride(name = "synset2id", column = @Column(name = "synset2id", nullable = false, precision = 9, scale = 0)),
			@AttributeOverride(name = "linkid", column = @Column(name = "linkid", nullable = false, precision = 2, scale = 0)) })
	public LexlinkrefId getId() {
		return this.id;
	}

	public void setId(LexlinkrefId id) {
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

	@ManyToOne(targetEntity = Word.class, cascade = CascadeType.ALL, optional = false)
	@JoinColumn(name = "word1id", insertable = false, updatable = false)
	@XmlTransient
	public Word getFromWord() {
		return fromWord;
	}

	public void setFromWord(Word fromWord) {
		this.fromWord = fromWord;
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

	@ManyToOne(targetEntity = Word.class, cascade = CascadeType.ALL, optional = false)
	@JoinColumn(name = "word2id", insertable = false, updatable = false)
	@XmlTransient
	public Word getToWord() {
		return toWord;
	}

	public void setToWord(Word toWord) {
		this.toWord = toWord;
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
		final Lexlinkref other = (Lexlinkref) obj;

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

		if (getFromWord() == null) {
			if (other.getFromWord() != null) {
				return false;
			}
		} else if (!getFromWord().equals(other.getFromWord())) {
			return false;
		}

		if (getToSynset() == null) {
			if (other.getToSynset() != null) {
				return false;
			}
		} else if (!getToSynset().equals(other.getToSynset())) {
			return false;
		}

		if (getToWord() == null) {
			if (other.getToWord() != null) {
				return false;
			}
		} else if (!getToWord().equals(other.getToWord())) {
			return false;
		}

		if (getLinkType() == null) {
			if (other.getLinkType() != null) {
				return false;
			}
		} else if (!getLinkType().equals(other.getLinkType())) {
			return false;
		}

		return true;
	}

	@Override
	public int compareTo(Lexlinkref o) {
		return getId().compareTo(o.getId());
	}
}
