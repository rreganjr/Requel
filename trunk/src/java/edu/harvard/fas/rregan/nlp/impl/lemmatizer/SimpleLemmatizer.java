/*
 * $Id: SimpleLemmatizer.java,v 1.2 2009/01/26 10:19:00 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.impl.lemmatizer;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.harvard.fas.rregan.nlp.GrammaticalStructureLevel;
import edu.harvard.fas.rregan.nlp.LemmatizerRule;
import edu.harvard.fas.rregan.nlp.NLPProcessor;
import edu.harvard.fas.rregan.nlp.NLPText;

/**
 * Lemmatizer that uses a set of rules to find a matching lemma in the
 * dictionary.<br>
 * The lemmatizer rules are configured in the spring lemmatizerConfig.xml file.
 * 
 * @author ron
 */
@Component("lemmatizer")
public class SimpleLemmatizer implements NLPProcessor<NLPText> {

	private final Set<LemmatizerRule> rules;

	/**
	 * @param rules -
	 *            a set of rules for lemmatizing words.
	 */
	@Autowired
	public SimpleLemmatizer(Set<LemmatizerRule> rules) {
		this.rules = rules;
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.NLPProcessor#process(edu.harvard.fas.rregan.nlp.NLPText)
	 */
	@Override
	public NLPText process(NLPText text) {
		if (text.is(GrammaticalStructureLevel.WORD)) {
			for (LemmatizerRule rule : rules) {
				String lemma = rule.lemmatize(text.getText(), text.getPartOfSpeech());
				if (lemma != null) {
					text.setLemma(lemma);
					break;
				}
			}
		} else {
			for (NLPText word : text.getLeaves()) {
				process(word);
			}
		}
		return text;
	}
}
