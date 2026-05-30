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
package com.rreganjr.requel.assistant.openai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rreganjr.requel.assistant.ai.AiAnalysisClient;
import com.rreganjr.requel.assistant.ai.AiAnalysisException;
import com.rreganjr.requel.assistant.ai.AiAnalysisRequest;
import com.rreganjr.requel.assistant.ai.AiAnalysisResponse;
import com.rreganjr.requel.assistant.ai.AiFindingDraft;
import com.rreganjr.requel.assistant.ai.AiProperties;
import com.rreganjr.requel.assistant.ai.AiUsage;
import com.rreganjr.requel.assistant.api.AssistantMessage;

/**
 * OpenAI Responses API implementation of {@link AiAnalysisClient}.
 *
 * <p>The client uses OpenAI Structured Outputs through {@code text.format}
 * with a caller-supplied JSON schema. It still validates the returned JSON
 * enough for Requel's provider-neutral contract before handing it to
 * assistant/application code.</p>
 */
@Component
@ConditionalOnProperty(prefix = "requel.ai", name = "provider", havingValue = "openai")
public class OpenAiAnalysisClient implements AiAnalysisClient {

	private static final String PROVIDER = "openai";

	private final AiProperties properties;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;
	private final Clock clock;

	@Autowired
	public OpenAiAnalysisClient(AiProperties properties, ObjectMapper objectMapper) {
		this(properties, objectMapper, HttpClient.newBuilder()
				.connectTimeout(properties.getTimeout())
				.build(), Clock.systemUTC());
	}

	OpenAiAnalysisClient(AiProperties properties, ObjectMapper objectMapper,
			HttpClient httpClient, Clock clock) {
		this.properties = Objects.requireNonNull(properties, "properties");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
		this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	@Override
	public AiAnalysisResponse analyze(AiAnalysisRequest request) throws AiAnalysisException {
		Objects.requireNonNull(request, "request");
		String apiKey = resolveApiKey();
		if (apiKey == null || apiKey.isBlank()) {
			throw new AiAnalysisException("OpenAI API key is not configured");
		}
		Instant startedAt = clock.instant();
		JsonNode payload = requestPayload(request);
		HttpResponse<String> httpResponse = sendWithRetries(payload, apiKey);
		JsonNode responseNode = readResponse(httpResponse.body());
		JsonNode structuredOutput = extractStructuredOutput(responseNode);
		validateStructuredOutput(structuredOutput);
		Duration latency = Duration.between(startedAt, clock.instant());
		AiUsage usage = usage(responseNode, latency);
		Map<String, Object> metadata = providerMetadata(responseNode, httpResponse);
		return new AiAnalysisResponse(summary(structuredOutput), structuredOutput,
				findings(structuredOutput), messages(structuredOutput), usage, metadata);
	}

	private JsonNode requestPayload(AiAnalysisRequest request) {
		ObjectNode root = objectMapper.createObjectNode();
		root.put("model", properties.getModel());
		root.put("instructions", instructions(request));
		root.put("max_output_tokens", properties.getMaxOutputTokens());

		ArrayNode input = root.putArray("input");
		ObjectNode message = input.addObject();
		message.put("role", "user");
		ArrayNode content = message.putArray("content");
		content.addObject()
				.put("type", "input_text")
				.put("text", prompt(request));

		ObjectNode text = root.putObject("text");
		ObjectNode format = text.putObject("format");
		format.put("type", "json_schema");
		format.put("name", request.outputSchemaName());
		format.put("strict", true);
		format.set("schema", request.outputSchema());

		ObjectNode metadata = root.putObject("metadata");
		metadata.put("assistant_id", request.assistantId());
		metadata.put("run_id", request.runId().toString());
		metadata.put("task_type", request.taskType());
		metadata.put("target_type", request.targetRef().entityType());
		metadata.put("target_id", String.valueOf(request.targetRef().entityId()));
		metadata.put("project_id", String.valueOf(request.projectRef().entityId()));
		return root;
	}

	private String instructions(AiAnalysisRequest request) {
		return """
				You are a Requel requirements analysis assistant. Return only JSON matching
				the supplied schema. Findings are drafts for reviewable Requel annotations;
				do not invent commands that directly mutate project data.
				"""
				+ "\nTask type: " + request.taskType()
				+ "\nLocale: " + request.locale().toLanguageTag();
	}

	private String prompt(AiAnalysisRequest request) {
		ObjectNode root = objectMapper.createObjectNode();
		root.put("assistantId", request.assistantId());
		root.put("runId", request.runId().toString());
		root.put("taskType", request.taskType());
		root.set("targetRef", objectMapper.valueToTree(request.targetRef()));
		root.set("projectRef", objectMapper.valueToTree(request.projectRef()));
		root.put("locale", request.locale().toLanguageTag());
		root.set("contextPacks", objectMapper.valueToTree(request.contextPacks()));
		root.put("outputSchemaName", request.outputSchemaName());
		root.put("outputSchemaVersion", request.outputSchemaVersion());
		root.set("dataHandlingFlags", objectMapper.valueToTree(request.dataHandlingFlags()));
		root.set("attributes", objectMapper.valueToTree(request.attributes()));
		root.put("approximateInputTokenBudget", properties.getMaxInputTokens());
		try {
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Could not serialize AI analysis prompt", e);
		}
	}

	private HttpResponse<String> sendWithRetries(JsonNode payload, String apiKey)
			throws AiAnalysisException {
		String body;
		try {
			body = objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new AiAnalysisException("Could not serialize OpenAI request", e);
		}
		int maxAttempts = Math.max(1, properties.getMaxRetries() + 1);
		AiAnalysisException lastFailure = null;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(properties.getEndpoint()))
						.timeout(properties.getTimeout())
						.header("Authorization", "Bearer " + apiKey)
						.header("Content-Type", "application/json")
						.POST(HttpRequest.BodyPublishers.ofString(body))
						.build();
				HttpResponse<String> response = httpClient.send(httpRequest,
						HttpResponse.BodyHandlers.ofString());
				if (response.statusCode() >= 200 && response.statusCode() < 300) {
					return response;
				}
				lastFailure = new AiAnalysisException("OpenAI request failed with HTTP "
						+ response.statusCode() + ": " + response.body());
				if (!isRetryable(response.statusCode()) || attempt == maxAttempts) {
					throw lastFailure;
				}
			} catch (IOException e) {
				lastFailure = new AiAnalysisException("OpenAI request failed", e);
				if (attempt == maxAttempts) {
					throw lastFailure;
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new AiAnalysisException("OpenAI request interrupted", e);
			}
		}
		throw lastFailure == null ? new AiAnalysisException("OpenAI request failed") : lastFailure;
	}

	private static boolean isRetryable(int statusCode) {
		return statusCode == 429 || statusCode >= 500;
	}

	private JsonNode readResponse(String body) throws AiAnalysisException {
		try {
			return objectMapper.readTree(body);
		} catch (JsonProcessingException e) {
			throw new AiAnalysisException("OpenAI response was not JSON", e);
		}
	}

	private JsonNode extractStructuredOutput(JsonNode response) throws AiAnalysisException {
		JsonNode error = response.path("error");
		if (error.isObject() && !error.isEmpty()) {
			throw new AiAnalysisException("OpenAI response error: " + error.path("message")
					.asText(error.toString()));
		}
		String status = response.path("status").asText("completed");
		if (!"completed".equals(status)) {
			throw new AiAnalysisException("OpenAI response was not completed: " + status);
		}
		for (JsonNode outputItem : response.path("output")) {
			for (JsonNode contentItem : outputItem.path("content")) {
				if ("output_text".equals(contentItem.path("type").asText())) {
					return parseStructuredText(contentItem.path("text").asText());
				}
			}
		}
		JsonNode outputText = response.path("output_text");
		if (outputText.isTextual()) {
			return parseStructuredText(outputText.asText());
		}
		throw new AiAnalysisException("OpenAI response did not contain output_text content");
	}

	private JsonNode parseStructuredText(String text) throws AiAnalysisException {
		try {
			return objectMapper.readTree(text);
		} catch (JsonProcessingException e) {
			throw new AiAnalysisException("OpenAI output_text did not contain structured JSON", e);
		}
	}

	private static void validateStructuredOutput(JsonNode structuredOutput)
			throws AiAnalysisException {
		if (!structuredOutput.isObject()) {
			throw new AiAnalysisException("OpenAI structured output must be a JSON object");
		}
		if (!structuredOutput.path("summary").isTextual()) {
			throw new AiAnalysisException("OpenAI structured output missing textual summary");
		}
		if (!structuredOutput.path("findings").isArray()) {
			throw new AiAnalysisException("OpenAI structured output missing findings array");
		}
		for (JsonNode finding : structuredOutput.path("findings")) {
			if (!finding.path("findingType").isTextual()) {
				throw new AiAnalysisException("OpenAI structured finding missing findingType");
			}
		}
	}

	private AiUsage usage(JsonNode response, Duration latency) {
		JsonNode usage = response.path("usage");
		Integer inputTokens = integerOrNull(usage, "input_tokens");
		Integer outputTokens = integerOrNull(usage, "output_tokens");
		Integer cachedInputTokens = integerOrNull(usage.path("input_tokens_details"),
				"cached_tokens");
		return new AiUsage(PROVIDER, response.path("model").asText(properties.getModel()),
				inputTokens, outputTokens, cachedInputTokens, latency, null);
	}

	private Map<String, Object> providerMetadata(JsonNode response,
			HttpResponse<String> httpResponse) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("provider", PROVIDER);
		metadata.put("httpStatus", httpResponse.statusCode());
		putIfPresent(metadata, "responseId", response.path("id"));
		putIfPresent(metadata, "status", response.path("status"));
		putIfPresent(metadata, "model", response.path("model"));
		JsonNode incompleteReason = response.path("incomplete_details").path("reason");
		putIfPresent(metadata, "incompleteReason", incompleteReason);
		return metadata;
	}

	private static void putIfPresent(Map<String, Object> metadata, String key, JsonNode node) {
		if (node != null && !node.isMissingNode() && !node.isNull()) {
			metadata.put(key, node.asText());
		}
	}

	private static Integer integerOrNull(JsonNode node, String fieldName) {
		JsonNode value = node.path(fieldName);
		return value.isInt() || value.isLong() ? value.asInt() : null;
	}

	private static String summary(JsonNode structuredOutput) {
		return structuredOutput.path("summary").asText();
	}

	private List<AiFindingDraft> findings(JsonNode structuredOutput) {
		List<AiFindingDraft> findings = new ArrayList<AiFindingDraft>();
		for (JsonNode finding : structuredOutput.path("findings")) {
			findings.add(new AiFindingDraft(
					finding.path("findingType").asText(),
					textOrNull(finding, "severity"),
					doubleOrNull(finding, "confidence"),
					stringList(finding.path("evidenceReferences")),
					textOrNull(finding, "suggestedIssueText"),
					textOrNull(finding, "suggestedNoteText"),
					stringList(finding.path("suggestedPositions")),
					objectMap(finding.path("metadata"))));
		}
		return findings;
	}

	private static String textOrNull(JsonNode node, String fieldName) {
		JsonNode value = node.path(fieldName);
		return value.isTextual() ? value.asText() : null;
	}

	private static Double doubleOrNull(JsonNode node, String fieldName) {
		JsonNode value = node.path(fieldName);
		return value.isNumber() ? value.asDouble() : null;
	}

	private static List<String> stringList(JsonNode arrayNode) {
		List<String> values = new ArrayList<String>();
		if (arrayNode.isArray()) {
			for (JsonNode value : arrayNode) {
				if (value.isTextual()) {
					values.add(value.asText());
				}
			}
		}
		return values;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> objectMap(JsonNode objectNode) {
		if (!objectNode.isObject()) {
			return Map.of();
		}
		return objectMapper.convertValue(objectNode, Map.class);
	}

	private List<AssistantMessage> messages(JsonNode structuredOutput) {
		JsonNode warnings = structuredOutput.path("warnings");
		if (!warnings.isArray()) {
			return List.of();
		}
		List<AssistantMessage> messages = new ArrayList<AssistantMessage>();
		for (JsonNode warning : warnings) {
			if (warning.isTextual()) {
				messages.add(AssistantMessage.warning(warning.asText()));
			}
		}
		return messages;
	}

	private String resolveApiKey() {
		if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
			return properties.getApiKey();
		}
		String environmentVariable = properties.getApiKeyEnvironmentVariable();
		if (environmentVariable == null || environmentVariable.isBlank()) {
			return null;
		}
		return System.getenv(environmentVariable);
	}
}
