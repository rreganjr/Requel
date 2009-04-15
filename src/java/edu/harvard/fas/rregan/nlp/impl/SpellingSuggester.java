/*
 * $Id: SpellingSuggester.java,v 1.5 2008/12/13 00:40:00 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.harvard.fas.rregan.nlp.GrammaticalStructureLevel;
import edu.harvard.fas.rregan.nlp.NLPProcessor;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.PartOfSpeech;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;

/**
 * Find spelling suggestions
 * 
 * @author ron
 */
@Component("spellingSuggester")
public class SpellingSuggester implements NLPProcessor<Collection<NLPText>> {
	private static final Logger log = Logger.getLogger(SpellingSuggester.class);

	private final DictionaryRepository dictionaryRepository;

	/**
	 * @param dictionaryRepository
	 */
	@Autowired
	public SpellingSuggester(DictionaryRepository dictionaryRepository) {
		this.dictionaryRepository = dictionaryRepository;
	}

	@Override
	public Collection<NLPText> process(NLPText text) {
		if (text.is(GrammaticalStructureLevel.WORD) && text.hasText()
				&& !text.is(PartOfSpeech.PUNCTUATION)) {
			List<NLPText> results = new ArrayList<NLPText>();
			for (String word : dictionaryRepository.findSpellingSuggestions(text.getText(), 2)) {
				results.add(new NLPTextImpl(word, GrammaticalStructureLevel.WORD));
			}
			return results;
		}
		return Collections.EMPTY_LIST;
	}
}
