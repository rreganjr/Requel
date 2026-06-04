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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Generic OpenAI-<em>compatible</em> Chat Completions implementation of {@link AiAnalysisClient}.
 *
 * <p>
 * Talks to any server that implements {@code POST /v1/chat/completions} — cloud OpenAI/Azure and
 * self-hosted servers such as Ollama, LM Studio, vLLM, and LocalAI. Point it at the server with
 * {@code requel.ai.endpoint}.
 *
 * <p>
 * Because structured-output support varies across these servers, the request format is chosen by
 * {@code requel.ai.structuredOutputMode}:
 * <ul>
 * <li>{@code json_schema} — strict schema via {@code response_format.json_schema} (cloud OpenAI,
 * LM Studio, newer servers);</li>
 * <li>{@code json_object} — broadly supported JSON mode (the default);</li>
 * <li>{@code none} — no {@code response_format} at all (minimal servers, e.g. Ollama's
 * compatibility endpoint).</li>
 * </ul>
 * In every mode the output JSON schema is also embedded in the system prompt, so the reply is
 * schema-conforming JSON regardless of how much the server enforces. The result is validated
 * against Requel's provider-neutral contract before use.
 *
 * <p>
 * Selected by {@code requel.ai.provider=openai-compat}; mutually exclusive with the other clients.
 */
@Component
@ConditionalOnProperty(prefix = "requel.ai", name = "provider", havingValue = "openai-compat")
public class OpenAiCompatibleAnalysisClient implements AiAnalysisClient {

	private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleAnalysisClient.class);

	private static final String PROVIDER = "openai-compat";

	private static final String DEFAULT_GUIDANCE =
			"You are a Requel requirements analysis assistant. Analyze the target requirement and "
					+ "report quality problems. Findings are drafts for reviewable Requel "
					+ "annotations; do not invent commands that directly mutate project data.";

	/** Default Chat Completions endpoint (cloud OpenAI). Local servers override via endpoint. */
	static final String DEFAULT_ENDPOINT = "https://api.openai.com/v1/chat/completions";

	private final AiProperties properties;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;
	private final Clock clock;

	@Autowired
	public OpenAiCompatibleAnalysisClient(AiProperties properties, ObjectMapper objectMapper) {
		this(properties, objectMapper, HttpClient.newBuilder()
				.connectTimeout(properties.getTimeout())
				.build(), Clock.systemUTC());
	}

	OpenAiCompatibleAnalysisClient(AiProperties properties, ObjectMapper objectMapper,
			HttpClient httpClient, Clock clock) {
		this.properties = Objects.requireNonNull(properties, "properties");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
		this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	@Override
	public AiAnalysisResponse analyze(AiAnalysisRequest request) throws AiAnalysisException {
		Objects.requireNonNull(request, "request");
		// A key is optional for local servers; send a bearer header only when one is configured.
		String apiKey = resolveApiKey();
		Instant startedAt = clock.instant();
		JsonNode payload = requestPayload(request);
		HttpResponse<String> httpResponse = sendWithRetries(payload, apiKey);
		JsonNode responseNode = readResponse(httpResponse.body());
		JsonNode structuredOutput = extractStructuredOutput(responseNode);
		validateStructuredOutput(structuredOutput);
		if (log.isDebugEnabled()) {
			log.debug("openai-compat structured output for run {} ({} findings): {}",
					request.runId(), structuredOutput.path("findings").size(), structuredOutput);
		}
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

		ArrayNode messages = root.putArray("messages");
		messages.addObject().put("role", "system").put("content", instructions(request));
		messages.addObject().put("role", "user").put("content", prompt(request));

		applyResponseFormat(root, request);
		return root;
	}

	/** Sets {@code response_format} per the configured mode; {@code none} omits it entirely. */
	private void applyResponseFormat(ObjectNode root, AiAnalysisRequest request) {
		String mode = structuredOutputMode();
		if ("json_schema".equals(mode)) {
			ObjectNode responseFormat = root.putObject("response_format");
			responseFormat.put("type", "json_schema");
			ObjectNode jsonSchema = responseFormat.putObject("json_schema");
			jsonSchema.put("name", request.outputSchemaName());
			jsonSchema.put("strict", true);
			jsonSchema.set("schema", request.outputSchema());
		} else if ("json_object".equals(mode)) {
			root.putObject("response_format").put("type", "json_object");
		}
		// "none": no response_format; the prompt still demands schema-conforming JSON.
	}

	private String structuredOutputMode() {
		String mode = properties.getStructuredOutputMode();
		return mode == null ? "json_object" : mode.trim().toLowerCase();
	}

	private String instructions(AiAnalysisRequest request) {
		String schema;
		try {
			schema = objectMapper.writerWithDefaultPrettyPrinter()
					.writeValueAsString(request.outputSchema());
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Could not serialize AI output schema", e);
		}
		String guidance = request.instructions() != null && !request.instructions().isBlank()
				? request.instructions()
				: DEFAULT_GUIDANCE;
		// Compat servers don't all enforce a schema, so always reinforce the JSON-only contract.
		return guidance
				+ "\n\nTask type: " + request.taskType()
				+ "\nLocale: " + request.locale().toLanguageTag()
				+ "\n\nRespond with ONLY a single JSON object (no prose, no markdown code fences) "
				+ "that conforms to this JSON Schema:\n" + schema;
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
			throw new AiAnalysisException("Could not serialize chat-completions request", e);
		}
		int maxAttempts = Math.max(1, properties.getMaxRetries() + 1);
		AiAnalysisException lastFailure = null;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(resolveEndpoint()))
						.timeout(properties.getTimeout())
						.header("Content-Type", "application/json")
						.POST(HttpRequest.BodyPublishers.ofString(body));
				if (apiKey != null && !apiKey.isBlank()) {
					builder.header("Authorization", "Bearer " + apiKey);
				}
				HttpResponse<String> response = httpClient.send(builder.build(),
						HttpResponse.BodyHandlers.ofString());
				if (response.statusCode() >= 200 && response.statusCode() < 300) {
					return response;
				}
				lastFailure = new AiAnalysisException("Chat-completions request failed with HTTP "
						+ response.statusCode() + ": " + response.body());
				if (!isRetryable(response.statusCode()) || attempt == maxAttempts) {
					throw lastFailure;
				}
			} catch (IOException e) {
				lastFailure = new AiAnalysisException("Chat-completions request failed", e);
				if (attempt == maxAttempts) {
					throw lastFailure;
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new AiAnalysisException("Chat-completions request interrupted", e);
			}
		}
		throw lastFailure == null ? new AiAnalysisException("Chat-completions request failed")
				: lastFailure;
	}

	private static boolean isRetryable(int statusCode) {
		return statusCode == 429 || statusCode >= 500;
	}

	private JsonNode readResponse(String body) throws AiAnalysisException {
		try {
			return objectMapper.readTree(body);
		} catch (JsonProcessingException e) {
			throw new AiAnalysisException("Chat-completions response was not JSON", e);
		}
	}

	private JsonNode extractStructuredOutput(JsonNode response) throws AiAnalysisException {
		JsonNode error = response.path("error");
		if (error.isObject() && !error.isEmpty()) {
			throw new AiAnalysisException("Chat-completions response error: "
					+ error.path("message").asText(error.toString()));
		}
		JsonNode message = response.path("choices").path(0).path("message");
		JsonNode content = message.path("content");
		if (!content.isTextual()) {
			throw new AiAnalysisException(
					"Chat-completions response did not contain a message content string");
		}
		return parseStructuredText(content.asText());
	}

	/** Parses the model content as JSON, tolerating ```json fences some servers add. */
	private JsonNode parseStructuredText(String text) throws AiAnalysisException {
		String trimmed = stripCodeFence(text);
		try {
			return objectMapper.readTree(trimmed);
		} catch (JsonProcessingException e) {
			throw new AiAnalysisException("Chat-completions content was not structured JSON", e);
		}
	}

	private static String stripCodeFence(String text) {
		String trimmed = text.strip();
		if (trimmed.startsWith("```")) {
			int firstNewline = trimmed.indexOf('\n');
			if (firstNewline >= 0) {
				trimmed = trimmed.substring(firstNewline + 1);
			}
			if (trimmed.endsWith("```")) {
				trimmed = trimmed.substring(0, trimmed.length() - 3);
			}
		}
		return trimmed.strip();
	}

	private static void validateStructuredOutput(JsonNode structuredOutput)
			throws AiAnalysisException {
		if (!structuredOutput.isObject()) {
			throw new AiAnalysisException("Chat-completions structured output must be a JSON object");
		}
		if (!structuredOutput.path("summary").isTextual()) {
			throw new AiAnalysisException("Chat-completions structured output missing summary");
		}
		if (!structuredOutput.path("findings").isArray()) {
			throw new AiAnalysisException("Chat-completions structured output missing findings");
		}
		for (JsonNode finding : structuredOutput.path("findings")) {
			if (!finding.path("findingType").isTextual()) {
				throw new AiAnalysisException("Chat-completions finding missing findingType");
			}
		}
	}

	private AiUsage usage(JsonNode response, Duration latency) {
		JsonNode usage = response.path("usage");
		Integer inputTokens = integerOrNull(usage, "prompt_tokens");
		Integer outputTokens = integerOrNull(usage, "completion_tokens");
		Integer cachedInputTokens = integerOrNull(usage.path("prompt_tokens_details"),
				"cached_tokens");
		return new AiUsage(PROVIDER, response.path("model").asText(properties.getModel()),
				inputTokens, outputTokens, cachedInputTokens, latency, null);
	}

	private Map<String, Object> providerMetadata(JsonNode response,
			HttpResponse<String> httpResponse) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("provider", PROVIDER);
		metadata.put("httpStatus", httpResponse.statusCode());
		metadata.put("structuredOutputMode", structuredOutputMode());
		putIfPresent(metadata, "responseId", response.path("id"));
		putIfPresent(metadata, "model", response.path("model"));
		putIfPresent(metadata, "finishReason",
				response.path("choices").path(0).path("finish_reason"));
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
