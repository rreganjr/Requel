/*
 * $Id: SyntaxMatchingRule.java,v 1.3 2009/02/11 09:02:55 rregan Exp $
 * Copyright (c) 2009 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl.srl;

import java.util.ListIterator;

import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;

/**
 * A rule derived from the VerbNet frame syntax that matches a syntactic element
 * of a sentance potentially identifying a semantic role.
 * 
 * @author ron
 */
public interface SyntaxMatchingRule {

	/**
	 * Apply the rule to the current nlpText in the context and return the
	 * matched nlpText or throw a SemanticRoleLabelerException if the text
	 * wasn't matched.
	 * 
	 * @param dictionaryRepository
	 * @param verb -
	 *            The primary verb of the text being processed.
	 * @param textIterator -
	 *            an interator to get the next word level token in the text or
	 *            step back if the next word is not handled by the rule.
	 * @throws SemanticRoleLabelerException -
	 *             if the rule can't be applied to the current state of the
	 *             text.
	 */
	public void match(DictionaryRepository dictionaryRepository, NLPText verb,
			ListIterator<NLPText> textIterator) throws SemanticRoleLabelerException;
}
