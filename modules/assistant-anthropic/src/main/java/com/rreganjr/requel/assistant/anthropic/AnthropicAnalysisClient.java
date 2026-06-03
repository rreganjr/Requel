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
package com.rreganjr.requel.assistant.anthropic;

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
 * Anthropic (Claude) Messages API implementation of {@link AiAnalysisClient}.
 *
 * <p>
 * Structured JSON is obtained with a single forced tool: the caller-supplied output schema is
 * registered as the tool's {@code input_schema} and {@code tool_choice} forces Claude to call it,
 * so the model's reply is a {@code tool_use} block whose {@code input} is JSON conforming to the
 * schema. This is the most stable way to get guaranteed-shape JSON from the raw Messages API. The
 * result is validated against Requel's provider-neutral contract before being handed to
 * assistant/application code.
 *
 * <p>
 * Selected by {@code requel.ai.provider=anthropic}; mutually exclusive with the OpenAI and Noop
 * clients (see {@code NoopAiAnalysisClient}).
 */
@Component
@ConditionalOnProperty(prefix = "requel.ai", name = "provider", havingValue = "anthropic")
public class AnthropicAnalysisClient implements AiAnalysisClient {

	private static final String PROVIDER = "anthropic";

	/** Default Messages endpoint used when {@code requel.ai.endpoint} is left blank. */
	static final String DEFAULT_ENDPOINT = "https://api.anthropic.com/v1/messages";

	/** Anthropic requires an explicit API version header. */
	static final String ANTHROPIC_VERSION = "2023-06-01";

	/** Name of the forced tool that carries the structured output. */
	static final String TOOL_NAME = "requel_structured_output";

	private final AiProperties properties;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;
	private final Clock clock;

	@Autowired
	public AnthropicAnalysisClient(AiProperties properties, ObjectMapper objectMapper) {
		this(properties, objectMapper, HttpClient.newBuilder()
				.connectTimeout(properties.getTimeout())
				.build(), Clock.systemUTC());
	}

	AnthropicAnalysisClient(AiProperties properties, ObjectMapper objectMapper,
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
			throw new AiAnalysisException("Anthropic API key is not configured");
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
		root.put("max_tokens", properties.getMaxOutputTokens());
		root.put("system", instructions(request));

		ArrayNode messages = root.putArray("messages");
		ObjectNode message = messages.addObject();
		message.put("role", "user");
		message.put("content", prompt(request));

		ArrayNode tools = root.putArray("tools");
		ObjectNode tool = tools.addObject();
		tool.put("name", TOOL_NAME);
		tool.put("description",
				"Emit the requirements-review result as JSON matching the provided schema ("
						+ request.outputSchemaName() + ").");
		tool.set("input_schema", request.outputSchema());

		ObjectNode toolChoice = root.putObject("tool_choice");
		toolChoice.put("type", "tool");
		toolChoice.put("name", TOOL_NAME);
		return root;
	}

	private String instructions(AiAnalysisRequest request) {
		return """
				You are a Requel requirements analysis assistant. Use the supplied tool to return
				only JSON matching its schema. Findings are drafts for reviewable Requel
				annotations; do not invent commands that directly mutate project data.
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
			throw new AiAnalysisException("Could not serialize Anthropic request", e);
		}
		int maxAttempts = Math.max(1, properties.getMaxRetries() + 1);
		AiAnalysisException lastFailure = null;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(resolveEndpoint()))
						.timeout(properties.getTimeout())
						.header("x-api-key", apiKey)
						.header("anthropic-version", ANTHROPIC_VERSION)
						.header("Content-Type", "application/json")
						.POST(HttpRequest.BodyPublishers.ofString(body))
						.build();
				HttpResponse<String> response = httpClient.send(httpRequest,
						HttpResponse.BodyHandlers.ofString());
				if (response.statusCode() >= 200 && response.statusCode() < 300) {
					return response;
				}
				lastFailure = new AiAnalysisException("Anthropic request failed with HTTP "
						+ response.statusCode() + ": " + response.body());
				if (!isRetryable(response.statusCode()) || attempt == maxAttempts) {
					throw lastFailure;
				}
			} catch (IOException e) {
				lastFailure = new AiAnalysisException("Anthropic request failed", e);
				if (attempt == maxAttempts) {
					throw lastFailure;
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new AiAnalysisException("Anthropic request interrupted", e);
			}
		}
		throw lastFailure == null ? new AiAnalysisException("Anthropic request failed") : lastFailure;
	}

	private static boolean isRetryable(int statusCode) {
		return statusCode == 429 || statusCode >= 500;
	}

	private JsonNode readResponse(String body) throws AiAnalysisException {
		try {
			return objectMapper.readTree(body);
		} catch (JsonProcessingException e) {
			throw new AiAnalysisException("Anthropic response was not JSON", e);
		}
	}

	private JsonNode extractStructuredOutput(JsonNode response) throws AiAnalysisException {
		if ("error".equals(response.path("type").asText())) {
			throw new AiAnalysisException("Anthropic response error: "
					+ response.path("error").path("message").asText(response.toString()));
		}
		for (JsonNode contentItem : response.path("content")) {
			if ("tool_use".equals(contentItem.path("type").asText())
					&& TOOL_NAME.equals(contentItem.path("name").asText())) {
				JsonNode input = contentItem.path("input");
				if (input.isObject()) {
					return input;
				}
			}
		}
		throw new AiAnalysisException(
				"Anthropic response did not contain a " + TOOL_NAME + " tool_use block");
	}

	private static void validateStructuredOutput(JsonNode structuredOutput)
			throws AiAnalysisException {
		if (!structuredOutput.isObject()) {
			throw new AiAnalysisException("Anthropic structured output must be a JSON object");
		}
		if (!structuredOutput.path("summary").isTextual()) {
			throw new AiAnalysisException("Anthropic structured output missing textual summary");
		}
		if (!structuredOutput.path("findings").isArray()) {
			throw new AiAnalysisException("Anthropic structured output missing findings array");
		}
		for (JsonNode finding : structuredOutput.path("findings")) {
			if (!finding.path("findingType").isTextual()) {
				throw new AiAnalysisException("Anthropic structured finding missing findingType");
			}
		}
	}

	private AiUsage usage(JsonNode response, Duration latency) {
		JsonNode usage = response.path("usage");
		Integer inputTokens = integerOrNull(usage, "input_tokens");
		Integer outputTokens = integerOrNull(usage, "output_tokens");
		Integer cachedInputTokens = integerOrNull(usage, "cache_read_input_tokens");
		return new AiUsage(PROVIDER, response.path("model").asText(properties.getModel()),
				inputTokens, outputTokens, cachedInputTokens, latency, null);
	}

	private Map<String, Object> providerMetadata(JsonNode response,
			HttpResponse<String> httpResponse) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("provider", PROVIDER);
		metadata.put("httpStatus", httpResponse.statusCode());
		putIfPresent(metadata, "responseId", response.path("id"));
		putIfPresent(metadata, "stopReason", response.path("stop_reason"));
		putIfPresent(metadata, "model", response.path("model"));
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

	private String resolveEndpoint() {
		String endpoint = properties.getEndpoint();
		return endpoint == null || endpoint.isBlank() ? DEFAULT_ENDPOINT : endpoint;
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
