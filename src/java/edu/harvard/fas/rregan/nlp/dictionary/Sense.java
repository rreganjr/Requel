/*
 * $Id: Sense.java,v 1.7 2009/02/08 13:25:14 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.dictionary;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;

import edu.harvard.fas.rregan.nlp.dictionary.impl.command.ImportDictionaryCommandImpl;

/**
 * Wordnet Word Sense
 */
@Entity
@Table(name = "sense")
@XmlRootElement(name = "sense")
public class Sense implements Comparable<Sense>, Serializable {
	static final long serialVersionUID = 0;

	private SenseId id;
	private Integer rank;
	private Word word;
	private Synset synset;
	private Integer sampleFrequency;
	private String senseKey;
	private Set<VerbNetFrameRef> frameRefs = new TreeSet<VerbNetFrameRef>();

	/**
	 * @param word
	 * @param synset
	 */
	public Sense(Word word, Synset synset) {
		setId(new SenseId(synset.getId(), word.getId()));
		setWord(word);
		setSynset(synset);
	}

	protected Sense() {
	}

	@EmbeddedId
	@XmlTransient
	// this must be public for a query.
	public SenseId getId() {
		return this.id;
	}

	protected void setId(SenseId id) {
		this.id = id;
	}

	@ManyToOne(targetEntity = Word.class, cascade = CascadeType.ALL, optional = false)
	@JoinColumn(name = "wordid", insertable = false, updatable = false)
	@XmlTransient
	public Word getWord() {
		return word;
	}

	protected void setWord(Word word) {
		this.word = word;
	}

	@ManyToOne(targetEntity = Synset.class, cascade = CascadeType.ALL, optional = false)
	@JoinColumn(name = "synsetid", insertable = false, updatable = false)
	@XmlIDREF
	@XmlAttribute
	public Synset getSynset() {
		return synset;
	}

	protected void setSynset(Synset synset) {
		this.synset = synset;
	}

	@OneToMany(targetEntity = VerbNetFrameRef.class, cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, mappedBy = "sense")
	@Sort(type = SortType.NATURAL)
	@XmlTransient
	public Set<VerbNetFrameRef> getVerbNetFrameRefs() {
		return frameRefs;
	}

	protected void setVerbNetFrameRefs(Set<VerbNetFrameRef> frameRefs) {
		this.frameRefs = frameRefs;
	}

	/**
	 * @return The VerbNet frames applicable to this sense for supported verbs,
	 *         or an empty set.
	 */
	@Transient
	@XmlTransient
	public Set<VerbNetFrame> getVerbNetFrames() {
		Set<VerbNetFrame> frames = new HashSet<VerbNetFrame>(getVerbNetFrameRefs().size());
		for (VerbNetFrameRef ref : getVerbNetFrameRefs()) {
			frames.add(ref.getFrame());
		}
		return frames;
	}

	@Column(name = "rank", nullable = false)
	@XmlAttribute(name = "rank")
	public Integer getRank() {
		return this.rank;
	}

	public void setRank(Integer rank) {
		this.rank = rank;
	}

	@Column(name = "freq", nullable = true)
	@XmlAttribute(name = "frequency")
	public Integer getSampleFrequency() {
		return sampleFrequency;
	}

	public void setSampleFrequency(Integer sampleFrequency) {
		this.sampleFrequency = sampleFrequency;
	}

	@Column(name = "sense_key", nullable = true)
	@XmlAttribute(name = "senseKey")
	public String getSenseKey() {
		return senseKey;
	}

	public void setSenseKey(String senseKey) {
		this.senseKey = senseKey;
	}

	@Override
	public int compareTo(Sense o) {
		if (getWord().equals(o.getWord())) {
			return (getSynset().getPos().compareTo(o.getSynset().getPos()) * 100 + getRank()
					.compareTo(o.getRank()));
		}
		return getWord().compareTo(o.getWord());
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
			result = prime * result + ((getRank() == null) ? 0 : getRank().hashCode());
			result = prime * result + ((getSynset() == null) ? 0 : getSynset().hashCode());
			result = prime * result + ((getWord() == null) ? 0 : getWord().hashCode());
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
		final Sense other = (Sense) obj;
		if ((getId() != null) && getId().equals(other.getId())) {
			return true;
		}
		if (getWord() == null) {
			if (other.getWord() != null) {
				return false;
			}
		} else if (!getWord().equals(other.getWord())) {
			return false;
		}
		if (getSynset() == null) {
			if (other.getSynset() != null) {
				return false;
			}
		} else if (!getSynset().equals(other.getSynset())) {
			return false;
		}
		if (getRank() == null) {
			if (other.getRank() != null) {
				return false;
			}
		} else if (!getRank().equals(other.getRank())) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return getWord().getLemma() + "#" + getSynset().getPos() + "#" + getRank();
	}

	/**
	 * This is for JAXB to patchup the parent/child relationship and swap the
	 * creator with an existing user.
	 * 
	 * @param parent
	 * @see ImportDictionaryCommandImpl.UnmarshallerListener
	 */
	public void afterUnmarshal(Object parent) {
		setWord((Word) parent);
		setId(new SenseId(getSynset().getId(), getWord().getId()));
	}
}
