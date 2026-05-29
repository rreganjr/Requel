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
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.AssistantMessage.Level;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class OpenAiAnalysisClientTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private HttpServer server;

	@AfterEach
	void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void postsStructuredResponsesRequestAndParsesFindings() throws Exception {
		AtomicReference<JsonNode> postedBody = new AtomicReference<JsonNode>();
		startServer(exchange -> {
			postedBody.set(objectMapper.readTree(exchange.getRequestBody()));
			assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo(
					"Bearer test-key");
			respond(exchange, 200, openAiResponse(structuredOutput()));
		});
		OpenAiAnalysisClient client = new OpenAiAnalysisClient(properties(serverEndpoint()),
				objectMapper, java.net.http.HttpClient.newHttpClient(), fixedClock());

		AiAnalysisResponse response = client.analyze(request());

		JsonNode requestBody = postedBody.get();
		assertThat(requestBody.path("model").asText()).isEqualTo("gpt-test");
		assertThat(requestBody.path("max_output_tokens").asInt()).isEqualTo(500);
		assertThat(requestBody.path("text").path("format").path("type").asText()).isEqualTo(
				"json_schema");
		assertThat(requestBody.path("text").path("format").path("strict").asBoolean()).isTrue();
		assertThat(requestBody.path("text").path("format").path("schema").path("required").get(0)
				.asText()).isEqualTo("summary");
		assertThat(requestBody.path("input").get(0).path("content").get(0).path("text").asText())
				.contains("REQUIREMENTS_REVIEW", "Goal", "contextPacks");

		assertThat(response.summary()).isEqualTo("Review found one issue.");
		assertThat(response.findings()).hasSize(1);
		AiFindingDraft finding = response.findings().get(0);
		assertThat(finding.findingType()).isEqualTo("ambiguous-language");
		assertThat(finding.severity()).isEqualTo("MEDIUM");
		assertThat(finding.confidence()).isEqualTo(0.82);
		assertThat(finding.evidenceReferences()).containsExactly("target.text");
		assertThat(finding.suggestedIssueText()).contains("ambiguous");
		assertThat(response.messages()).hasSize(1);
		assertThat(response.messages().get(0).level()).isEqualTo(Level.WARNING);
		assertThat(response.usage().provider()).isEqualTo("openai");
		assertThat(response.usage().model()).isEqualTo("gpt-test");
		assertThat(response.usage().inputTokens()).isEqualTo(42);
		assertThat(response.usage().outputTokens()).isEqualTo(11);
		assertThat(response.usage().cachedInputTokens()).isEqualTo(5);
		assertThat(response.providerMetadata()).containsEntry("responseId", "resp_test");
	}

	@Test
	void retriesRetryableHttpFailures() throws Exception {
		AtomicInteger attempts = new AtomicInteger();
		startServer(exchange -> {
			if (attempts.incrementAndGet() == 1) {
				respond(exchange, 429, "{\"error\":{\"message\":\"rate limited\"}}");
				return;
			}
			respond(exchange, 200, openAiResponse(structuredOutput()));
		});
		AiProperties properties = properties(serverEndpoint());
		properties.setMaxRetries(1);
		OpenAiAnalysisClient client = new OpenAiAnalysisClient(properties, objectMapper,
				java.net.http.HttpClient.newHttpClient(), fixedClock());

		AiAnalysisResponse response = client.analyze(request());

		assertThat(response.summary()).isEqualTo("Review found one issue.");
		assertThat(attempts.get()).isEqualTo(2);
	}

	@Test
	void rejectsMissingApiKeyBeforeNetworkWork() {
		AiProperties properties = properties("http://127.0.0.1:1/v1/responses");
		properties.setApiKey(null);
		properties.setApiKeyEnvironmentVariable("");
		OpenAiAnalysisClient client = new OpenAiAnalysisClient(properties, objectMapper,
				java.net.http.HttpClient.newHttpClient(), fixedClock());

		assertThatThrownBy(() -> client.analyze(request()))
				.isInstanceOf(AiAnalysisException.class)
				.hasMessageContaining("API key");
	}

	@Test
	void rejectsInvalidStructuredOutput() throws Exception {
		startServer(exchange -> respond(exchange, 200, openAiResponse("{\"findings\":[]}")));
		OpenAiAnalysisClient client = new OpenAiAnalysisClient(properties(serverEndpoint()),
				objectMapper, java.net.http.HttpClient.newHttpClient(), fixedClock());

		assertThatThrownBy(() -> client.analyze(request()))
				.isInstanceOf(AiAnalysisException.class)
				.hasMessageContaining("summary");
	}

	private void startServer(ExchangeHandler handler) throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/v1/responses", exchange -> {
			try {
				handler.handle(exchange);
			} catch (Throwable t) {
				respond(exchange, 500, "{\"error\":{\"message\":\"" + t.getMessage() + "\"}}");
			}
		});
		server.start();
	}

	private String serverEndpoint() {
		return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/responses";
	}

	private void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(statusCode, bytes.length);
		try (OutputStream outputStream = exchange.getResponseBody()) {
			outputStream.write(bytes);
		}
	}

	private String openAiResponse(String structuredText) throws Exception {
		ObjectNode root = objectMapper.createObjectNode();
		root.put("id", "resp_test");
		root.put("status", "completed");
		root.put("model", "gpt-test");
		ObjectNode content = root.putArray("output").addObject()
				.put("type", "message")
				.putArray("content").addObject();
		content.put("type", "output_text");
		content.put("text", structuredText);
		ObjectNode usage = root.putObject("usage");
		usage.put("input_tokens", 42);
		usage.put("output_tokens", 11);
		usage.putObject("input_tokens_details").put("cached_tokens", 5);
		return objectMapper.writeValueAsString(root);
	}

	private String structuredOutput() throws Exception {
		ObjectNode root = objectMapper.createObjectNode();
		root.put("summary", "Review found one issue.");
		ObjectNode finding = root.putArray("findings").addObject();
		finding.put("findingType", "ambiguous-language");
		finding.put("severity", "MEDIUM");
		finding.put("confidence", 0.82);
		finding.putArray("evidenceReferences").add("target.text");
		finding.put("suggestedIssueText", "The requirement uses ambiguous wording.");
		finding.putArray("suggestedPositions").add("Clarify the measurable outcome.");
		finding.putObject("metadata").put("source", "test");
		root.putArray("warnings").add("Output is a synthetic test response.");
		return objectMapper.writeValueAsString(root);
	}

	private AiProperties properties(String endpoint) {
		AiProperties properties = new AiProperties();
		properties.setProvider("openai");
		properties.setModel("gpt-test");
		properties.setApiKey("test-key");
		properties.setEndpoint(endpoint);
		properties.setMaxOutputTokens(500);
		return properties;
	}

	private AiAnalysisRequest request() {
		return new AiAnalysisRequest("requirements-review-openai", UUID.randomUUID(),
				"REQUIREMENTS_REVIEW", EntityRef.of("Goal", 1L), EntityRef.of("Project", 2L),
				Locale.US, List.of(Map.of("kind", "target", "text", "The system should be fast.")),
				"requirements-review-output", "1", schema(), Map.of("externalProviderAllowed",
						true), Map.of("templateId", "requirements-review"));
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
