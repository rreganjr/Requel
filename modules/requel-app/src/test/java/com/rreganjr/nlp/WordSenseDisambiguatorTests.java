/*
 * $Id: WordSenseDisambiguatorTests.java,v 1.5 2008/12/13 00:41:45 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
