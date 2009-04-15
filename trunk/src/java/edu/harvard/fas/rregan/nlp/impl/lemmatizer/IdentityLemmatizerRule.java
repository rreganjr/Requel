/*
 * $Id: IdentityLemmatizerRule.java,v 1.3 2008/12/15 06:36:00 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl.lemmatizer;

import edu.harvard.fas.rregan.nlp.PartOfSpeech;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.impl.repository.NoSuchWordException;

/**
 * The identity rule just searches for the given word as the lemma for the given
 * part of speech and returns the word if it exists in the dictionary. <br>
 * For example:<br>
 * saw + verb -> null<br>
 * saw + noun -> saw<br>
 * saws + noun -> null<br>
 * 
 * @author ron
 */
public class IdentityLemmatizerRule extends AbstractDictionaryLemmatizerRule {

	/**
	 * @param dictionaryRepository
	 */
	public IdentityLemmatizerRule(DictionaryRepository dictionaryRepository) {
		super(dictionaryRepository);
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.LemmatizerRule#lemmatize(java.lang.String,
	 *      edu.harvard.fas.rregan.nlp.PartOfSpeech)
	 */
	@Override
	public String lemmatize(String word, PartOfSpeech partOfSpeech) {
		try {
			return getDictionaryRepository().findWord(word, partOfSpeech).getLemma();
		} catch (NoSuchWordException e) {
			// no match
		}
		return null;
	}
}
