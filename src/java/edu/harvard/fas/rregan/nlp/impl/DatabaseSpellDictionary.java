/*
 * $Id: DatabaseSpellDictionary.java,v 1.5 2009/01/24 11:08:38 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
