/*
 * $Id: SpellingChecker.java,v 1.6 2009/01/28 06:01:55 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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

/**
 * Check if a word is spelled correctly
 * 
 * @author ron
 */
@Component("spellingChecker")
public class SpellingChecker implements NLPProcessor<Boolean> {
	private static final Logger log = Logger.getLogger(SpellingChecker.class);

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
