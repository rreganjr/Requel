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
import com.rreganjr.nlp.dictionary.InstallDictionaryWord;

import org.fife.com.swabunga.spell.engine.SpellDictionaryASpell;

/**
 * The jazzy dictionary for installation-wide added words (issue #319), backed by
 * {@code install_dictionary_words}.
 * <p>
 * One shared instance is in every checker: it is the installation checker's <em>user
 * dictionary</em>, so a project-less {@code addToDictionary(String)} writes here, and it is in
 * each per-project checker's read-only dictionary list. It reads per lookup, like
 * {@link DatabaseSpellDictionary}, so an admin add or remove takes effect everywhere at once with
 * no cached checker to evict.
 * <p>
 * {@link #isCorrect(String)} compares case-insensitively, as {@link ProjectSpellDictionary} does
 * and for the same reason: words are stored in the case they were entered, and the base class
 * only lower-cases the query.
 *
 * @author ron
 */
public class InstallSpellDictionary extends SpellDictionaryASpell {

	private final DictionaryRepository dictionaryRepository;

	/**
	 * @param dictionaryRepository -
	 *            the repository this dictionary reads and writes through.
	 * @throws IOException
	 *             never: a null phonetic file selects the DoubleMeta transformator, the one
	 *             {@code DictionaryRepository.generatePhoneticCode} uses when a word is stored.
	 */
	public InstallSpellDictionary(DictionaryRepository dictionaryRepository) throws IOException {
		super((File) null);
		this.dictionaryRepository = dictionaryRepository;
	}

	@Override
	protected List<String> getWords(String phoneticCode) {
		List<String> results = new ArrayList<String>();
		for (InstallDictionaryWord word : dictionaryRepository
				.findInstallWordsByPhoneticCode(phoneticCode)) {
			results.add(word.getLemma());
		}
		return results;
	}

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

	@Override
	public boolean addWord(String text) {
		dictionaryRepository.addInstallWord(text, null);
		return true;
	}
}
