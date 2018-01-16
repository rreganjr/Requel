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
package com.rreganjr.nlp.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rreganjr.nlp.GrammaticalStructureLevel;
import com.rreganjr.nlp.NLPProcessor;
import com.rreganjr.nlp.NLPText;
import com.rreganjr.nlp.PartOfSpeech;
import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.Sense;

/**
 * Find words that are more specific (less vague) than the supplied word. This
 * uses the WordNet Hyponym hierarchy to find a set of words with a higher
 * information context.
 * 
 * @author ron
 */
@Component("moreSpecificWordSuggester")
public class MoreSpecificWordSuggester implements NLPProcessor<Collection<NLPText>> {
	private static final Logger log = Logger.getLogger(SpellingSuggester.class);

	private final DictionaryRepository dictionaryRepository;

	/**
	 * @param dictionaryRepository
	 */
	@Autowired
	public MoreSpecificWordSuggester(DictionaryRepository dictionaryRepository) {
		this.dictionaryRepository = dictionaryRepository;
	}

	@Override
	public Collection<NLPText> process(NLPText text) {
		if (text.is(GrammaticalStructureLevel.WORD) && text.hasText()
				&& !text.is(PartOfSpeech.PUNCTUATION)) {
			List<NLPText> results = new ArrayList<NLPText>();
			for (Sense sense : dictionaryRepository.findMoreSpecificWords(text
					.getDictionaryWordSense(), 5)) {
				NLPText word = new NLPTextImpl(sense.getWord().getLemma(),
						GrammaticalStructureLevel.WORD);
				word.setDictionaryWord(sense.getWord());
				word.setDictionaryWordSense(sense);
				results.add(word);
			}
			return results;
		}
		return Collections.EMPTY_LIST;
	}
}
