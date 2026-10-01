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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.ObjectProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.assistant.ai.AiAnalysisClient;
import com.rreganjr.requel.assistant.ai.AiAnalysisException;
import com.rreganjr.requel.assistant.ai.AiAnalysisRequest;
import com.rreganjr.requel.assistant.ai.AiAnalysisResponse;
import com.rreganjr.requel.assistant.ai.AiPromptBuilder;
import com.rreganjr.requel.assistant.ai.AiProperties;
import com.rreganjr.requel.assistant.ai.AiProviderLocality;
import com.rreganjr.requel.assistant.ai.DataHandlingGuard;
import com.rreganjr.requel.assistant.ai.AiUsage;
import com.rreganjr.requel.assistant.ai.ReviewResultMapper;
import com.rreganjr.requel.assistant.ai.ReviewResultMapper.ReviewResult;

/**
 * Single {@link AiAnalysisClient} backed by Spring AI's {@link ChatClient}. The configured
 * {@code ChatModel} (hosted OpenAI, or any OpenAI-compatible local server via
 * {@code spring.ai.openai.base-url} — Ollama, vLLM, LM Studio, LocalAI, …) is chosen by which
 * {@code spring-ai-starter-model-*} is on the classpath plus {@code spring.ai.*} properties, so
 * this one class replaces the former hand-rolled OpenAI, OpenAI-compatible, and Anthropic clients.
 *
 * <p>
 * Structured output is <em>requested</em> via Spring AI's structured-output support
 * ({@code responseEntity(ReviewResult.class)} forces + binds the reply) and then
 * <em>validated</em> by Requel against the provider-neutral contract before use; validation and
 * mapping live in {@link ReviewResultMapper}, shared with the CLI client (#259). Registered as a
 * bean by {@link com.rreganjr.requel.assistant.ai.AiConfiguration} when
 * {@code requel.ai.provider} is {@code openai} or {@code openai-compat}; mutually exclusive with
 * the {@code noop} client.
 */
public class SpringAiAnalysisClient implements AiAnalysisClient {

	private static final Logger log = LoggerFactory.getLogger(SpringAiAnalysisClient.class);

	/**
	 * Supplies the {@link ChatClient.Builder} lazily. Injected as an {@link ObjectProvider} (rather
	 * than the builder directly) on purpose: the autoconfigured `ChatClient.Builder` pulls in the
	 * OpenAI `ChatModel`, whose `ToolCallingManager` collects every `ToolCallbackProvider` bean —
	 * including the MCP server's, which depends back through the gateway/command chain into the
	 * assistant and so into this client. Depending on the builder eagerly at construction therefore
	 * forms a bean cycle (assistant ↔ MCP via Spring AI tool-calling). Resolving it lazily, after
	 * the context is built, breaks the cycle. Null in the unit-test constructor.
	 */
	private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
	private volatile ChatClient chat;
	private final AiProperties properties;
	private final ReviewResultMapper mapper;
	private final AiPromptBuilder promptBuilder;
	private final Clock clock;
	/** #262: remote providers check the request's data-handling flags before sending. */
	private volatile AiProviderLocality locality;

	public SpringAiAnalysisClient(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
			AiProperties properties, ObjectMapper objectMapper) {
		this.chatClientBuilderProvider = Objects.requireNonNull(chatClientBuilderProvider,
				"chatClientBuilderProvider");
		this.properties = Objects.requireNonNull(properties, "properties");
		this.mapper = new ReviewResultMapper(Objects.requireNonNull(objectMapper, "objectMapper"));
		this.promptBuilder = new AiPromptBuilder(objectMapper, properties);
		this.clock = Clock.systemUTC();
		this.locality = AiProviderLocality.classify(properties.getProvider(), null);
	}

	SpringAiAnalysisClient(ChatClient chat, AiProperties properties, ObjectMapper objectMapper,
			Clock clock) {
		this.chat = Objects.requireNonNull(chat, "chat");
		this.chatClientBuilderProvider = null;
		this.properties = Objects.requireNonNull(properties, "properties");
		this.mapper = new ReviewResultMapper(Objects.requireNonNull(objectMapper, "objectMapper"));
		this.promptBuilder = new AiPromptBuilder(objectMapper, properties);
		this.clock = Objects.requireNonNull(clock, "clock");
		this.locality = AiProviderLocality.classify(properties.getProvider(), null);
	}

	/**
	 * Issue #262: the configured provider's locality (an {@code openai-compat} base-url on this
	 * machine is local). Without it the provider name alone decides, which treats
	 * {@code openai-compat} as remote.
	 */
	public void setLocality(AiProviderLocality locality) {
		this.locality = Objects.requireNonNull(locality, "locality");
	}

	/** Builds the {@link ChatClient} on first use and memoizes it (see field doc for why lazy). */
	private ChatClient chat() {
		ChatClient local = this.chat;
		if (local == null) {
			synchronized (this) {
				local = this.chat;
				if (local == null) {
					local = chatClientBuilderProvider.getObject().build();
					this.chat = local;
				}
			}
		}
		return local;
	}

	@Override
	public AiAnalysisResponse analyze(AiAnalysisRequest request) throws AiAnalysisException {
		Objects.requireNonNull(request, "request");
		DataHandlingGuard.requireAllowed(request, locality);
		Instant startedAt = clock.instant();

		ReviewResult result;
		ChatResponse chatResponse;
		try {
			// responseEntity(...) registers the JSON schema, forces the model to fill it (native
			// Structured Outputs on OpenAI, prompt-embedded format on compatible servers), binds
			// the reply, AND exposes the ChatResponse so we can read usage/finish metadata.
			var responseEntity = chat().prompt()
					// Spring AI's structured-output converter appends the JSON-shape format
					// instructions, so only the shared task guidance and body are supplied here.
					.system(promptBuilder.instructions(request))
					.user(promptBuilder.prompt(request))
					.call()
					.responseEntity(ReviewResult.class);
			result = responseEntity.entity();
			chatResponse = responseEntity.response();
		} catch (RuntimeException e) {
			throw new AiAnalysisException("Spring AI chat request failed", e);
		}

		ReviewResultMapper.validate(result);
		Duration latency = Duration.between(startedAt, clock.instant());
		if (log.isDebugEnabled()) {
			log.debug("spring-ai structured output for run {} ({} findings)", request.runId(),
					result.findings() == null ? 0 : result.findings().size());
		}
		return toResponse(result, chatResponse, latency);
	}

	// ---- mapping (pure; unit-tested without the network) ------------------

	/** Maps a validated result: the reply via {@link ReviewResultMapper}, usage from Spring AI. */
	AiAnalysisResponse toResponse(ReviewResult result, ChatResponse chatResponse, Duration latency) {
		return mapper.toResponse(result, usage(chatResponse, latency),
				providerMetadata(chatResponse));
	}

	private AiUsage usage(ChatResponse chatResponse, Duration latency) {
		Integer inputTokens = null;
		Integer outputTokens = null;
		// Anthropic cache-read tokens map through Usage#getNativeUsage(); deferred to the
		// Anthropic fast-follow (see doc/work/2.0/spring_ai_provider_port_plan.md).
		Integer cachedInputTokens = null;
		if (chatResponse != null && chatResponse.getMetadata() != null
				&& chatResponse.getMetadata().getUsage() != null) {
			var u = chatResponse.getMetadata().getUsage();
			inputTokens = asInt(u.getPromptTokens());
			outputTokens = asInt(u.getCompletionTokens());
		}
		return new AiUsage(properties.getProvider(), properties.getModel(), inputTokens,
				outputTokens, cachedInputTokens, latency, null);
	}

	private Map<String, Object> providerMetadata(ChatResponse chatResponse) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("provider", properties.getProvider());
		metadata.put("model", properties.getModel());
		if (chatResponse != null && chatResponse.getResult() != null
				&& chatResponse.getResult().getMetadata() != null) {
			String finishReason = chatResponse.getResult().getMetadata().getFinishReason();
			if (finishReason != null && !finishReason.isBlank()) {
				metadata.put("finishReason", finishReason);
			}
		}
		return metadata;
	}

	/** Token-count getters returned {@code Long} historically and {@code Integer} now; accept both. */
	private static Integer asInt(Object value) {
		return value instanceof Number number ? number.intValue() : null;
	}
}
