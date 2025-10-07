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
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.hibernate.annotations.IndexColumn;

import com.rreganjr.nlp.dictionary.Word.IdAdapter;

/**
 * A sentence of the Semcor Corpus.
 * 
 * @author ron
 */
@Entity
@Table(name = "semcor_sentence")
@XmlRootElement(name = "semcorSentence")
public class SemcorSentence implements Comparable<SemcorSentence>, Serializable {
	static final long serialVersionUID = 0;

	private Long id;

	private SemcorFile file;
	// the snum from the s element wrapping the sentince in the file.
	private Long sentenceIndex;

	private List<SemcorSentenceWord> words = new ArrayList<SemcorSentenceWord>();

	/**
	 * Create a new sentence. This is used by the semcor db loader.
	 * 
	 * @param corpusSectionFolder -
	 *            the Semcor folder: brown1, brown2, or brownv
	 * @param corpusFileName -
	 *            the Semcor tagfile filename in the tagfiles folder under the
	 *            section folder. for example: br-a01, br-j17, ...
	 * @param sentenceIndex -
	 *            the sentence index from Semcor based on the value of snum
	 */
	public SemcorSentence(SemcorFile file, Long sentenceIndex) {
		setFile(file);
		setSentenceIndex(sentenceIndex);
	}

	protected SemcorSentence() {
	}

	@Id
	@Column(name = "id", unique = true, nullable = false)
	@GeneratedValue(strategy = GenerationType.AUTO)
	@XmlID
	@XmlAttribute(name = "id")
	@XmlJavaTypeAdapter(IdAdapter.class)
	protected Long getId() {
		return this.id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(targetEntity = SemcorFile.class, cascade = CascadeType.ALL, optional = false)
	@JoinColumn(name = "file_id")
	public SemcorFile getFile() {
		return file;
	}

	protected void setFile(SemcorFile file) {
		this.file = file;
	}

	@Column(name = "snum")
	public Long getSentenceIndex() {
		return sentenceIndex;
	}

	protected void setSentenceIndex(Long sentenceIndex) {
		this.sentenceIndex = sentenceIndex;
	}

	/**
	 * @return The list of words in the sentence.
	 */
	@OneToMany(mappedBy = "sentence", targetEntity = SemcorSentenceWord.class, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@IndexColumn(name = "word_index", base = 1)
	public List<SemcorSentenceWord> getWords() {
		return words;
	}

	protected void setWords(List<SemcorSentenceWord> words) {
		this.words = words;
	}

	private Integer tmpHashCode = null;

	@Override
	public int hashCode() {
		if (tmpHashCode == null) {
			if (getId() != null) {
				tmpHashCode = getId().hashCode();
			} else {
				final int prime = 31;
				int result = 1;
				result = prime * result + getFile().hashCode();
				result = prime * result + getSentenceIndex().hashCode();
				tmpHashCode = result;
			}
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
		// NOTE: getClass().equals(obj.getClass()) fails when the obj is a proxy
		if (!(obj instanceof SemcorSentence)) {
			return false;
		}
		final SemcorSentence other = (SemcorSentence) obj;
		if ((getId() != null) && getId().equals(other.getId())) {
			return true;
		}

		if (getFile() == null) {
			if (other.getFile() != null) {
				return false;
			}
		} else if (!getFile().equals(other.getFile())) {
			return false;
		}
		if (getSentenceIndex() == null) {
			if (other.getSentenceIndex() != null) {
				return false;
			}
		} else if (!getSentenceIndex().equals(other.getSentenceIndex())) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(SemcorSentence o) {
		if (!getFile().equals(o.getFile())) {
			return getFile().compareTo(o.getFile());
		}
		return getSentenceIndex().compareTo(o.getSentenceIndex());
	}

	@Override
	public String toString() {
		return "" + getWords();
	}
}
