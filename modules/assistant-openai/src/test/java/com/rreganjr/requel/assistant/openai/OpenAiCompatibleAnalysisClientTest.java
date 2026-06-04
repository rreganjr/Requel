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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rreganjr.requel.assistant.ai.AiAnalysisException;
import com.rreganjr.requel.assistant.ai.AiAnalysisRequest;
import com.rreganjr.requel.assistant.ai.AiAnalysisResponse;
import com.rreganjr.requel.assistant.ai.AiFindingDraft;
import com.rreganjr.requel.assistant.ai.AiProperties;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class OpenAiCompatibleAnalysisClientTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private HttpServer server;

	@AfterEach
	void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void defaultJsonObjectModePostsChatCompletionsAndParsesFindings() throws Exception {
		AtomicReference<JsonNode> postedBody = new AtomicReference<JsonNode>();
		startServer(exchange -> {
			postedBody.set(objectMapper.readTree(exchange.getRequestBody()));
			assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo(
					"Bearer local-key");
			respond(exchange, 200, chatResponse(objectMapper.writeValueAsString(structuredOutput())));
		});
		OpenAiCompatibleAnalysisClient client = new OpenAiCompatibleAnalysisClient(
				properties(serverEndpoint()), objectMapper,
				java.net.http.HttpClient.newHttpClient(), fixedClock());

		AiAnalysisResponse response = client.analyze(request());

		JsonNode body = postedBody.get();
		assertThat(body.path("model").asText()).isEqualTo("local-model");
		assertThat(body.path("messages").get(0).path("role").asText()).isEqualTo("system");
		assertThat(body.path("messages").get(0).path("content").asText())
				.contains("JSON Schema", "findings");
		assertThat(body.path("messages").get(1).path("role").asText()).isEqualTo("user");
		assertThat(body.path("messages").get(1).path("content").asText())
				.contains("REQUIREMENTS_REVIEW", "contextPacks");
		// default mode is json_object
		assertThat(body.path("response_format").path("type").asText()).isEqualTo("json_object");

		assertThat(response.summary()).isEqualTo("Review found one issue.");
		assertThat(response.findings()).hasSize(1);
		AiFindingDraft finding = response.findings().get(0);
		assertThat(finding.findingType()).isEqualTo("ambiguous-language");
		assertThat(finding.severity()).isEqualTo("MEDIUM");
		assertThat(response.usage().provider()).isEqualTo("openai-compat");
		assertThat(response.usage().inputTokens()).isEqualTo(31);
		assertThat(response.usage().outputTokens()).isEqualTo(12);
		assertThat(response.providerMetadata()).containsEntry("structuredOutputMode", "json_object");
	}

	@Test
	void usesCentralizedInstructionsInSystemMessageWhenProvided() throws Exception {
		AtomicReference<JsonNode> postedBody = new AtomicReference<JsonNode>();
		startServer(exchange -> {
			postedBody.set(objectMapper.readTree(exchange.getRequestBody()));
			respond(exchange, 200, chatResponse(objectMapper.writeValueAsString(structuredOutput())));
		});
		OpenAiCompatibleAnalysisClient client = new OpenAiCompatibleAnalysisClient(
				properties(serverEndpoint()), objectMapper,
				java.net.http.HttpClient.newHttpClient(), fixedClock());

		client.analyze(requestWithInstructions("REVIEW RULES: do original analysis only."));

		assertThat(postedBody.get().path("messages").get(0).path("content").asText())
				.contains("REVIEW RULES: do original analysis only.");
	}

	@Test
	void jsonSchemaModeSendsStrictSchema() throws Exception {
		AtomicReference<JsonNode> postedBody = new AtomicReference<JsonNode>();
		startServer(exchange -> {
			postedBody.set(objectMapper.readTree(exchange.getRequestBody()));
			respond(exchange, 200, chatResponse(objectMapper.writeValueAsString(structuredOutput())));
		});
		AiProperties properties = properties(serverEndpoint());
		properties.setStructuredOutputMode("json_schema");
		OpenAiCompatibleAnalysisClient client = new OpenAiCompatibleAnalysisClient(properties,
				objectMapper, java.net.http.HttpClient.newHttpClient(), fixedClock());

		client.analyze(request());

		JsonNode responseFormat = postedBody.get().path("response_format");
		assertThat(responseFormat.path("type").asText()).isEqualTo("json_schema");
		assertThat(responseFormat.path("json_schema").path("strict").asBoolean()).isTrue();
		assertThat(responseFormat.path("json_schema").path("schema").path("required").get(0)
				.asText()).isEqualTo("summary");
	}

	@Test
	void noneModeOmitsResponseFormatAndToleratesCodeFence() throws Exception {
		AtomicReference<JsonNode> postedBody = new AtomicReference<JsonNode>();
		startServer(exchange -> {
			postedBody.set(objectMapper.readTree(exchange.getRequestBody()));
			String fenced = "```json\n" + objectMapper.writeValueAsString(structuredOutput())
					+ "\n```";
			respond(exchange, 200, chatResponse(fenced));
		});
		AiProperties properties = properties(serverEndpoint());
		properties.setStructuredOutputMode("none");
		OpenAiCompatibleAnalysisClient client = new OpenAiCompatibleAnalysisClient(properties,
				objectMapper, java.net.http.HttpClient.newHttpClient(), fixedClock());

		AiAnalysisResponse response = client.analyze(request());

		assertThat(postedBody.get().has("response_format")).isFalse();
		assertThat(response.summary()).isEqualTo("Review found one issue.");
	}

	@Test
	void omitsAuthorizationHeaderWhenNoKeyConfigured() throws Exception {
		AtomicReference<Boolean> hadAuth = new AtomicReference<Boolean>();
		startServer(exchange -> {
			hadAuth.set(exchange.getRequestHeaders().containsKey("Authorization"));
			respond(exchange, 200, chatResponse(objectMapper.writeValueAsString(structuredOutput())));
		});
		AiProperties properties = properties(serverEndpoint());
		properties.setApiKey(null);
		properties.setApiKeyEnvironmentVariable("");
		OpenAiCompatibleAnalysisClient client = new OpenAiCompatibleAnalysisClient(properties,
				objectMapper, java.net.http.HttpClient.newHttpClient(), fixedClock());

		client.analyze(request());

		assertThat(hadAuth.get()).isFalse();
	}

	@Test
	void retriesRetryableHttpFailures() throws Exception {
		AtomicInteger attempts = new AtomicInteger();
		startServer(exchange -> {
			if (attempts.incrementAndGet() == 1) {
				respond(exchange, 503, "{\"error\":{\"message\":\"loading model\"}}");
				return;
			}
			respond(exchange, 200, chatResponse(objectMapper.writeValueAsString(structuredOutput())));
		});
		AiProperties properties = properties(serverEndpoint());
		properties.setMaxRetries(1);
		OpenAiCompatibleAnalysisClient client = new OpenAiCompatibleAnalysisClient(properties,
				objectMapper, java.net.http.HttpClient.newHttpClient(), fixedClock());

		AiAnalysisResponse response = client.analyze(request());

		assertThat(response.summary()).isEqualTo("Review found one issue.");
		assertThat(attempts.get()).isEqualTo(2);
	}

	@Test
	void rejectsNonJsonContent() throws Exception {
		startServer(exchange -> respond(exchange, 200, chatResponse("not json at all")));
		OpenAiCompatibleAnalysisClient client = new OpenAiCompatibleAnalysisClient(
				properties(serverEndpoint()), objectMapper,
				java.net.http.HttpClient.newHttpClient(), fixedClock());

		assertThatThrownBy(() -> client.analyze(request()))
				.isInstanceOf(AiAnalysisException.class)
				.hasMessageContaining("structured JSON");
	}

	private void startServer(ExchangeHandler handler) throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/v1/chat/completions", exchange -> {
			try {
				handler.handle(exchange);
			} catch (Throwable t) {
				respond(exchange, 500, "{\"error\":{\"message\":\"" + t.getMessage() + "\"}}");
			}
		});
		server.start();
	}

	private String serverEndpoint() {
		return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions";
	}

	private void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(statusCode, bytes.length);
		try (OutputStream outputStream = exchange.getResponseBody()) {
			outputStream.write(bytes);
		}
	}

	private String chatResponse(String messageContent) throws Exception {
		ObjectNode root = objectMapper.createObjectNode();
		root.put("id", "chatcmpl_test");
		root.put("model", "local-model");
		ObjectNode choice = root.putArray("choices").addObject();
		choice.put("index", 0);
		choice.put("finish_reason", "stop");
		choice.putObject("message").put("role", "assistant").put("content", messageContent);
		ObjectNode usage = root.putObject("usage");
		usage.put("prompt_tokens", 31);
		usage.put("completion_tokens", 12);
		return objectMapper.writeValueAsString(root);
	}

	private ObjectNode structuredOutput() {
		ObjectNode root = objectMapper.createObjectNode();
		root.put("summary", "Review found one issue.");
		ObjectNode finding = root.putArray("findings").addObject();
		finding.put("findingType", "ambiguous-language");
		finding.put("severity", "MEDIUM");
		finding.put("confidence", 0.7);
		finding.putArray("evidenceReferences").add("target.text");
		finding.put("suggestedIssueText", "The requirement uses ambiguous wording.");
		finding.putArray("suggestedPositions").add("Clarify the measurable outcome.");
		return root;
	}

	private AiProperties properties(String endpoint) {
		AiProperties properties = new AiProperties();
		properties.setProvider("openai-compat");
		properties.setModel("local-model");
		properties.setApiKey("local-key");
		properties.setEndpoint(endpoint);
		properties.setMaxOutputTokens(500);
		return properties;
	}

	private AiAnalysisRequest request() {
		return new AiAnalysisRequest("ai-requirements-review", UUID.randomUUID(),
				"REQUIREMENTS_REVIEW", EntityRef.of("Goal", 1L), EntityRef.of("Project", 2L),
				Locale.US, List.of(Map.of("kind", "target", "text", "The system should be fast.")),
				"RequirementsReviewOutput", "1", schema(), Map.of("externalProviderAllowed", true),
				Map.of("templateId", "requirements-review"));
	}

	private AiAnalysisRequest requestWithInstructions(String instructions) {
		return new AiAnalysisRequest("ai-requirements-review", UUID.randomUUID(),
				"REQUIREMENTS_REVIEW", EntityRef.of("Goal", 1L), EntityRef.of("Project", 2L),
				Locale.US, List.of(Map.of("kind", "target", "text", "The system should be fast.")),
				"RequirementsReviewOutput", "1", schema(), Map.of("externalProviderAllowed", true),
				Map.of("templateId", "requirements-review"), instructions);
	}

	private JsonNode schema() {
		ObjectNode schema = objectMapper.createObjectNode();
		schema.put("type", "object");
		schema.putArray("required").add("summary").add("findings");
		ObjectNode properties = schema.putObject("properties");
		properties.putObject("summary").put("type", "string");
		properties.putObject("findings").put("type", "array");
		return schema;
	}

	private Clock fixedClock() {
		return Clock.fixed(Instant.parse("2026-05-29T00:00:00Z"), ZoneOffset.UTC);
	}

	@FunctionalInterface
	private interface ExchangeHandler {
		void handle(HttpExchange exchange) throws Exception;
	}
}
