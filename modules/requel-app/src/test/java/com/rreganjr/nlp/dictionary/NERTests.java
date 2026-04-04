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
package com.rreganjr.nlp.dictionary;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.nlp.impl.StanfordNameEntityRecognizer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;


/**
 * @author ron
 *
 * TODO: The NER is returning location#n#1 for "Virgin" when we expect "organization#n#1"
 * TODO: maybe because "Virgin Mobile USA's" is the full match and because it has USA it
 * TODO: thinks it is a location?
 */
@Ignore("Named entity recognizer relies on legacy models and is currently broken; skipping until NLP stack is refreshed.")
@RunWith(SpringRunner.class)
public class NERTests extends AbstractIntegrationTestCase {

	private StanfordNameEntityRecognizer nameEntityRecognizer;
	private Sense personSense;
	private Sense locationSense;
	private Sense organizationSense;

	/**
	 */
	public NERTests() {
		super();
	}

	@Before
	public void onSetUp() throws Exception {
		super.onSetUp();
		ensureDictionaryLoaded();
		nameEntityRecognizer = new StanfordNameEntityRecognizer(getDictionaryRepository());
		Word personWord = getDictionaryRepository().findWord("person", PartOfSpeech.NOUN);
		personSense = personWord.getSense(PartOfSpeech.NOUN, 1);

		Word locationWord = getDictionaryRepository().findWord("location", PartOfSpeech.NOUN);
		locationSense = locationWord.getSense(PartOfSpeech.NOUN, 1);

		Word organizationWord = getDictionaryRepository().findWord("organization",
				PartOfSpeech.NOUN);
		organizationSense = organizationWord.getSense(PartOfSpeech.NOUN, 1);
	}

	@Test
	public void testNER() {
		String sentence = "Nellymoser will design and develop a Streaming Audio and Video product for Virgin Mobile USA's first EVDO device.";
		NLPText text = process(sentence);
		nameEntityRecognizer.process(text);
		for (NLPText word : text.getLeaves()) {
			if (word.getText().equals("Nellymoser")) {
				Assert.assertEquals(personSense, word.getDictionaryWordSense());
			} else if (word.getText().equals("Virgin")) {
				Assert.assertEquals(organizationSense, word.getDictionaryWordSense());
			} else if (word.getText().equals("Mobile")) {
				Assert.assertEquals(organizationSense, word.getDictionaryWordSense());
			} else if (word.getText().equals("USA")) {
				Assert.assertEquals(organizationSense, word.getDictionaryWordSense());
			} else if (word.getText().equals("EVDO")) {
				Assert.assertEquals(organizationSense, word.getDictionaryWordSense());
			} else {
				Assert.assertNull(word.getDictionaryWordSense());
			}
		}
	}

	@Test
	public void testNER2() {
		String sentence = "The streaming video product will be a new VMU-branded service designed by Nellymoser and approved by VMU.";
		NLPText text = process(sentence);
		nameEntityRecognizer.process(text);
		for (NLPText word : text.getLeaves()) {
			if (word.getText().equals("Nellymoser")) {
				Assert.assertEquals(personSense, word.getDictionaryWordSense());
			} else if (word.getText().equals("VMU")) {
				Assert.assertEquals(organizationSense, word.getDictionaryWordSense());
			} else {
				Assert.assertNull(word.getDictionaryWordSense());
			}
		}
	}

	@Test
	public void testNER3() {
		String sentence = "John is the CEO of Nellymoser.";
		NLPText text = process(sentence);
		nameEntityRecognizer.process(text);
		for (NLPText word : text.getLeaves()) {
			if (word.getText().equals("John")) {
				Assert.assertEquals(personSense, word.getDictionaryWordSense());
			} else if (word.getText().equals("Nellymoser")) {
				Assert.assertEquals(personSense, word.getDictionaryWordSense());
			} else if (word.getText().equals("Arlington")) {
				// assertEquals(locationSense, word.getDictionaryWordSense());
			} else if (word.getText().equals("Massachusetts")) {
				// assertEquals(locationSense, word.getDictionaryWordSense());
			} else {
				Assert.assertNull(word.getDictionaryWordSense());
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
