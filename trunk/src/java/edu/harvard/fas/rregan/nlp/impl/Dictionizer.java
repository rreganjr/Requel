/*
 * $Id: Dictionizer.java,v 1.7 2009/03/27 07:16:08 rregan Exp $
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

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.harvard.fas.rregan.nlp.GrammaticalStructureLevel;
import edu.harvard.fas.rregan.nlp.NLPProcessor;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.ParseTag;
import edu.harvard.fas.rregan.nlp.PartOfSpeech;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.Sense;
import edu.harvard.fas.rregan.nlp.dictionary.Word;
import edu.harvard.fas.rregan.nlp.dictionary.impl.repository.NoSuchWordException;

/**
 * Assign the WordNet "dictionary" word to each NLPText word.
 * 
 * @author ron
 */
@Component("dictionizer")
public class Dictionizer implements NLPProcessor<NLPText> {
	private static final Logger log = Logger.getLogger(Dictionizer.class);

	private final DictionaryRepository dictionaryRepository;

	/**
	 * @param dictionaryRepository
	 * @param processorFactory
	 */
	@Autowired
	public Dictionizer(DictionaryRepository dictionaryRepository) {
		this.dictionaryRepository = dictionaryRepository;
	}

	@Override
	public NLPText process(NLPText text) {
		if (text.is(GrammaticalStructureLevel.WORD)) {
			if (!text.in(PartOfSpeech.PUNCTUATION, PartOfSpeech.SYMBOL)) {
				try {
					Word dictionaryWord = null;
					Sense dictionaryWordSense = null;
					if (text.in(ParseTag.CD)) {
						// TODO: look at the siblings and possibly combine
						// elements into more advanced numbers, for example:
						// (NP (CD 1) (. .) (CD 25)) -> (NP (NN 1.25))
						dictionaryWord = dictionaryRepository
								.findWord("integer", PartOfSpeech.NOUN);
						dictionaryWordSense = dictionaryWord.getSense(PartOfSpeech.NOUN, 1);
						// TODO: change the part of speech to NUMBER

					}
					if (text.in(ParseTag.NNP, ParseTag.NNPS)) {
						// TODO: use named entity recognizer to get the most
						// specific regular noun replacement
						dictionaryWord = dictionaryRepository.findWord("entity", text
								.getPartOfSpeech());
						dictionaryWordSense = dictionaryWord.getSense(PartOfSpeech.NOUN, 1);
					} else {
						dictionaryWord = dictionaryRepository.findWord(text.getLemma(), text
								.getPartOfSpeech());
					}
					text.setDictionaryWord(dictionaryWord);
					text.setDictionaryWordSense(dictionaryWordSense);
				} catch (NoSuchWordException e) {
					// not a dictionary word
				}
			}
		} else {
			for (NLPText word : text.getLeaves()) {
				process(word);
			}
		}
		return text;
	}
}
