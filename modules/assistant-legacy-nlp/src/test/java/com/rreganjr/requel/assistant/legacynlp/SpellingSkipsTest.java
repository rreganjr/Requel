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
package com.rreganjr.requel.assistant.legacynlp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

/** Issue #268: the spelling exemptions, each from a token the roundtable harness turned up. */
class SpellingSkipsTest {

	private static final Set<String> DICTIONARY = Set.of("host", "session", "stream", "production",
			"encoding", "entering");
	private static final Predicate<String> KNOWN = DICTIONARY::contains;
	private static final ProjectVocabulary VOCABULARY = ProjectVocabulary.of(
			List.of("Passthrough remux", "Dry-run event"), List.of("Zoom Host"));

	private static String reason(String token) {
		return SpellingSkips.reason(token, VOCABULARY, KNOWN);
	}

	@Test
	void projectVocabularyIsExemptWholeAndWordByWord() {
		assertThat(reason("remux")).isEqualTo("vocabulary");
		assertThat(reason("Passthrough")).isEqualTo("vocabulary");
		assertThat(reason("zoom")).isEqualTo("vocabulary");
		assertThat(reason("dry-run")).isEqualTo("vocabulary");
		assertThat(reason("dry")).isEqualTo("vocabulary");
	}

	@Test
	void acronymsAreExempt() {
		for (String token : List.of("MP4", "RTMP", "AWS", "S3", "IAM", "APIs")) {
			assertThat(reason(token)).as(token).isEqualTo("acronym");
		}
	}

	@Test
	void camelCaseDigitsAndUnderscoresAreExempt() {
		assertThat(reason("CloudWatch")).isEqualTo("camel-case");
		assertThat(reason("MediaConvert")).isEqualTo("camel-case");
		assertThat(reason("DeleteChannel")).isEqualTo("camel-case");
		assertThat(reason("v1")).isEqualTo("digit");
		assertThat(reason("h1")).isEqualTo("digit");
		assertThat(reason("ENTRA_SETUP")).isEqualTo("underscore");
	}

	@Test
	void hyphenatedCompoundsOfKnownWordsAndPrefixesAreExempt() {
		for (String token : List.of("co-hosts", "mid-session", "mid-stream", "non-production",
				"re-encoding", "re-entering")) {
			assertThat(reason(token)).as(token).isEqualTo("hyphen");
		}
	}

	@Test
	void anUnknownPartOrAnOrdinaryUnknownWordIsStillReported() {
		assertThat(reason("co-hsts")).isNull();
		assertThat(reason("-stream")).isNull();
		assertThat(reason("stream-")).isNull();
		assertThat(reason("webinar")).isNull();
		assertThat(reason("Zzz")).isNull();
		assertThat(reason("Microsoft")).isNull();
		assertThat(reason("A")).isNull();
	}

	@Test
	void withoutADictionaryHyphenatedTokensAreNotExempt() {
		assertThat(SpellingSkips.reason("co-hosts", VOCABULARY, null)).isNull();
		assertThat(SpellingSkips.reason("", VOCABULARY, KNOWN)).isNull();
		assertThat(SpellingSkips.reason(null, VOCABULARY, KNOWN)).isNull();
	}
}
