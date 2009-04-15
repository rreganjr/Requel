/*
 * $Id: VerbMatchingRule.java,v 1.6 2009/02/11 09:02:54 rregan Exp $
 * Copyright (c) 2009 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl.srl;

import java.util.ListIterator;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.nlp.GrammaticalRelationType;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.PartOfSpeech;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.impl.NLPTextImpl;

/**
 * @author ron
 */
public class VerbMatchingRule implements SyntaxMatchingRule {
	private static final Logger log = Logger.getLogger(VerbMatchingRule.class);

	/**
	 * Create a new rule that matches the primary verb verbs.
	 */
	public VerbMatchingRule() {
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.impl.srl.SyntaxMatchingRule#match(edu.harvard.fas.rregan.nlp.impl.srl.SyntaxMatchingContext)
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
