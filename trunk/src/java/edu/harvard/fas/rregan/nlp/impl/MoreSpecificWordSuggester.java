/*
 * $Id: MoreSpecificWordSuggester.java,v 1.1 2009/03/27 07:16:08 rregan Exp $
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
import edu.harvard.fas.rregan.nlp.dictionary.Sense;

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
