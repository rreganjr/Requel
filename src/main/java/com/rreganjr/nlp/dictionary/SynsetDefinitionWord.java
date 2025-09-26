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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.hibernate.annotations.Index;

import com.rreganjr.nlp.ParseTag;
import com.rreganjr.nlp.dictionary.Word.IdAdapter;

/**
 * @author ron
 */
@Entity
@Table(name = "synset_definition_word")
@org.hibernate.annotations.Table(appliesTo = "synset_definition_word", indexes = {
		@Index(name = "index_sdw_word", columnNames = { "word_id" }),
		@Index(name = "index_sdw_synset", columnNames = { "sense_id" }) })
@XmlRootElement(name = "word")
public class SynsetDefinitionWord implements Comparable<SynsetDefinitionWord>, Serializable {
	static final long serialVersionUID = 0;

	private Long id;
	private Synset synset;
	private Integer index;
	private String text;
	private ParseTag parseTag;
	private Category properNounCategory;
	private Sense sense;

	/**
	 * Create a word that doesn't have a sense, such as text tagged with DT, IN,
	 * WDT, CC, ...
	 * 
	 * @param synset -
	 *            the synset this word is part of.
	 * @param index -
	 *            the index of the word (one based) in the synset.
	 * @param text -
	 *            the text of the word.
	 * @param parseTag -
	 *            the parse tag.
	 */
	public SynsetDefinitionWord(Synset synset, Integer index, String text, ParseTag parseTag) {
		setSynset(synset);
		setIndex(index);
		setText(text);
		setParseTag(parseTag);
	}

	/**
	 * Create a word with a sense.
	 * 
	 * @param synset -
	 *            the synset this word is part of.
	 * @param index -
	 *            the index of the word (one based) in the synset definition.
	 * @param text -
	 *            the text of the word.
	 * @param parseTag -
	 *            the parse tag.
	 * @param sense -
	 *            the wordnet Sense.
	 */
	public SynsetDefinitionWord(Synset synset, Integer index, String text, ParseTag parseTag,
			Sense sense) {
		setSynset(synset);
		setIndex(index);
		setText(text);
		setParseTag(parseTag);
		setSense(sense);
	}

	/**
	 * Create a word that is a named entity, for example
	 * City_Executive_Committee, City_of_Atlanta, or
	 * Superior_Court_Judge_Durwood_Pye
	 * 
	 * @param synset -
	 *            the synset this word is part of.
	 * @param index -
	 *            the index of the word (one based) in the synset definition.
	 * @param text -
	 *            the text of the word.
	 * @param parseTag -
	 *            the parse tag.
	 * @param sense -
	 *            the wordnet Sense.
	 * @param properNounCategory -
	 *            the wordnet category of an entity mapped from the pn value in
	 *            Semcor such as group, location or person.
	 */
	public SynsetDefinitionWord(Synset synset, Integer index, String text, ParseTag parseTag,
			Sense sense, Category properNounCategory) {
		setSynset(synset);
		setIndex(index);
		setText(text);
		setParseTag(parseTag);
		setProperNounCategory(properNounCategory);
		setSense(sense);
	}

	protected SynsetDefinitionWord() {
		// for hibernate
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

	@ManyToOne(targetEntity = Synset.class, cascade = CascadeType.ALL, optional = false)
	@JoinColumn(name = "synset_id")
	public Synset getSynset() {
		return synset;
	}

	public void setSynset(Synset sentence) {
		this.synset = sentence;
	}

	@Column(name = "word_index")
	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

	@Column(name = "text")
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Transient
	public boolean isIgnored() {
		return getSense() == null;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "parse_tag")
	public ParseTag getParseTag() {
		return parseTag;
	}

	public void setParseTag(ParseTag parseTag) {
		this.parseTag = parseTag;
	}

	@Transient
	public boolean isNamedEntity() {
		return properNounCategory != null;
	}

	@ManyToOne(targetEntity = Category.class, cascade = CascadeType.ALL, optional = true)
	@JoinColumn(name = "category_id")
	public Category getProperNounCategory() {
		return properNounCategory;
	}

	public void setProperNounCategory(Category properNounCategory) {
		this.properNounCategory = properNounCategory;
	}

	@ManyToOne(targetEntity = Sense.class, cascade = CascadeType.ALL, optional = true)
	@JoinColumns( { @JoinColumn(name = "sense_id"), @JoinColumn(name = "word_id") })
	public Sense getSense() {
		return sense;
	}

	public void setSense(Sense sense) {
		this.sense = sense;
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
				result = prime * result + getSynset().hashCode();
				result = prime * result + getIndex().hashCode();
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
		if (!(obj instanceof SynsetDefinitionWord)) {
			return false;
		}
		final SynsetDefinitionWord other = (SynsetDefinitionWord) obj;
		if ((getId() != null) && getId().equals(other.getId())) {
			return true;
		}

		if (getSynset() == null) {
			if (other.getSynset() != null) {
				return false;
			}
		} else if (!getSynset().equals(other.getSynset())) {
			return false;
		}
		if (getIndex() == null) {
			if (other.getIndex() != null) {
				return false;
			}
		} else if (!getIndex().equals(other.getIndex())) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(SynsetDefinitionWord o) {
		if (getSynset().equals(o.getSynset())) {
			return getIndex().compareTo(o.getIndex());
		}
		return getSynset().compareTo(o.getSynset());
	}

	@Override
	public String toString() {
		if (getSense() != null) {
			return getSense().toString();
		}
		return getText();
	}
}
