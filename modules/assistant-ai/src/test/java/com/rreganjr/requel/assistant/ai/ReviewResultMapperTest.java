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
package com.rreganjr.requel.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.assistant.ai.ReviewResultMapper.ReviewResult;
import com.rreganjr.requel.assistant.ai.ReviewResultMapper.ReviewResult.Finding;

/**
 * The shared review contract (#259): validation (moved from {@code SpringAiAnalysisClientTest}),
 * JSON parsing for clients without a structured-output converter, and mapping.
 */
class ReviewResultMapperTest {

	private final ReviewResultMapper mapper = new ReviewResultMapper(new ObjectMapper());

	@Test
	void validateAcceptsAWellFormedResult() throws AiAnalysisException {
		ReviewResult result = new ReviewResult("ok",
				List.of(new Finding("clarity", "MEDIUM", 0.8, List.of("g1"), "issue", "note",
						List.of("pos"))),
				List.of());
		// no exception
		ReviewResultMapper.validate(result);
	}

	@Test
	void validateRejectsMissingSummary() {
		ReviewResult result = new ReviewResult("  ", List.of(), List.of());
		assertThatThrownBy(() -> ReviewResultMapper.validate(result))
				.isInstanceOf(AiAnalysisException.class)
				.hasMessageContaining("summary");
	}

	@Test
	void validateRejectsBlankFindingType() {
		ReviewResult result = new ReviewResult("ok",
				List.of(new Finding("  ", "LOW", null, List.of(), null, null, List.of())),
				List.of());
		assertThatThrownBy(() -> ReviewResultMapper.validate(result))
				.isInstanceOf(AiAnalysisException.class)
				.hasMessageContaining("findingType");
	}

	@Test
	void validateRejectsNull() {
		assertThatThrownBy(() -> ReviewResultMapper.validate(null))
				.isInstanceOf(AiAnalysisException.class);
	}

	@Test
	void parseReadsAPlainJsonReply() throws AiAnalysisException {
		ReviewResult result = mapper.parse("""
				{"summary":"s","findings":[{"findingType":"AMBIGUOUS","severity":"HIGH",
				 "confidence":0.5,"evidenceReferences":["fast"],"suggestedIssueText":"i",
				 "suggestedNoteText":null,"suggestedPositions":["p"]}],"warnings":["w"]}
				""");
		assertThat(result.summary()).isEqualTo("s");
		assertThat(result.findings()).singleElement().satisfies(f -> {
			assertThat(f.findingType()).isEqualTo("AMBIGUOUS");
			assertThat(f.severity()).isEqualTo("HIGH");
			assertThat(f.confidence()).isEqualTo(0.5);
			assertThat(f.suggestedPositions()).containsExactly("p");
		});
		assertThat(result.warnings()).containsExactly("w");
	}

	@Test
	void parseStripsACodeFenceAndIgnoresUnknownProperties() throws AiAnalysisException {
		ReviewResult result = mapper.parse("""
				```json
				{"summary":"fenced","findings":[],"extra":{"ignored":true}}
				```
				""");
		assertThat(result.summary()).isEqualTo("fenced");
		assertThat(result.findings()).isEmpty();
	}

	@Test
	void parseRejectsEmptyNonJsonAndNonObjectReplies() {
		assertThatThrownBy(() -> mapper.parse(null)).isInstanceOf(AiAnalysisException.class);
		assertThatThrownBy(() -> mapper.parse("   ")).isInstanceOf(AiAnalysisException.class);
		assertThatThrownBy(() -> mapper.parse("Here are my findings: none"))
				.isInstanceOf(AiAnalysisException.class);
		assertThatThrownBy(() -> mapper.parse("[1,2,3]"))
				.isInstanceOf(AiAnalysisException.class)
				.hasMessageContaining("JSON object");
	}

	@Test
	void stripCodeFenceLeavesUnfencedTextTrimmed() {
		assertThat(ReviewResultMapper.stripCodeFence("  {\"a\":1}\n")).isEqualTo("{\"a\":1}");
		assertThat(ReviewResultMapper.stripCodeFence("```\n{\"a\":1}\n```")).isEqualTo("{\"a\":1}");
		assertThat(ReviewResultMapper.stripCodeFence(null)).isNull();
	}

	@Test
	void toResponseMapsTheReplyAndKeepsTheClientsUsageAndMetadata() {
		ReviewResult result = new ReviewResult("a summary",
				List.of(new Finding("clarity", null, null, null, "issue", null, null)),
				List.of("watch out", " "));
		AiUsage usage = new AiUsage("cli", "claude-cli", 10, 20, 5, Duration.ofMillis(3), null);

		AiAnalysisResponse response = mapper.toResponse(result, usage, Map.of("provider", "cli"));

		assertThat(response.summary()).isEqualTo("a summary");
		assertThat(response.structuredOutput().path("summary").asText()).isEqualTo("a summary");
		assertThat(response.findings()).singleElement().satisfies(f -> {
			assertThat(f.evidenceReferences()).isEmpty();
			assertThat(f.suggestedPositions()).isEmpty();
			assertThat(f.metadata()).isEmpty();
		});
		assertThat(response.messages()).hasSize(1);
		assertThat(response.usage()).isSameAs(usage);
		assertThat(response.providerMetadata()).containsEntry("provider", "cli");
	}
}
