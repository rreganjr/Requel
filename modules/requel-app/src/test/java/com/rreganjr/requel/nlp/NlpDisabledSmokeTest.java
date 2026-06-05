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
package com.rreganjr.requel.nlp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.rreganjr.nlp.dictionary.NLPProcessorFactory;
import com.rreganjr.nlp.dictionary.NLPText;
import com.rreganjr.requel.Application;

/**
 * NLP-optional Scope 1 smoke test (issue #43, Phase 4.5 Step 7): with
 * {@code requel.nlp.enabled=false} the application context still boots, the real
 * {@code NLPProcessorFactoryImpl} is gated out, and the {@link NoOpNLPProcessorFactory}
 * takes its place returning safe empty values (so assistants degrade to "no findings"
 * rather than NPE-ing).
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "requel.nlp.enabled=false")
public class NlpDisabledSmokeTest {

	@Autowired
	private NLPProcessorFactory nlpProcessorFactory;

	@Test
	void contextLoadsWithNoOpFactoryWhenNlpDisabled() {
		assertThat(nlpProcessorFactory).isInstanceOf(NoOpNLPProcessorFactory.class);
	}

	@Test
	void noOpFactoryReturnsSafeEmptyValues() {
		NLPText text = nlpProcessorFactory.processText("the groal is vaige");
		assertThat(text).isNotNull();
		// Spelling checker reports every word correct -> no spelling findings.
		assertThat(nlpProcessorFactory.getSpellingChecker().process(text)).isTrue();
		// Suggestion / phrase processors return empty collections, never null.
		Collection<NLPText> similar = nlpProcessorFactory.getSimilarWordFinder().process(text);
		assertThat(similar).isEmpty();
		assertThat(nlpProcessorFactory.getMoreSpecificWordSuggester().process(text)).isEmpty();
		assertThat(nlpProcessorFactory.getNounPhraseFinder().process(text)).isEmpty();
		// Complexity depth below any threshold; NLPText-typed processors echo their input.
		int depth = nlpProcessorFactory.getConstituentTreeDepthFinder().process(text);
		assertThat(depth).isZero();
		assertThat(nlpProcessorFactory.getLemmatizer().process(text)).isSameAs(text);
	}
}
