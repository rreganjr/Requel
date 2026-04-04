/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.nlp;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.nlp.dictionary.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author ron
 */
@RunWith(SpringRunner.class)
public class WordSenseDisambiguatorTests extends AbstractIntegrationTestCase {
	private static final String TEST_SENTENCE_1 = "I charged the purchase on my credit card.";
	private static final String TEST_SENTENCE_2 = "I charged in to the bar.";
	private static final String TEST_SENTENCE_3 = "John was charged with grand larceny.";

	/**
	 * @param name
	 */
	public WordSenseDisambiguatorTests() {
		super();
	}

	/**
	 * Test method for
	 * {@link com.rreganjr.nlp.impl.wsd.SimpleLeskWSD#process(NLPText)}.
	 */
	@Test
	public void testSentence1() {
		report(process(getNlpProcessorFactory().createNLPText(TEST_SENTENCE_1)));
	}

	@Test
	public void testSentence2() {
		report(process(getNlpProcessorFactory().createNLPText(TEST_SENTENCE_2)));
	}

	@Test
	public void testSentence3() {
		report(process(getNlpProcessorFactory().createNLPText(TEST_SENTENCE_3)));
	}

	private NLPText process(NLPText nlpText) {
		getNlpProcessorFactory().getSentencizer().process(nlpText);
		getNlpProcessorFactory().getParser().process(nlpText);
		getNlpProcessorFactory().getLemmatizer().process(nlpText);
		getNlpProcessorFactory().getDictionizer().process(nlpText);
		getNlpProcessorFactory().getWordSenseDisambiguator().process(nlpText);
		return nlpText;
	}

	private void report(NLPText nlpText) {
		System.out.println((getNlpProcessorFactory().getConstituentTreePrinter().process(nlpText)));
		for (NLPText word : nlpText.getLeaves()) {
			Word dicWord = word.getDictionaryWord();
			Sense sense = word.getDictionaryWordSense();
			Synset synset = (sense == null ? null : sense.getSynset());
			Category category = (synset == null ? null : synset.getCategory());
			System.out.println(word.getText()
					+ " "
					+ word.getParseTag()
					+ " "
					+ word.getPartOfSpeech()
					+ (dicWord == null ? "" : " " + dicWord.getLemma())
					+ " "
					+ (sense == null ? "" : " " + sense.getRank() + " "
							+ sense.getSampleFrequency()) + " "
					+ (synset == null ? "" : " " + synset.getDefinition()) + " "
					+ (category == null ? "" : " " + category.getName()) + " ");
			if (dicWord != null) {
				for (Sense aSense : dicWord.getSenses(word.getPartOfSpeech())) {
					System.out.println(dicWord.getLemma() + " - " + aSense.getRank() + " "
							+ aSense.getSampleFrequency() + " "
							+ aSense.getSynset().getDefinition() + " "
							+ aSense.getSynset().getCategory().getName());
				}
			}
		}

	}
}
