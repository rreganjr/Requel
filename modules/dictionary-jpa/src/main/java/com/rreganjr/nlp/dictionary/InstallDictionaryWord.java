/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
 */
package com.rreganjr.nlp.dictionary;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;

/**
 * A word added to the installation-wide dictionary (issue #319): known in every project and with
 * no project.
 * <p>
 * Before #319 these were written into the WordNet {@link Word} table as rows with no senses, which
 * mixed user additions into the corpus: they could not be listed apart from it, and a re-import of
 * the WordNet dumps could collide with or drop them. {@code V20__install_dictionary_words.sql}
 * moved those rows here, and the WordNet table is read-only again.
 * <p>
 * {@code createdById} is a plain id, not an association, for the reason {@link ProjectDictionaryWord}
 * gives: {@code dictionary-jpa} cannot name {@code User}. Null for the rows V20 moved, whose adder
 * was never recorded.
 *
 * @author ron
 */
@Entity
@Table(name = "install_dictionary_words", uniqueConstraints = @UniqueConstraint(columnNames = "lemma"))
public class InstallDictionaryWord implements Comparable<InstallDictionaryWord>, Serializable {
	static final long serialVersionUID = 0;

	private Long id;
	private String lemma;
	private String phoneticCode;
	private Long createdById;
	private Date dateCreated;

	/**
	 * @param lemma -
	 *            the word, stored in the case it was entered.
	 * @param phoneticCode -
	 *            the jazzy DoubleMeta code for the lemma, used to find it as a spelling suggestion.
	 * @param createdById -
	 *            the id of the user who added it, or null if unknown.
	 */
	public InstallDictionaryWord(String lemma, String phoneticCode, Long createdById) {
		setLemma(lemma);
		setPhoneticCode(phoneticCode);
		setCreatedById(createdById);
		setDateCreated(new Date());
	}

	protected InstallDictionaryWord() {
	}

	@Id
	@Column(name = "id", unique = true, nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	@Column(name = "lemma", nullable = false, length = 80)
	public String getLemma() {
		return lemma;
	}

	protected void setLemma(String lemma) {
		this.lemma = lemma;
	}

	@Column(name = "phonetic_code", nullable = true, length = 80)
	public String getPhoneticCode() {
		return phoneticCode;
	}

	protected void setPhoneticCode(String phoneticCode) {
		this.phoneticCode = phoneticCode;
	}

	@Column(name = "created_by_id", nullable = true)
	public Long getCreatedById() {
		return createdById;
	}

	protected void setCreatedById(Long createdById) {
		this.createdById = createdById;
	}

	@Column(name = "date_created", nullable = true)
	@Temporal(TemporalType.TIMESTAMP)
	public Date getDateCreated() {
		return dateCreated;
	}

	protected void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	/** Ordered by lemma, case-insensitively, the comparison the repository uses for duplicates. */
	@Override
	public int compareTo(InstallDictionaryWord other) {
		if (other == null) {
			return 1;
		}
		String thisLemma = getLemma() == null ? "" : getLemma();
		String otherLemma = other.getLemma() == null ? "" : other.getLemma();
		return thisLemma.compareToIgnoreCase(otherLemma);
	}

	@Override
	public String toString() {
		return "InstallDictionaryWord[lemma=" + getLemma() + "]";
	}
}
