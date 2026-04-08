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
import com.rreganjr.nlp.dictionary.impl.NLPTextImpl;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Test the Lemmatizer
 * 
 * @author ron
 */
@Disabled("Lemmatizer relies on legacy models and is currently broken; skipping until NLP stack is refreshed.")

public class LemmatizerTests extends AbstractIntegrationTestCase {

	@Test
	public void testLemmatize() {
		NLPProcessor<NLPText> lemmatizer = getNlpProcessorFactory().getLemmatizer();
		// standard suffix replacing tests
		assertEquals("running", lemmatizer.process(new NLPTextImpl("running", PartOfSpeech.ADJECTIVE)).getLemma());
		assertEquals("running", lemmatizer.process(new NLPTextImpl("running", PartOfSpeech.NOUN)).getLemma());
		assertEquals("run", lemmatizer.process(new NLPTextImpl("running", PartOfSpeech.VERB)).getLemma());
		assertEquals("ring", lemmatizer.process(new NLPTextImpl("ringing", PartOfSpeech.VERB)).getLemma());
		assertEquals("ring", lemmatizer.process(new NLPTextImpl("rings", PartOfSpeech.VERB)).getLemma());
		assertEquals("be", lemmatizer.process(new NLPTextImpl("being", PartOfSpeech.VERB)).getLemma());
		assertEquals("ring", lemmatizer.process(new NLPTextImpl("rang", PartOfSpeech.VERB)).getLemma());

		// special cases
		assertEquals("run", lemmatizer.process(new NLPTextImpl("ran", PartOfSpeech.VERB)).getLemma());
		assertEquals("be", lemmatizer.process(new NLPTextImpl("been", PartOfSpeech.VERB)).getLemma());
		assertEquals("be", lemmatizer.process(new NLPTextImpl("am", PartOfSpeech.VERB)).getLemma());
		assertEquals("be", lemmatizer.process(new NLPTextImpl("is", PartOfSpeech.VERB)).getLemma());
		assertEquals("be", lemmatizer.process(new NLPTextImpl("are", PartOfSpeech.VERB)).getLemma());
		assertEquals("be", lemmatizer.process(new NLPTextImpl("was", PartOfSpeech.VERB)).getLemma());
		assertEquals("be", lemmatizer.process(new NLPTextImpl("were", PartOfSpeech.VERB)).getLemma());
	}

}
