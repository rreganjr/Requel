/*
 * $Id: SemcorSentenceWord.java,v 1.2 2009/01/03 10:24:32 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.dictionary;

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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.hibernate.annotations.Index;

import edu.harvard.fas.rregan.nlp.ParseTag;
import edu.harvard.fas.rregan.nlp.dictionary.Word.IdAdapter;

/**
 * A word in a sentence of the Semcor Corpus.
 * 
 * @author ron
 */
@Entity
@Table(name = "semcor_sentence_word")
@org.hibernate.annotations.Table(appliesTo = "semcor_sentence_word", indexes = {
		@Index(name = "index_ssw_word", columnNames = { "word_id" }),
		@Index(name = "index_ssw_synset", columnNames = { "sense_id" }) })
@XmlRootElement(name = "word")
public class SemcorSentenceWord implements Comparable<SemcorSentenceWord>, Serializable {
	static final long serialVersionUID = 0;

	private Long id;

	private SemcorSentence sentence;
	private Integer index;
	private String text;
	private ParseTag parseTag;
	private Category properNounCategory;
	private Sense sense;

	/**
	 * Create a word that doesn't have a sense, such as text tagged with DT, IN,
	 * WDT, CC, ...
	 * 
	 * @param sentence -
	 *            the semcor sentence this word is part of.
	 * @param index -
	 *            the index of the word (zero based) in the semcor sentence.
	 * @param text -
	 *            the text of the word.
	 * @param parseTag -
	 *            the parse tag.
	 */
	public SemcorSentenceWord(SemcorSentence sentence, Integer index, String text, ParseTag parseTag) {
		setSentence(sentence);
		setIndex(index);
		setText(text);
		setParseTag(parseTag);
	}

	/**
	 * Create a word with a sense.
	 * 
	 * @param sentence -
	 *            the semcor sentence this word is part of.
	 * @param index -
	 *            the index of the word (zero based) in the semcor sentence.
	 * @param text -
	 *            the text of the word.
	 * @param parseTag -
	 *            the parse tag.
	 * @param sense -
	 *            the wordnet Sense.
	 */
	public SemcorSentenceWord(SemcorSentence sentence, Integer index, String text,
			ParseTag parseTag, Sense sense) {
		setSentence(sentence);
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
	 * @param sentence -
	 *            the semcor sentence this word is part of.
	 * @param index -
	 *            the index of the word (zero based) in the semcor sentence.
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
	public SemcorSentenceWord(SemcorSentence sentence, Integer index, String text,
			ParseTag parseTag, Sense sense, Category properNounCategory) {
		setSentence(sentence);
		setIndex(index);
		setText(text);
		setParseTag(parseTag);
		setProperNounCategory(properNounCategory);
		setSense(sense);
	}

	protected SemcorSentenceWord() {
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

	@ManyToOne(targetEntity = SemcorSentence.class, cascade = CascadeType.ALL, optional = false)
	@JoinColumn(name = "sentence_id")
	public SemcorSentence getSentence() {
		return sentence;
	}

	protected void setSentence(SemcorSentence sentence) {
		this.sentence = sentence;
	}

	@Column(name = "word_index")
	public Integer getIndex() {
		return index;
	}

	protected void setIndex(Integer index) {
		this.index = index;
	}

	@Column(name = "text")
	public String getText() {
		return text;
	}

	protected void setText(String text) {
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

	protected void setParseTag(ParseTag parseTag) {
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

	protected void setProperNounCategory(Category properNounCategory) {
		this.properNounCategory = properNounCategory;
	}

	@ManyToOne(targetEntity = Sense.class, cascade = CascadeType.ALL, optional = true)
	@JoinColumns( { @JoinColumn(name = "sense_id"), @JoinColumn(name = "word_id") })
	public Sense getSense() {
		return sense;
	}

	protected void setSense(Sense sense) {
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
				result = prime * result + getSentence().hashCode();
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
		if (!(obj instanceof SemcorSentenceWord)) {
			return false;
		}
		final SemcorSentenceWord other = (SemcorSentenceWord) obj;
		if ((getId() != null) && getId().equals(other.getId())) {
			return true;
		}

		if (getSentence() == null) {
			if (other.getSentence() != null) {
				return false;
			}
		} else if (!getSentence().equals(other.getSentence())) {
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
	public int compareTo(SemcorSentenceWord o) {
		if (getSentence().equals(o.getSentence())) {
			getIndex().compareTo(o.getIndex());
		}
		return getSentence().compareTo(o.getSentence());
	}

	@Override
	public String toString() {
		return getId() + ":" + getText();
	}
}
