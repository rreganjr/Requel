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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rreganjr.nlp.GrammaticalRelationType;
import com.rreganjr.nlp.NLPText;
import com.rreganjr.nlp.PartOfSpeech;
import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.impl.NLPTextImpl;

/**
 * @author ron
 */
public class VerbMatchingRule implements SyntaxMatchingRule {
	private static final Log log = LogFactory.getLog(VerbMatchingRule.class);

	/**
	 * Create a new rule that matches the primary verb verbs.
	 */
	public VerbMatchingRule() {
	}

	/**
	 * @see com.rreganjr.nlp.impl.srl.SyntaxMatchingRule#match(com.rreganjr.nlp.impl.srl.SyntaxMatchingContext)
	 */
	@Override
	public void match(DictionaryRepository dictionaryRepository, NLPText verb,
			ListIterator<NLPText> textIterator) throws SemanticRoleLabelerException {
		NLPText matchedWords = new NLPTextImpl();
		NLPText word = textIterator.next();

		// http://en.wikipedia.org/wiki/Auxiliary_verb
		// optional modal auxiliary verbs: can/could, may/might, shall/should,
		// will/would, must, ought.
		if (word.is(PartOfSpeech.MODAL)
				&& word.isDependentOf(GrammaticalRelationType.AUXILIARY, verb)) {
			matchedWords.addRef(word);
			word = textIterator.next();
			// match for "not"
			if ("not".equalsIgnoreCase(word.getText()) && word.is(PartOfSpeech.ADVERB)
					&& word.isDependentOf(GrammaticalRelationType.NEGATION_MODIFIER, verb)) {
				matchedWords.addRef(word);
				word = textIterator.next();
			}
		}

		// I have/had <verb>
		// optional auxiliary verb: have, had
		if (word.is(PartOfSpeech.VERB)
				&& word.isDependentOf(GrammaticalRelationType.AUXILIARY, verb)) {
			matchedWords.addRef(word);
			word = textIterator.next();
			// match for "not"
			if ("not".equalsIgnoreCase(word.getText()) && word.is(PartOfSpeech.ADVERB)
					&& word.isDependentOf(GrammaticalRelationType.NEGATION_MODIFIER, verb)) {
				matchedWords.addRef(word);
				word = textIterator.next();
			}
		}

		// optional auxiliary verb: be
		if (word.is(PartOfSpeech.VERB)
				&& word.isDependentOf(GrammaticalRelationType.AUXILIARY, verb)) {
			matchedWords.addRef(word);
			word = textIterator.next();
			// match for "not"
		}

		// finally the verb!
		if (word.is(PartOfSpeech.VERB) && verb.equals(word)) {
			matchedWords.addRef(word);
			log.debug("matched: " + matchedWords.getText());
			return;
		}
		matchedWords.addRef(word);
		throw SemanticRoleLabelerException.matchFailed(this, matchedWords);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}
