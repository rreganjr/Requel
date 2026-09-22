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
package com.rreganjr.nlp.dictionary.impl.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.ProjectDictionaryWord;

import org.fife.com.swabunga.spell.engine.SpellDictionaryASpell;

/**
 * The jazzy dictionary for one project's own words (issue #313).
 * <p>
 * {@link DatabaseSpellDictionary}'s sibling. Where that one is backed by the installation-wide
 * WordNet {@code word} table, this is backed by {@code project_dictionary_words} filtered to a
 * single project, and it is what a per-project {@code SpellChecker} carries as its <em>user
 * dictionary</em>. That placement is what makes a project word count as correctly spelled
 * <em>and</em> show up among the suggestions for a near-miss, which a plain "is this word in the
 * project's table" check could not do.
 * <p>
 * Constructed with a {@code null} phonetic file so the base class uses the {@code DoubleMeta}
 * transformator — the same one {@code DictionaryRepository.generatePhoneticCode} uses when a word
 * is stored, so the codes written and the codes looked up agree.
 *
 * @author ron
 */
public class ProjectSpellDictionary extends SpellDictionaryASpell {

	private final DictionaryRepository dictionaryRepository;
	private final Long projectId;

	/**
	 * @param dictionaryRepository -
	 *            the repository this dictionary reads and writes through.
	 * @param projectId -
	 *            the project whose words this dictionary sees. Never null; the installation-wide
	 *            dictionary is {@link DatabaseSpellDictionary}.
	 * @throws IOException
	 *             never, in practice: the base class only does IO when given a phonetic file, and
	 *             this passes none.
	 */
	public ProjectSpellDictionary(DictionaryRepository dictionaryRepository, Long projectId)
			throws IOException {
		super((File) null);
		this.dictionaryRepository = dictionaryRepository;
		this.projectId = projectId;
	}

	public Long getProjectId() {
		return projectId;
	}

	/**
	 * @param phoneticCode
	 *            The phonetic code common to the list of words
	 * @return the project's words having that phonetic code
	 */
	@Override
	protected List<String> getWords(String phoneticCode) {
		List<String> results = new ArrayList<String>();
		for (ProjectDictionaryWord word : dictionaryRepository.findProjectWordsByPhoneticCode(
				projectId, phoneticCode)) {
			results.add(word.getLemma());
		}
		return results;
	}

	/**
	 * Case-insensitive, unlike the base class (issue #313).
	 * <p>
	 * {@code SpellDictionaryASpell.isCorrect} compares the query against the candidate list
	 * exactly, then retries with the <em>query</em> lower-cased — it never lower-cases the stored
	 * entry. That suits the jazzy .dic files, whose entries are all lower-case, but not a project
	 * dictionary, which stores a word in the case the user entered it so that proper nouns are
	 * suggested the way they are written. Without this override, adding "Requel" would leave
	 * "requel" reading as a misspelling.
	 * <p>
	 * Comparing here rather than lower-casing on store keeps {@link #getWords(String)} returning
	 * one entry per word in its display case, so suggestions are not doubled up or flattened.
	 * The phonetic code is case-insensitive already (DoubleMeta upper-cases its input), so the
	 * candidate list is the same whatever case the query arrives in.
	 */
	@Override
	public boolean isCorrect(String word) {
		if (word == null) {
			return false;
		}
		for (String known : getWords(getCode(word))) {
			if (known.equalsIgnoreCase(word)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Reached when jazzy's {@code SpellChecker.addToDictionary} is called on a checker carrying
	 * this dictionary. It delegates to the repository rather than persisting here, so the
	 * idempotency check and the cached-checker eviction happen exactly once and in one place.
	 *
	 * @see SpellDictionaryASpell#addWord(java.lang.String)
	 */
	@Override
	public boolean addWord(String text) {
		dictionaryRepository.addToDictionary(projectId, text);
		return true;
	}
}
