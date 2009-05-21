/*
 * $Id$
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
