/*
 * $Id: PrepositionMatchingRule.java,v 1.3 2009/02/11 09:02:55 rregan Exp $
 * Copyright (c) 2009 Ron Regan Jr. All Rights Reserved.
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
