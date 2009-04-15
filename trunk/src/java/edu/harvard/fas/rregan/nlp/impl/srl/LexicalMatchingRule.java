/*
 * $Id: LexicalMatchingRule.java,v 1.3 2009/02/11 09:02:54 rregan Exp $
 * Copyright (c) 2009 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl.srl;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;

/**
 * Match a specific string or one of a specific string.
 * 
 * @author ron
 */
public class LexicalMatchingRule implements SyntaxMatchingRule {
	private static final Logger log = Logger.getLogger(LexicalMatchingRule.class);

	// list of prepositions that the match must be one of.
	private final List<String> matchingFilter;

	/**
	 * Create a rule that matches one of the supplied words
	 * 
	 * @param filter -
	 *            a space delimited list of words to match.
	 */
	public LexicalMatchingRule(String filter) {
		matchingFilter = Arrays.asList(filter.split(" "));
	}

	/**
	 * 
	 */
	@Override
	public void match(DictionaryRepository dictionaryRepository, NLPText verb,
			ListIterator<NLPText> textIterator) throws SemanticRoleLabelerException {
		NLPText word = textIterator.next();
		if (matchingFilter.size() > 0) {
			for (String matchWord : matchingFilter) {
				// TODO: match lemma?
				if (word.getText().equalsIgnoreCase(matchWord)) {
					log.debug("matched: " + word.getText());
					return;
				}
			}
		} else {
			log.debug("matched: " + word.getText());
			return;
		}
		throw SemanticRoleLabelerException.matchFailed(this, word);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + matchingFilter;
	}
}
