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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import jakarta.persistence.*;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlIDREF;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.SortNatural;

import com.rreganjr.nlp.PartOfSpeech;

/**
 * Wordnet Synset (synonym set)
 */
@Entity
@Table(name = "synset")
@XmlRootElement(name = "synset")
public class Synset implements Comparable<Synset>, Serializable {
	static final long serialVersionUID = 0;

	private Long id;
	private String pos;
	private Category category;
	private String definition;
	private Map<Linkdef, Integer> subsumerCounts = new HashMap<Linkdef, Integer>();
	private Set<Sense> senses = new TreeSet<Sense>();
	private Set<Semlinkref> semlinks = new TreeSet<Semlinkref>();
	private List<SynsetDefinitionWord> words = new ArrayList<SynsetDefinitionWord>();

	// private List<SynsetDefinitionWordEntry> parsedDefinitionWordEntries;

	protected Synset() {
	}

	@Id
	@Column(name = "synsetid", unique = true, nullable = false)
	@XmlID
	@XmlAttribute(name = "id")
	@XmlJavaTypeAdapter(IdAdapter.class)
	public Long getId() {
		return this.id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	@Transient
	public PartOfSpeech getPartOfSpeech() {
		return PartOfSpeech.fromWordNetPOS(getPos());
	}

	public boolean isPartOfSpeech(PartOfSpeech pos) {
		return getPartOfSpeech().equals(pos);
	}

	@Column(name = "pos", length = 2)
	@XmlAttribute(name = "pos")
	public String getPos() {
		return this.pos;
	}

	public void setPos(String pos) {
		this.pos = pos;
	}

	@ManyToOne(targetEntity = Category.class, cascade = CascadeType.ALL, optional = false)
	@JoinColumn(name = "categoryid")
	@XmlIDREF
	@XmlAttribute(name = "category")
	public Category getCategory() {
		return this.category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	@Column(name = "definition", length = 1000)
	@XmlElement(name = "definition")
	public String getDefinition() {
		return this.definition;
	}

	public void setDefinition(String definition) {
		this.definition = definition;
	}

	@Transient
	public Integer getSubsumerCount(Linkdef linkType) {
		if (getSubsumerCounts().containsKey(linkType)) {
			return getSubsumerCounts().get(linkType);
		}
		return 0;
	}

	public void setSubsumerCount(Linkdef linkType, Integer count) {
		getSubsumerCounts().put(linkType, count);
	}

	/**
	 * @return a map of the relation type to count of all synsets subsumed by
	 *         this synset in that particular relation.
	 */
	@ElementCollection(targetClass = Integer.class, fetch = FetchType.LAZY)
	@CollectionTable(name = "synset_subsumer_counts", joinColumns = { @JoinColumn(name = "synsetid") })
	@Column(name = "element")
	@MapKeyJoinColumn(name = "mapkey_linkid", referencedColumnName = "linkid")
	public Map<Linkdef, Integer> getSubsumerCounts() {
		return subsumerCounts;
	}

	public void setSubsumerCounts(Map<Linkdef, Integer> subsumerCounts) {
		this.subsumerCounts = subsumerCounts;
	}

	@OneToMany(targetEntity = Sense.class, cascade = { CascadeType.ALL }, fetch = FetchType.LAZY)
	@JoinColumn(name = "synsetid")
	@SortNatural
	@XmlElementWrapper(name = "senses")
	@XmlElementRef(name = "sense")
	public Set<Sense> getSenses() {
		return senses;
	}

	public void setSenses(Set<Sense> senses) {
		this.senses = senses;
	}

	@OneToMany(targetEntity = Semlinkref.class, cascade = { CascadeType.ALL }, fetch = FetchType.LAZY)
	@JoinColumn(name = "synset1id", referencedColumnName = "synsetid")
	@SortNatural
	@XmlElementWrapper(name = "semlinks")
	@XmlElementRef(name = "semlink")
	public Set<Semlinkref> getSemlinks() {
		return semlinks;
	}

	public void setSemlinks(Set<Semlinkref> semlinks) {
		this.semlinks = semlinks;
	}

	@Transient
	@XmlTransient
	public Set<Synset> getHyponyms() {
		Set<Synset> hyponyms = new HashSet<Synset>();
		for (Semlinkref semlinkref : getSemlinks()) {
			if ("hyponym".equals(semlinkref.getLinkType().getName())) {
				hyponyms.add(semlinkref.getToSynset());
			}
		}
		return hyponyms;
	}

	@Transient
	@XmlTransient
	public Set<Synset> getHypernyms() {
		Set<Synset> hypernyms = new HashSet<Synset>();
		for (Semlinkref semlinkref : getSemlinks()) {
			if ("hypernym".equals(semlinkref.getLinkType().getName())) {
				hypernyms.add(semlinkref.getToSynset());
			}
		}
		return hypernyms;
	}

	/**
	 * @return The list of words in the sentence.
	 */
	@OneToMany(mappedBy = "synset", targetEntity = SynsetDefinitionWord.class, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@IndexColumn(name = "word_index", base = 0)
	public List<SynsetDefinitionWord> getWords() {
		return words;
	}

	protected void setWords(List<SynsetDefinitionWord> words) {
		this.words = words;
	}

	/*
	 * @OneToMany(targetEntity = SynsetDefinitionWordEntry.class, cascade = {
	 * CascadeType.ALL }, fetch = FetchType.LAZY) @JoinColumn(name =
	 * "synset_defword_entry_id") @SortNatural
	 * @XmlElementWrapper(name = "definitionWordEntries") @XmlElementRef(name =
	 * "definitionWordEntry") public List<SynsetDefinitionWordEntry>
	 * getParsedDefinitionWordEntries() { return parsedDefinitionWordEntries; }
	 * public void setParsedDefinitionWordEntries( List<SynsetDefinitionWordEntry>
	 * parsedDefinitionWordEntries) { this.parsedDefinitionWordEntries =
	 * parsedDefinitionWordEntries; }
	 */
	@Override
	public int compareTo(Synset o) {
		return getId().compareTo(o.getId());
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
			result = prime * result + ((getCategory() == null) ? 0 : getCategory().hashCode());
			result = prime * result + ((getDefinition() == null) ? 0 : getDefinition().hashCode());
			result = prime * result + ((getPos() == null) ? 0 : getPos().hashCode());
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
		if (!getClass().isAssignableFrom(obj.getClass())) {
			return false;
		}
		final Synset other = (Synset) obj;
		if ((getId() != null) && getId().equals(other.getId())) {
			return true;
		}
		if (getDefinition() == null) {
			if (other.getDefinition() != null) {
				return false;
			}
		} else if (!getDefinition().equals(other.getDefinition())) {
			return false;
		}
		if (getCategory() == null) {
			if (other.getCategory() != null) {
				return false;
			}
		} else if (!getCategory().equals(other.getCategory())) {
			return false;
		}
		if (getPos() == null) {
			if (other.getPos() != null) {
				return false;
			}
		} else if (!getPos().equals(other.getPos())) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return getPartOfSpeech() + "[" + getId() + "]: " + getDefinition();
	}

	/**
	 * This class is used by JAXB to convert the id of an entity into an xml id
	 * string that will be distinct from other entity xml id strings by the use
	 * of a prefix.
	 * 
	 * @author ron
	 */
	public static class IdAdapter extends XmlAdapter<String, Long> {
		private static final String prefix = "SYNSET_";

		@Override
		public Long unmarshal(String id) throws Exception {
			return Long.valueOf(id.substring(prefix.length()));
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
