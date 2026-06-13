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
package com.rreganjr.requel.assistant.ai.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.assistant.ai.AiAnalysisException;
import com.rreganjr.requel.assistant.ai.AiAnalysisResponse;
import com.rreganjr.requel.assistant.ai.AiFindingDraft;
import com.rreganjr.requel.assistant.ai.AiProperties;
import com.rreganjr.requel.assistant.api.AssistantMessage;
import com.rreganjr.requel.assistant.ai.spring.SpringAiAnalysisClient.ReviewResult;
import com.rreganjr.requel.assistant.ai.spring.SpringAiAnalysisClient.ReviewResult.Finding;

/**
 * Network-free unit tests for the Requel-side validation and the
 * {@code ReviewResult -> AiAnalysisResponse}/{@code AiFindingDraft} mapping. The Spring AI call
 * itself ({@code chat.prompt()...responseEntity}) is covered separately against a stubbed
 * ChatClient; here we exercise the pure glue.
 */
class SpringAiAnalysisClientTest {

	private final AiProperties properties = properties();
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final SpringAiAnalysisClient client = new SpringAiAnalysisClient(
			mock(ChatClient.class), properties, objectMapper, Clock.systemUTC());

	private static AiProperties properties() {
		AiProperties p = new AiProperties();
		p.setProvider("openai");
		p.setModel("gpt-4o-mini");
		return p;
	}

	@Test
	void validateAcceptsAWellFormedResult() throws AiAnalysisException {
		ReviewResult result = new ReviewResult("ok",
				List.of(new Finding("clarity", "MEDIUM", 0.8, List.of("g1"), "issue", "note",
						List.of("pos"))),
				List.of());
		// no exception
		SpringAiAnalysisClient.validate(result);
	}

	@Test
	void validateRejectsMissingSummary() {
		ReviewResult result = new ReviewResult("  ", List.of(), List.of());
		assertThatThrownBy(() -> SpringAiAnalysisClient.validate(result))
				.isInstanceOf(AiAnalysisException.class)
				.hasMessageContaining("summary");
	}

	@Test
	void validateRejectsBlankFindingType() {
		ReviewResult result = new ReviewResult("ok",
				List.of(new Finding("  ", "LOW", null, List.of(), null, null, List.of())),
				List.of());
		assertThatThrownBy(() -> SpringAiAnalysisClient.validate(result))
				.isInstanceOf(AiAnalysisException.class)
				.hasMessageContaining("findingType");
	}

	@Test
	void toResponseMapsSummaryFindingsAndWarnings() {
		ReviewResult result = new ReviewResult("a summary",
				List.of(new Finding("clarity", "HIGH", 0.9, List.of("g1", "g2"), "issue text",
						"note text", List.of("for", "against"))),
				List.of("watch out", "  "));

		AiAnalysisResponse response = client.toResponse(result, null, Duration.ofMillis(5));

		assertThat(response.summary()).isEqualTo("a summary");
		assertThat(response.structuredOutput().path("summary").asText()).isEqualTo("a summary");

		assertThat(response.findings()).hasSize(1);
		AiFindingDraft finding = response.findings().get(0);
		assertThat(finding.findingType()).isEqualTo("clarity");
		assertThat(finding.severity()).isEqualTo("HIGH");
		assertThat(finding.confidence()).isEqualTo(0.9);
		assertThat(finding.evidenceReferences()).containsExactly("g1", "g2");
		assertThat(finding.suggestedIssueText()).isEqualTo("issue text");
		assertThat(finding.suggestedNoteText()).isEqualTo("note text");
		assertThat(finding.suggestedPositions()).containsExactly("for", "against");

		// blank warnings are dropped
		assertThat(response.messages()).hasSize(1);
		AssistantMessage message = response.messages().get(0);
		assertThat(message.level()).isEqualTo(AssistantMessage.Level.WARNING);
		assertThat(message.text()).isEqualTo("watch out");
	}

	@Test
	void toResponseCoalescesNullListsAndUsageWhenNoChatResponse() {
		ReviewResult result = new ReviewResult("s",
				List.of(new Finding("clarity", null, null, null, null, null, null)),
				null);

		AiAnalysisResponse response = client.toResponse(result, null, Duration.ofMillis(1));

		AiFindingDraft finding = response.findings().get(0);
		assertThat(finding.evidenceReferences()).isEmpty();
		assertThat(finding.suggestedPositions()).isEmpty();
		assertThat(finding.metadata()).isEmpty();
		assertThat(response.messages()).isEmpty();

		// usage falls back to configured provider/model with null token counts when no ChatResponse
		assertThat(response.usage().provider()).isEqualTo("openai");
		assertThat(response.usage().model()).isEqualTo("gpt-4o-mini");
		assertThat(response.usage().inputTokens()).isNull();
		assertThat(response.usage().outputTokens()).isNull();
		assertThat(response.providerMetadata())
				.containsEntry("provider", "openai")
				.containsEntry("model", "gpt-4o-mini");
	}
}
