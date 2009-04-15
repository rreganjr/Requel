/*
 * $Id: AdverbMatchingRule.java,v 1.3 2009/02/11 09:02:54 rregan Exp $
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
public class AdverbMatchingRule implements SyntaxMatchingRule {
	private static final Logger log = Logger.getLogger(AdverbMatchingRule.class);

	/**
	 * 
	 */
	@Override
	public void match(DictionaryRepository dictionaryRepository, NLPText verb,
			ListIterator<NLPText> textIterator) throws SemanticRoleLabelerException {
		NLPText word = textIterator.next();
		if (!word.is(PartOfSpeech.ADVERB)) {
			throw SemanticRoleLabelerException.matchFailed(this, word);
		}
		log.debug("matched: " + word.getText());
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}