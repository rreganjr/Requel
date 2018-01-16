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
package com.rreganjr.nlp.impl.srl;

import java.util.ListIterator;

import com.rreganjr.nlp.NLPText;
import com.rreganjr.nlp.dictionary.DictionaryRepository;

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
