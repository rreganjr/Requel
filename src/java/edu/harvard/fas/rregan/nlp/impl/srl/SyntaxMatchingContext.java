/*
 * $Id: SyntaxMatchingContext.java,v 1.7 2009/02/11 09:02:54 rregan Exp $
 * Copyright (c) 2009 Ron Regan Jr. All Rights Reserved.
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
