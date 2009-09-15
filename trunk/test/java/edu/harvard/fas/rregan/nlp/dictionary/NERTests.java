/*
 * $Id: NERTests.java,v 1.1 2008/12/14 04:05:35 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.dictionary;

import edu.harvard.fas.rregan.AbstractIntegrationTestCase;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.PartOfSpeech;
import edu.harvard.fas.rregan.nlp.impl.StanfordNameEntityRecognizer;

/**
 * @author ron
 */
public class NERTests extends AbstractIntegrationTestCase {

	private StanfordNameEntityRecognizer nameEntityRecognizer;
	private Sense personSense;
	private Sense locationSense;
	private Sense organizationSense;

	/**
	 * @param name
	 */
	public NERTests() {
		super();
	}

	@Override
	protected void onSetUp() throws Exception {
		super.onSetUp();
		nameEntityRecognizer = new StanfordNameEntityRecognizer(getDictionaryRepository());
		Word personWord = getDictionaryRepository().findWord("person", PartOfSpeech.NOUN);
		personSense = personWord.getSense(PartOfSpeech.NOUN, 1);

		Word locationWord = getDictionaryRepository().findWord("location", PartOfSpeech.NOUN);
		locationSense = locationWord.getSense(PartOfSpeech.NOUN, 1);

		Word organizationWord = getDictionaryRepository().findWord("organization",
				PartOfSpeech.NOUN);
		organizationSense = organizationWord.getSense(PartOfSpeech.NOUN, 1);
	}

	public void testNER() {
		String sentence = "Nellymoser will design and develop a Streaming Audio and Video product for Virgin Mobile USA's first EVDO device.";
		NLPText text = process(sentence);
		nameEntityRecognizer.process(text);
		for (NLPText word : text.getLeaves()) {
			if (word.getText().equals("Nellymoser")) {
				assertEquals(personSense, word.getDictionaryWordSense());
			} else if (word.getText().equals("Virgin")) {
				assertEquals(organizationSense, word.getDictionaryWordSense());
			} else if (word.getText().equals("Mobile")) {
				assertEquals(organizationSense, word.getDictionaryWordSense());
			} else if (word.getText().equals("USA")) {
				assertEquals(organizationSense, word.getDictionaryWordSense());
			} else if (word.getText().equals("EVDO")) {
				assertEquals(organizationSense, word.getDictionaryWordSense());
			} else {
				assertNull(word.getDictionaryWordSense());
			}
		}
	}

	public void testNER2() {
		String sentence = "The streaming video product will be a new VMU-branded service designed by Nellymoser and approved by VMU.";
		NLPText text = process(sentence);
		nameEntityRecognizer.process(text);
		for (NLPText word : text.getLeaves()) {
			if (word.getText().equals("Nellymoser")) {
				assertEquals(personSense, word.getDictionaryWordSense());
			} else if (word.getText().equals("VMU")) {
				assertEquals(organizationSense, word.getDictionaryWordSense());
			} else {
				assertNull(word.getDictionaryWordSense());
			}
		}
	}

	public void testNER3() {
		String sentence = "John is the CEO of Nellymoser.";
		NLPText text = process(sentence);
		nameEntityRecognizer.process(text);
		for (NLPText word : text.getLeaves()) {
			if (word.getText().equals("John")) {
				assertEquals(personSense, word.getDictionaryWordSense());
			} else if (word.getText().equals("Nellymoser")) {
				assertEquals(personSense, word.getDictionaryWordSense());
			} else if (word.getText().equals("Arlington")) {
				// assertEquals(locationSense, word.getDictionaryWordSense());
			} else if (word.getText().equals("Massachusetts")) {
				// assertEquals(locationSense, word.getDictionaryWordSense());
			} else {
				assertNull(word.getDictionaryWordSense());
			}
		}
	}

	private NLPText process(String sentence) {
		NLPText text = getNlpProcessorFactory().createNLPText(sentence);
		getNlpProcessorFactory().getSentencizer().process(text);
		getNlpProcessorFactory().getParser().process(text);
		return text;
	}

}
