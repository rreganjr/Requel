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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Single {@link AiAnalysisClient} backed by Spring AI's {@link ChatClient}. The configured
 * {@code ChatModel} (hosted OpenAI, or any OpenAI-compatible local server via
 * {@code spring.ai.openai.base-url} — Ollama, vLLM, LM Studio, LocalAI, …) is chosen by which
 * {@code spring-ai-starter-model-*} is on the classpath plus {@code spring.ai.*} properties, so
 * this one class replaces the former hand-rolled OpenAI, OpenAI-compatible, and Anthropic clients.
 *
 * <p>
 * Structured output is <em>requested</em> via Spring AI's structured-output support
 * ({@code responseEntity(ReviewResult.class)} forces + binds the reply) and then
 * <em>validated</em> by Requel against the provider-neutral contract before use. Registered as a
 * bean by {@link com.rreganjr.requel.assistant.ai.AiConfiguration} when
 * {@code requel.ai.provider} is {@code openai} or {@code openai-compat}; mutually exclusive with
 * the {@code noop} client.
 */
public class SpringAiAnalysisClient implements AiAnalysisClient {

	private static final Logger log = LoggerFactory.getLogger(SpringAiAnalysisClient.class);

	private static final String DEFAULT_GUIDANCE =
			"You are a Requel requirements analysis assistant. Analyze the target requirement and "
					+ "report quality problems. Findings are drafts for reviewable Requel "
					+ "annotations; do not invent commands that directly mutate project data.";

	private final ChatClient chat;
	private final AiProperties properties;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	public SpringAiAnalysisClient(ChatClient.Builder builder, AiProperties properties,
			ObjectMapper objectMapper) {
		this(builder.build(), properties, objectMapper, Clock.systemUTC());
	}

	SpringAiAnalysisClient(ChatClient chat, AiProperties properties, ObjectMapper objectMapper,
			Clock clock) {
		this.chat = Objects.requireNonNull(chat, "chat");
		this.properties = Objects.requireNonNull(properties, "properties");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	@Override
	public AiAnalysisResponse analyze(AiAnalysisRequest request) throws AiAnalysisException {
		Objects.requireNonNull(request, "request");
		Instant startedAt = clock.instant();

		ReviewResult result;
		ChatResponse chatResponse;
		try {
			// responseEntity(...) registers the JSON schema, forces the model to fill it (native
			// Structured Outputs on OpenAI, prompt-embedded format on compatible servers), binds
			// the reply, AND exposes the ChatResponse so we can read usage/finish metadata.
			var responseEntity = chat.prompt()
					.system(instructions(request))
					.user(prompt(request))
					.call()
					.responseEntity(ReviewResult.class);
			result = responseEntity.entity();
			chatResponse = responseEntity.response();
		} catch (RuntimeException e) {
			throw new AiAnalysisException("Spring AI chat request failed", e);
		}

		validate(result);
		Duration latency = Duration.between(startedAt, clock.instant());
		if (log.isDebugEnabled()) {
			log.debug("spring-ai structured output for run {} ({} findings)", request.runId(),
					result.findings() == null ? 0 : result.findings().size());
		}
		return toResponse(result, chatResponse, latency);
	}

	// ---- prompt construction -------------------------------------------------

	private String instructions(AiAnalysisRequest request) {
		String guidance = request.instructions() != null && !request.instructions().isBlank()
				? request.instructions()
				: DEFAULT_GUIDANCE;
		// Spring AI's structured-output converter appends the JSON-shape format instructions, so we
		// only supply the task guidance here.
		return guidance
				+ "\n\nTask type: " + request.taskType()
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

	// ---- validation + mapping (pure; unit-tested without the network) --------

	/** Requel-side validation of the requested structured output. */
	static void validate(ReviewResult result) throws AiAnalysisException {
		if (result == null) {
			throw new AiAnalysisException("Spring AI structured output was null");
		}
		if (result.summary() == null || result.summary().isBlank()) {
			throw new AiAnalysisException("Spring AI structured output missing summary");
		}
		if (result.findings() != null) {
			for (ReviewResult.Finding finding : result.findings()) {
				if (finding == null || finding.findingType() == null
						|| finding.findingType().isBlank()) {
					throw new AiAnalysisException("Spring AI finding missing findingType");
				}
			}
		}
	}

	AiAnalysisResponse toResponse(ReviewResult result, ChatResponse chatResponse, Duration latency) {
		JsonNode structuredOutput = objectMapper.valueToTree(result);
		AiUsage usage = usage(chatResponse, latency);
		Map<String, Object> metadata = providerMetadata(chatResponse);
		return new AiAnalysisResponse(result.summary(), structuredOutput, findings(result),
				messages(result), usage, metadata);
	}

	private List<AiFindingDraft> findings(ReviewResult result) {
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

	private List<AssistantMessage> messages(ReviewResult result) {
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

	private AiUsage usage(ChatResponse chatResponse, Duration latency) {
		Integer inputTokens = null;
		Integer outputTokens = null;
		// Anthropic cache-read tokens map through Usage#getNativeUsage(); deferred to the
		// Anthropic fast-follow (see doc/spring_ai_provider_port_plan.md).
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

	private static List<String> orEmpty(List<String> values) {
		return values == null ? List.of() : values;
	}

	/**
	 * Mirrors today's provider-neutral output schema ({@code summary} / {@code findings} /
	 * {@code warnings}). Spring AI binds the model reply to this record.
	 */
	public record ReviewResult(String summary, List<Finding> findings, List<String> warnings) {
		public record Finding(String findingType, String severity, Double confidence,
				List<String> evidenceReferences, String suggestedIssueText, String suggestedNoteText,
				List<String> suggestedPositions) {
		}
	}
}
