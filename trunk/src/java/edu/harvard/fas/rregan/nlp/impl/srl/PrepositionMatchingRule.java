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
package edu.harvard.fas.rregan.nlp.impl.srl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.PartOfSpeech;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;

/**
 * @author ron
 */
public class PrepositionMatchingRule implements SyntaxMatchingRule {
	private static final Logger log = Logger.getLogger(PrepositionMatchingRule.class);

	// list of prepositions that the match must be one of.
	private final List<String> matchingFilter;

	/**
	 * Create a rule that matches any preposition
	 */
	public PrepositionMatchingRule() {
		matchingFilter = new ArrayList<String>();
	}

	/**
	 * Create a rule that matches one of the supplied preposition
	 * 
	 * @param prepositions -
	 *            a space delimited list of prepositions to match.
	 */
	public PrepositionMatchingRule(String prepositions) {
		matchingFilter = Arrays.asList(prepositions.split(" "));
	}

	/**
	 * 
	 */
	@Override
	public void match(DictionaryRepository dictionaryRepository, NLPText verb,
			ListIterator<NLPText> textIterator) throws SemanticRoleLabelerException {
		NLPText word = textIterator.next();
		if (word.is(PartOfSpeech.PREPOSITION)) {
			if (matchingFilter.size() > 0) {
				for (String matchWord : matchingFilter) {
					if (word.getText().equalsIgnoreCase(matchWord)) {
						log.debug("matched: " + word.getText());
						return;
					}
				}
			} else {
				log.debug("matched: " + word.getText());
				return;
			}
		}
		throw SemanticRoleLabelerException.matchFailed(this, word);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + matchingFilter;
	}
}
