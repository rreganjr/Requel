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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rreganjr.nlp.GrammaticalStructureLevel;
import com.rreganjr.nlp.NLPProcessor;
import com.rreganjr.nlp.NLPText;
import com.rreganjr.nlp.ParseTag;
import com.rreganjr.nlp.PartOfSpeech;
import com.rreganjr.nlp.dictionary.DictionaryRepository;

/**
 * Check if a word is spelled correctly
 * 
 * @author ron
 */
@Component("spellingChecker")
public class SpellingChecker implements NLPProcessor<Boolean> {
	private static final Log log = LogFactory.getLog(SpellingChecker.class);

	private final DictionaryRepository dictionaryRepository;

	/**
	 * @param dictionaryRepository
	 */
	@Autowired
	public SpellingChecker(DictionaryRepository dictionaryRepository) {
		this.dictionaryRepository = dictionaryRepository;
	}

	@Override
	public Boolean process(NLPText text) {
		if (text.is(GrammaticalStructureLevel.WORD) && text.hasText()) {
			if (!text.in(PartOfSpeech.PUNCTUATION, PartOfSpeech.NUMBER, PartOfSpeech.SYMBOL)
					&& !text.in(ParseTag.POS)) {
				// TODO: parser error let's -> let us -> (VB let) (POS 's)
				// TODO: handle contractions
				if (text.in(ParseTag.VBZ, ParseTag.PRP) && text.getText().equalsIgnoreCase("'s")) {
					// contractions like It's for it is and let's for let us
					// TODO: replace it with "is" or "us"?
				} else if (text.in(ParseTag.MD) && text.getText().equalsIgnoreCase("'ll")) {
					// contractions like I'll for I will
					// TODO: replace it with "will"?
				} else if (text.in(ParseTag.VBP) && text.getText().equalsIgnoreCase("'ve")) {
					// contractions like I've for I have
					// TODO: replace it with "have"?
				} else if (text.in(ParseTag.VBP) && text.getText().equalsIgnoreCase("'m")) {
					// contractions like I'm for I am
					// TODO: replace it with "am"?
				} else if (text.in(ParseTag.VBP) && text.getText().equalsIgnoreCase("'re")) {
					// contractions like you're for you are
					// TODO: replace it with "are"?
				} else if (text.in(PartOfSpeech.ADVERB) && text.getText().equalsIgnoreCase("n't")) {
					// contractions like don't for do not
					// TODO: replace it with "not"?
					// TODO: what about the lead word like can't -> (MD ca) (RB
					// n't)
				} else {
					return dictionaryRepository.isKnownWord(text.getText());
				}
			}
		} else {
			for (NLPText word : text.getLeaves()) {
				if (!process(word)) {
					return Boolean.FALSE;
				}
			}
		}
		return Boolean.TRUE;
	}
}
