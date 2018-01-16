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
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortNatural;
import org.hibernate.annotations.SortType;

import com.rreganjr.nlp.PartOfSpeech;

/**
 * Wordnet Word
 */
@Entity
@Table(name = "word", uniqueConstraints = @UniqueConstraint(columnNames = "lemma"))
@XmlRootElement(name = "word")
public class Word implements Comparable<Word>, Serializable {
	static final long serialVersionUID = 0;

	private Long id;
	private String lemma;
	private String phoneticCode; // for Jazzy spell checker
	private Set<Sense> senses = new TreeSet<Sense>();

	/**
	 * Create a new word with the supplied text
	 * 
	 * @param lemma -
	 *            the text of the word
	 * @param phoneticCode -
	 */
	public Word(String lemma, String phoneticCode) {
		setLemma(lemma);
		setPhoneticCode(phoneticCode);
	}

	protected Word() {
	}

	@Id
	@Column(name = "wordid", unique = true, nullable = false)
	@GeneratedValue(strategy = GenerationType.AUTO)
	@XmlID
	@XmlAttribute(name = "id")
	@XmlJavaTypeAdapter(IdAdapter.class)
	public Long getId() {
		return this.id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	@Column(name = "lemma", unique = true, nullable = false, length = 80)
	@XmlAttribute(name = "lemma")
	public String getLemma() {
		return this.lemma;
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	@Column(name = "phonetic_code", unique = false, nullable = true, length = 80)
	@XmlAttribute(name = "phoneticCode")
	public String getPhoneticCode() {
		return this.phoneticCode;
	}

	public void setPhoneticCode(String phoneticCode) {
		this.phoneticCode = phoneticCode;
	}

	@OneToMany(targetEntity = Sense.class, cascade = { CascadeType.ALL }, fetch = FetchType.LAZY)
	@JoinColumn(name = "wordid")
	@SortNatural
	@XmlElementWrapper(name = "senses")
	@XmlElementRef(name = "sense")
	public Set<Sense> getSenses() {
		return senses;
	}

	public void setSenses(Set<Sense> senses) {
		this.senses = senses;
	}

	/**
	 * Get the sense for a particular part of speech.
	 * 
	 * @param pos
	 * @return
	 */
	@Transient
	@XmlTransient
	public Set<Sense> getSenses(PartOfSpeech pos) {
		Set<Sense> senses = new HashSet<Sense>(getSenses().size());
		for (Sense sense : getSenses()) {
			if (sense.getSynset().isPartOfSpeech(pos)) {
				senses.add(sense);
			}
		}
		return senses;
	}

	/**
	 * @param pos
	 * @param rank
	 * @return A specific sense by part of speech and rank
	 */
	@Transient
	@XmlTransient
	public Sense getSense(PartOfSpeech pos, Integer rank) {
		for (Sense sense : getSenses(pos)) {
			if (rank.equals(sense.getRank())) {
				return sense;
			}
		}
		return null;
	}

	@Override
	public int compareTo(Word o) {
		return getLemma().compareTo(o.getLemma());
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
			result = prime * result + ((getLemma() == null) ? 0 : getLemma().hashCode());
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
		final Word other = (Word) obj;
		if ((getId() != null) && getId().equals(other.getId())) {
			return true;
		}
		if (getLemma() == null) {
			if (other.getLemma() != null) {
				return false;
			}
		} else if (!getLemma().equals(other.getLemma())) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return lemma;
	}

	/**
	 * This class is used by JAXB to convert the id of an entity into an xml id
	 * string that will be distinct from other entity xml id strings by the use
	 * of a prefix.
	 * 
	 * @author ron
	 */
	public static class IdAdapter extends XmlAdapter<String, Long> {
		private static final String prefix = "WORD_";

		@Override
		public Long unmarshal(String id) throws Exception {
			return new Long(id.substring(prefix.length()));
		}

		@Override
		public String marshal(Long id) throws Exception {
			if (id != null) {
				return prefix + id.toString();
			}
			return "";
		}
	}
}
