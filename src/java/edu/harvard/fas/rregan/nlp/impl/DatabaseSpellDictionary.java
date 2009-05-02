/*
 * $Id: DatabaseSpellDictionary.java,v 1.5 2009/01/24 11:08:38 rregan Exp $
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
package edu.harvard.fas.rregan.nlp.impl;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import com.swabunga.spell.engine.SpellDictionaryASpell;

import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;

/**
 * An implementation of Jazzy's SpellDictionary using The database based
 * DictionaryRepository of WordNet words.
 * <p>
 * 
 * @author ron
 */
public class DatabaseSpellDictionary extends SpellDictionaryASpell {

	private final DictionaryRepository dictionaryRepository;

	/**
	 * @param dictionaryRepository
	 * @param phonetic
	 * @param encoding
	 * @throws IOException
	 */
	public DatabaseSpellDictionary(DictionaryRepository dictionaryRepository, File phonetic,
			String encoding) throws IOException {
		super(phonetic, encoding);
		this.dictionaryRepository = dictionaryRepository;
	}

	/**
	 * @param dictionaryRepository
	 * @param phonetic
	 * @throws IOException
	 */
	public DatabaseSpellDictionary(DictionaryRepository dictionaryRepository, File phonetic)
			throws IOException {
		super(phonetic);
		this.dictionaryRepository = dictionaryRepository;
	}

	/**
	 * @param dictionaryRepository
	 * @param phonetic
	 * @throws IOException
	 */
	public DatabaseSpellDictionary(DictionaryRepository dictionaryRepository, Reader phonetic)
			throws IOException {
		super(phonetic);
		this.dictionaryRepository = dictionaryRepository;
	}

	/**
	 * Returns a list of words that have the same phonetic code.
	 * 
	 * @param phoneticCode
	 *            The phonetic code common to the list of words
	 * @return A list of words having the same phonetic code
	 */
	@Override
	protected List<String> getWords(String phoneticCode) {
		List<String> results = new ArrayList<String>();
		for (edu.harvard.fas.rregan.nlp.dictionary.Word word : dictionaryRepository
				.findWordsByPhoneticCode(phoneticCode)) {
			results.add(word.getLemma());
		}
		return results;
	}

	/**
	 * @see com.swabunga.spell.engine.SpellDictionary#addWord(java.lang.String)
	 */
	public void addWord(String text) {
		edu.harvard.fas.rregan.nlp.dictionary.Word word = new edu.harvard.fas.rregan.nlp.dictionary.Word(
				text, getCode(text));
		dictionaryRepository.persist(word);
	}
}
