/*
 * $Id: AdjectiveMatchingRule.java,v 1.3 2009/02/11 09:02:54 rregan Exp $
 * Copyright (c) 2009 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl.srl;

import java.util.ListIterator;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.PartOfSpeech;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;

/**
 * @author ron
 */
public class AdjectiveMatchingRule implements SyntaxMatchingRule {
	private static final Logger log = Logger.getLogger(AdjectiveMatchingRule.class);

	/**
	 * 
	 */
	@Override
	public void match(DictionaryRepository dictionaryRepository, NLPText verb,
			ListIterator<NLPText> textIterator) throws SemanticRoleLabelerException {
		NLPText word = textIterator.next();
		// TODO: what about an adjective phrase?
		if (!word.is(PartOfSpeech.ADJECTIVE)) {
			throw SemanticRoleLabelerException.matchFailed(this, word);
		}
		log.debug("matched: " + word.getText());
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
