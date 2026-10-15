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
 *
 */
package com.rreganjr.nlp.dictionary;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

/**
 * A word a user added to one project's dictionary (issue #313).
 * <p>
 * This is the third dictionary layer. The other two — the jazzy word lists on the classpath and
 * the WordNet {@link Word} table loaded from the SQL dumps — are installation-wide and read-only.
 * A row here makes its lemma spelled-correctly in exactly one project, and is the only thing "Add
 * to Dictionary" writes.
 * <p>
 * The entity deliberately holds a bare {@code projectId} rather than an association to
 * {@code Project}: {@code dictionary-jpa} depends on {@code platform-core} and nothing else of
 * ours, and mapping the association would invert the module graph. The project's side of the link
 * is a unidirectional, read-only {@code @OneToMany} + {@code @JoinColumn} on
 * {@code AbstractProjectOrDomain}, the same shape {@link Word#getSenses()} already uses. This side
 * owns the column for writes.
 * <p>
 * Ids are {@code IDENTITY}, not the assigned-or-generated strategy {@link Word} uses. That one
 * exists so a dictionary import can preserve dump-assigned ids across the composite sense -&gt;
 * word foreign keys; project words have no such ids to preserve and are never imported by id.
 *
 * @author ron
 */
@Entity
@Table(name = "project_dictionary_words", uniqueConstraints = @UniqueConstraint(columnNames = {
		"project_id", "lemma" }))
@XmlRootElement(name = "dictionaryWord", namespace = "http://www.rreganjr.com/requel")
@XmlType(name = "dictionaryWord", namespace = "http://www.rreganjr.com/requel")
public class ProjectDictionaryWord implements Comparable<ProjectDictionaryWord>, Serializable {
	static final long serialVersionUID = 0;

	private Long id;
	private Long projectId;
	private String lemma;
	private String phoneticCode;

	/**
	 * @param projectId -
	 *            the id of the project this word belongs to.
	 * @param lemma -
	 *            the word, stored in the case the user entered it.
	 * @param phoneticCode -
	 *            the jazzy DoubleMeta code for the lemma, used to find it as a spelling
	 *            suggestion. See {@code DictionaryRepository.generatePhoneticCode}.
	 */
	public ProjectDictionaryWord(Long projectId, String lemma, String phoneticCode) {
		setProjectId(projectId);
		setLemma(lemma);
		setPhoneticCode(phoneticCode);
	}

	protected ProjectDictionaryWord() {
	}

	@Id
	@Column(name = "id", unique = true, nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@XmlTransient
	public Long getId() {
		return id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	/**
	 * The owning project's id. Not an association: see the class comment.
	 * <p>
	 * {@code @XmlTransient} because the project element that wraps these words on export already
	 * says which project they belong to, and an id from another installation would be meaningless
	 * on import.
	 */
	@Column(name = "project_id", nullable = false)
	@XmlTransient
	public Long getProjectId() {
		return projectId;
	}

	protected void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	@Column(name = "lemma", nullable = false, length = 80)
	@XmlAttribute(name = "lemma")
	public String getLemma() {
		return lemma;
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	@Column(name = "phonetic_code", nullable = true, length = 80)
	@XmlAttribute(name = "phoneticCode")
	public String getPhoneticCode() {
		return phoneticCode;
	}

	public void setPhoneticCode(String phoneticCode) {
		this.phoneticCode = phoneticCode;
	}

	/**
	 * Ordered by project then lemma, case-insensitively — the same comparison the repository uses
	 * to decide whether a word is already in a project's dictionary.
	 */
	@Override
	public int compareTo(ProjectDictionaryWord other) {
		if (other == null) {
			return 1;
		}
		if (getProjectId() != null && other.getProjectId() != null
				&& !getProjectId().equals(other.getProjectId())) {
			return getProjectId().compareTo(other.getProjectId());
		}
		String thisLemma = getLemma() == null ? "" : getLemma();
		String otherLemma = other.getLemma() == null ? "" : other.getLemma();
		return thisLemma.compareToIgnoreCase(otherLemma);
	}

	@Override
	public String toString() {
		return "ProjectDictionaryWord[project=" + getProjectId() + ", lemma=" + getLemma() + "]";
	}
}
