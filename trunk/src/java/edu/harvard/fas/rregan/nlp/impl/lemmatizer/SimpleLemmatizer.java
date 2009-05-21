/*
 * $Id$
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
