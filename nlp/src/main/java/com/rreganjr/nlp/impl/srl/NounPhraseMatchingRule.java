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

import com.rreganjr.nlp.GrammaticalRelation;
import com.rreganjr.nlp.GrammaticalRelationType;
import com.rreganjr.nlp.NLPText;
import com.rreganjr.nlp.ParseTag;
import com.rreganjr.nlp.PartOfSpeech;
import com.rreganjr.nlp.SemanticRole;
import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.Synset;
import com.rreganjr.nlp.dictionary.VerbNetRoleRef;
import com.rreganjr.nlp.dictionary.VerbNetSelectionRestriction;
import com.rreganjr.nlp.impl.NLPTextImpl;

/**
 * @author ron
 */
public class NounPhraseMatchingRule implements SyntaxMatchingRule {
	private static final Log log = LogFactory.getLog(NounPhraseMatchingRule.class);

	private final VerbNetRoleRef roleRef;
	private final SemanticRole semanticRole;

	/**
	 * Create a new rule for matching a noun phrase that fills the specified
	 * thematic role.
	 * 
	 * @param roleRef -
	 *            a reference to the thematic role including selection
	 *            restrictions.
	 */
	public NounPhraseMatchingRule(VerbNetRoleRef roleRef) {
		this.roleRef = roleRef;
		semanticRole = roleRef.getVerbNetRole().getSemanticRole();
	}

	/**
	 * 
	 */
	@Override
	public void match(DictionaryRepository dictionaryRepository, NLPText verb,
			ListIterator<NLPText> textIterator) throws SemanticRoleLabelerException {
		NLPText word = textIterator.next();
		// TODO: syntax restrictions? some roles may be filled by full clauses
		// that may not be the immediate parent
		if (word.getParent().is(ParseTag.NP) || semanticRole.equals(SemanticRole.TOPIC)) {
			// find the noun in this phrase that is the subject/object of the
			// main verb or a modifier of the main verb and see if it meets
			// the restriction.
			if (roleRef.getSelectionalRestrictions().size() > 0) {
				NLPText mainNoun = getMainNoun(word.getParent(), verb);
				if (mainNoun.getDictionaryWordSense() != null) {
					// TODO: this assumes the restrictions are or-ed together
					for (VerbNetSelectionRestriction res : roleRef.getSelectionalRestrictions()) {
						if (meetsRestriction(dictionaryRepository, res, mainNoun)) {
							word.getParent().setSemanticRole(verb, semanticRole);
							consumeWordsToEndOfNode(textIterator, word.getParent());
							return;
						}
					}
					throw SemanticRoleLabelerException.matchFailedBySelectionalRestriction(this,
							mainNoun);
				}
				throw SemanticRoleLabelerException.matchFailed(this, word);
			}
			word.getParent().setSemanticRole(verb, semanticRole);
			consumeWordsToEndOfNode(textIterator, word.getParent());
			return;
		}
		throw SemanticRoleLabelerException.matchFailed(this, word);
	}

	private NLPText getMainNoun(NLPText phrase, NLPText verb) {
		for (NLPText word : phrase.getLeaves()) {
			if (word.is(PartOfSpeech.NOUN)) {
				for (GrammaticalRelation rel : word.getDependentOf()) {
					if (hasRelationToVerb(rel, verb)) {
						log.debug("main noun: " + word);
						return word;
					}
				}
			}
		}
		throw SemanticRoleLabelerException.matchFailed(this, phrase);
	}

	private void consumeWordsToEndOfNode(ListIterator<NLPText> textIterator, NLPText node) {
		NLPText matchedWords = new NLPTextImpl();
		while (textIterator.hasNext()) {
			NLPText word = textIterator.next();
			if (!word.isDescendentOf(node)) {
				textIterator.previous();
				log.debug("matched: " + matchedWords.getText());
				return;
			}
			matchedWords.addRef(word);
		}
	}

	private boolean meetsRestriction(DictionaryRepository dictionaryRepository,
			VerbNetSelectionRestriction restriction, NLPText word) {
		log.debug("word: " + word + " restriction " + restriction.getInclude()
				+ restriction.getType().getName());
		Synset restrictionSynset = restriction.getType().getSynset();
		log.debug("restriction synset: " + restrictionSynset);
		Synset wordSynset = word.getDictionaryWordSense().getSynset();
		log.debug("word synset: " + wordSynset);
		boolean isHyponym = dictionaryRepository.isHyponym(restrictionSynset, wordSynset);
		if (("+".equals(restriction.getInclude()) && isHyponym)
				|| ("-".equals(restriction.getInclude()) && !isHyponym)) {
			return true;
		}
		return false;
	}

	private boolean hasRelationToVerb(GrammaticalRelation rel, NLPText verb) {
		GrammaticalRelationType relType = rel.getType();
		// the object/subject of the verb
		if (rel.getGovernor().equals(verb)
				&& (relType.isA(GrammaticalRelationType.OBJECT) || relType
						.isA(GrammaticalRelationType.SUBJECT))) {
			return true;
		}
		// object/subject of a preposition that modifies the verb
		if (rel.getGovernor().is(PartOfSpeech.PREPOSITION)
				&& (relType.isA(GrammaticalRelationType.OBJECT) || relType
						.isA(GrammaticalRelationType.SUBJECT))) {
			for (GrammaticalRelation rel2 : rel.getGovernor().getDependentOf()) {
				if (rel2.getType().isA(GrammaticalRelationType.PREPOSITION)
						&& rel2.getGovernor().equals(verb)) {
					return true;
				}
			}
		}

		// adjust for bad prepositional phrase attachement
		// object/subject of a preposition that is a modifier of a
		// subject/object of the verb
		if (rel.getGovernor().is(PartOfSpeech.PREPOSITION)) {
			for (GrammaticalRelation rel2 : rel.getGovernor().getDependentOf()) {
				if (rel2.getType().isA(GrammaticalRelationType.PREPOSITION)) {
					// modifies an object/subject of the verb
					NLPText thing = rel2.getGovernor();
					for (GrammaticalRelation rel3 : verb.getGovernorOf()) {
						log.debug(rel3 + " -> " + thing);
						relType = rel3.getType();
						if (rel3.getDependent().equals(thing)
								&& (relType.isA(GrammaticalRelationType.OBJECT) || relType
										.isA(GrammaticalRelationType.SUBJECT))) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + semanticRole + " "
				+ roleRef.getSelectionalRestrictions();
	}
}
