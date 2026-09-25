/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.nlp.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.rreganjr.nlp.dictionary.GrammaticalStructureLevel;
import com.rreganjr.nlp.dictionary.NLPText;
import com.rreganjr.nlp.dictionary.impl.NLPTextImpl;

import opennlp.tools.ml.maxent.GISModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerFactory;

/**
 * Issue #314: opennlp-tools 1.8.2 to 2.5.9. The bundled models that go through the 1.5+ model
 * constructors don't load on either version, so the bar is "no worse than 1.8.2". These tests pin
 * the parts of the OpenNLP code that do run: the sentence splitter, the legacy GIS model reader,
 * and the POS tag format.
 * <p>
 * In {@code com.rreganjr.nlp.impl} so it can reach {@link AbstractOpenNLPTool}'s protected statics.
 */
class OpenNLPUpgradeTest {

	private static final String TAG_MODEL = OpenNLPTagger.PROP_POSTAGGER_MODEL_FILE_DEFAULT;

	private static List<String> sentences(String text) {
		NLPText nlpText = new NLPTextImpl(text);
		new Sentencizer().process(nlpText);
		return nlpText.getChildren().stream().map(NLPText::getText).collect(Collectors.toList());
	}

	/** Failed before #314: every sentence after the first was cut at the wrong offset. */
	@Test
	void sentencizerSplitsOnSpanBoundaries() {
		assertEquals(List.of("The user logs in.", "Then the user files a report."),
				sentences("The user logs in. Then the user files a report."));
	}

	@Test
	void sentencizerKeepsASingleSentenceWhole() {
		NLPText nlpText = new NLPTextImpl("The user files a report.");
		new Sentencizer().process(nlpText);
		assertEquals(GrammaticalStructureLevel.SENTENCE, nlpText.getGrammaticalStructureLevel());
		assertTrue(nlpText.getChildren().isEmpty());
	}

	@Test
	void sentencizerKeepsTrailingTextWithoutATerminator() {
		assertEquals(List.of("Users log in.", "Then"), sentences("Users log in. Then"));
	}

	/** Exercises AbstractModelReader, the class GHSA-659w-93r5-9j6m was fixed in, on 2.x. */
	@Test
	void readGISModelLoadsParserBuildAndCheckModels() throws Exception {
		GISModel build = AbstractOpenNLPTool.readGISModel(OpenNLPParser.PROP_PARSER_BUILD_MODEL_FILE_DEFAULT);
		GISModel check = AbstractOpenNLPTool.readGISModel(OpenNLPParser.PROP_PARSER_CHECK_MODEL_FILE_DEFAULT);
		assertNotNull(build);
		assertNotNull(check);
		assertEquals(50, build.getNumOutcomes());
		assertEquals(2, check.getNumOutcomes());
	}

	/**
	 * OpenNLP 2.x taggers emit Universal Dependencies tags unless told otherwise; ParseTag.tagOf
	 * expects Penn. The bundled tag model can't be read by POSModel(InputStream), so this wraps the
	 * raw GIS model instead, which is enough to check the format.
	 */
	@Test
	void posTaggerUsesPennTags() throws Exception {
		POSModel model = new POSModel("en", AbstractOpenNLPTool.readGISModel(TAG_MODEL), new HashMap<>(),
				new POSTaggerFactory());
		String[] tags = AbstractOpenNLPTool.newPosTagger(model)
				.tag(new String[] { "The", "user", "files", "a", "report", "." });
		assertEquals("DT", tags[0]);
		assertEquals("NN", tags[1]);
		Set<String> universal = Set.of("DET", "NOUN", "VERB", "ADP", "PUNCT");
		for (String tag : tags) {
			assertFalse(universal.contains(tag), "Universal Dependencies tag " + tag);
		}
	}
}
