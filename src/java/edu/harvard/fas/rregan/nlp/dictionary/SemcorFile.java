/*
 * $Id$
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
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.hibernate.annotations.IndexColumn;

import edu.harvard.fas.rregan.nlp.dictionary.Word.IdAdapter;

/**
 * @author ron
 */
@Entity
@Table(name = "semcor_file")
@XmlRootElement(name = "semcorFile")
public class SemcorFile implements Comparable<SemcorFile>, Serializable {
	static final long serialVersionUID = 0;

	private Long id;

	// the Semcor folder: brown1, brown2, or brownv
	private String corpusSectionFolder;
	// the Semcor tagfile filename in the tagfiles folder: br-a01, br-j17, ...
	private String corpusFileName;
	private List<SemcorSentence> sentences = new ArrayList<SemcorSentence>();

	/**
	 * @param corpusSectionFolder
	 * @param corpusFileName
	 */
	public SemcorFile(String corpusSectionFolder, String corpusFileName) {
		setCorpusSectionFolder(corpusSectionFolder);
		setCorpusFileName(corpusFileName);
	}

	protected SemcorFile() {
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

	@Column(name = "section")
	public String getCorpusSectionFolder() {
		return corpusSectionFolder;
	}

	protected void setCorpusSectionFolder(String corpusSectionFolder) {
		this.corpusSectionFolder = corpusSectionFolder;
	}

	@Column(name = "file")
	public String getCorpusFileName() {
		return corpusFileName;
	}

	protected void setCorpusFileName(String corpusFileName) {
		this.corpusFileName = corpusFileName;
	}

	@OneToMany(mappedBy = "file", targetEntity = SemcorSentence.class, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@IndexColumn(name = "snum", base = 1)
	public List<SemcorSentence> getSentences() {
		return sentences;
	}

	protected void setSentences(List<SemcorSentence> sentences) {
		this.sentences = sentences;
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
				result = prime * result + getCorpusSectionFolder().hashCode();
				result = prime * result + getCorpusFileName().hashCode();
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
		if (!(obj instanceof SemcorFile)) {
			return false;
		}
		final SemcorFile other = (SemcorFile) obj;
		if ((getId() != null) && getId().equals(other.getId())) {
			return true;
		}

		if (getCorpusSectionFolder() == null) {
			if (other.getCorpusSectionFolder() != null) {
				return false;
			}
		} else if (!getCorpusSectionFolder().equals(other.getCorpusSectionFolder())) {
			return false;
		}
		if (getCorpusFileName() == null) {
			if (other.getCorpusFileName() != null) {
				return false;
			}
		} else if (!getCorpusFileName().equals(other.getCorpusFileName())) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(SemcorFile o) {
		if (!getCorpusSectionFolder().equals(o.getCorpusSectionFolder())) {
			return getCorpusSectionFolder().compareTo(o.getCorpusSectionFolder());
		}
		return getCorpusFileName().compareTo(o.getCorpusFileName());
	}
}
