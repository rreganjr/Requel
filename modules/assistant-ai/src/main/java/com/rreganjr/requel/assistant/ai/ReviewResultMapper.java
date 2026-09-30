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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.assistant.api.AssistantMessage;

/**
 * The one implementation of the provider-neutral review contract ({@code summary} /
 * {@code findings} / {@code warnings}): parsing a JSON reply, Requel-side validation, and mapping
 * to {@link AiAnalysisResponse}. Every {@link AiAnalysisClient} that talks to a real model uses it,
 * so the clients cannot drift into two readings of the same contract (#259).
 *
 * <p>
 * Public because the clients live in subpackages ({@code .spring}, {@code .cli}).
 */
public final class ReviewResultMapper {

	private final ObjectMapper objectMapper;

	public ReviewResultMapper(ObjectMapper objectMapper) {
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
	}

	/**
	 * Parse a model's JSON reply into a {@link ReviewResult}. A surrounding Markdown code fence is
	 * stripped first; unknown properties are ignored.
	 *
	 * @throws AiAnalysisException if the reply is blank or not a JSON object of the expected shape
	 */
	public ReviewResult parse(String reply) throws AiAnalysisException {
		String json = stripCodeFence(reply);
		if (json == null || json.isBlank()) {
			throw new AiAnalysisException("model reply was empty");
		}
		try {
			JsonNode node = objectMapper.readTree(json);
			if (node == null || !node.isObject()) {
				throw new AiAnalysisException("model reply is not a JSON object");
			}
			return objectMapper.readerFor(ReviewResult.class)
					.without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
					.readValue(node);
		} catch (JsonProcessingException e) {
			throw new AiAnalysisException("model reply is not valid JSON: " + e.getOriginalMessage(),
					e);
		} catch (java.io.IOException e) {
			throw new AiAnalysisException("model reply could not be read: " + e.getMessage(), e);
		}
	}

	/** Requel-side validation of the structured output, whichever client produced it. */
	public static void validate(ReviewResult result) throws AiAnalysisException {
		if (result == null) {
			throw new AiAnalysisException("structured output was null");
		}
		if (result.summary() == null || result.summary().isBlank()) {
			throw new AiAnalysisException("structured output missing summary");
		}
		if (result.findings() != null) {
			for (ReviewResult.Finding finding : result.findings()) {
				if (finding == null || finding.findingType() == null
						|| finding.findingType().isBlank()) {
					throw new AiAnalysisException("finding missing findingType");
				}
			}
		}
	}

	/**
	 * Map a validated result to the response. Usage and provider metadata are the client's (they
	 * come from its transport); everything derived from the reply itself is built here.
	 */
	public AiAnalysisResponse toResponse(ReviewResult result, AiUsage usage,
			Map<String, Object> providerMetadata) {
		JsonNode structuredOutput = objectMapper.valueToTree(result);
		return new AiAnalysisResponse(result.summary(), structuredOutput, findings(result),
				messages(result), usage, providerMetadata);
	}

	/**
	 * Strip one surrounding Markdown code fence (```` ``` ```` or ```` ```json ````) if present;
	 * otherwise return the trimmed text. {@code null} stays {@code null}.
	 */
	static String stripCodeFence(String reply) {
		if (reply == null) {
			return null;
		}
		String text = reply.strip();
		if (!text.startsWith("```")) {
			return text;
		}
		int firstNewline = text.indexOf('\n');
		if (firstNewline < 0) {
			return text;
		}
		String body = text.substring(firstNewline + 1);
		int closing = body.lastIndexOf("```");
		if (closing >= 0) {
			body = body.substring(0, closing);
		}
		return body.strip();
	}

	private static List<AiFindingDraft> findings(ReviewResult result) {
		List<AiFindingDraft> findings = new ArrayList<AiFindingDraft>();
		if (result.findings() == null) {
			return findings;
		}
		for (ReviewResult.Finding finding : result.findings()) {
			findings.add(new AiFindingDraft(
					finding.findingType(),
					finding.severity(),
					finding.confidence(),
					orEmpty(finding.evidenceReferences()),
					finding.suggestedIssueText(),
					finding.suggestedNoteText(),
					orEmpty(finding.suggestedPositions()),
					Map.of()));
		}
		return findings;
	}

	private static List<AssistantMessage> messages(ReviewResult result) {
		if (result.warnings() == null || result.warnings().isEmpty()) {
			return List.of();
		}
		List<AssistantMessage> messages = new ArrayList<AssistantMessage>();
		for (String warning : result.warnings()) {
			if (warning != null && !warning.isBlank()) {
				messages.add(AssistantMessage.warning(warning));
			}
		}
		return messages;
	}

	private static List<String> orEmpty(List<String> values) {
		return values == null ? List.of() : values;
	}

	/**
	 * The provider-neutral output ({@code summary} / {@code findings} / {@code warnings}). Spring
	 * AI binds the model reply to this record; the CLI client parses into it.
	 */
	public record ReviewResult(String summary, List<Finding> findings, List<String> warnings) {
		public record Finding(String findingType, String severity, Double confidence,
				List<String> evidenceReferences, String suggestedIssueText, String suggestedNoteText,
				List<String> suggestedPositions) {
		}
	}
}
