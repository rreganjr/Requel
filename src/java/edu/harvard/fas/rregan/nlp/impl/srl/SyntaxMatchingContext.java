/*
 * $Id: SyntaxMatchingContext.java,v 1.7 2009/02/11 09:02:54 rregan Exp $
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

import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.PartOfSpeech;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.VerbNetFrameRef;

/**
 * A context for applying SyntaxMatchingRules to an NLPText. The context applies
 * a single set of rules set a creation time to various NLPTexts via the
 * matches() method.
 * 
 * @author ron
 */
public class SyntaxMatchingContext {
	private static final Logger log = Logger.getLogger(SyntaxMatchingContext.class);

	private final VerbNetFrameRef frameRef;
	private final List<SyntaxMatchingRule> rules;

	/**
	 * @param frameRef
	 * @param rules
	 */
	public SyntaxMatchingContext(VerbNetFrameRef frameRef, List<SyntaxMatchingRule> rules) {
		this.frameRef = frameRef;
		this.rules = rules;
	}

	/**
	 * @param dictionaryRepository
	 * @param nlpText -
	 *            the text to match the rules
	 * @param primaryVerb -
	 *            the primary verb of the supplied nlpText to extract the
	 *            thematic roles for.
	 * @return true if the supplied nlpText matches the rules in the context.
	 */
	public boolean matches(DictionaryRepository dictionaryRepository, NLPText nlpText,
			NLPText primaryVerb) {
		List<NLPText> leaves = nlpText.getLeaves();
		ListIterator<NLPText> leafIterator = leaves.listIterator();
		log.info("start matching: " + frameRef);
		try {
			for (SyntaxMatchingRule rule : rules) {
				skipNoise(leafIterator);
				if (!leafIterator.hasNext()) {
					throw SemanticRoleLabelerException.unmatchedRulesRemaining(rules.subList(rules
							.indexOf(rule), rules.size()));
				}
				log.info("current word: " + leaves.get(leafIterator.nextIndex()));
				rule.match(dictionaryRepository, primaryVerb, leafIterator);
			}
			// match the ending punctuation
			skipNoise(leafIterator);
			if (leafIterator.hasNext()) {
				throw SemanticRoleLabelerException.unmatchedSentenceElementsRemaining(nlpText
						.getTextRange(leafIterator.nextIndex(), leaves.size() - 1));
			}
		} catch (SemanticRoleLabelerException e) {
			log.info("matching failed: " + frameRef + " error: " + e.getMessage());
			return false;
		}
		log.info("matching successful: " + frameRef);
		return true;
	}

	protected void skipNoise(ListIterator<NLPText> iter) {
		while (iter.hasNext()) {
			NLPText word = iter.next();
			if (!word.in(PartOfSpeech.PUNCTUATION, PartOfSpeech.SYMBOL)) {
				iter.previous();
				return;
			}
		}
	}
}
