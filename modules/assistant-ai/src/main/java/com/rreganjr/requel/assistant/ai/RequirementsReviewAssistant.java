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

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;

import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantMessage;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.EvidenceRef;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.assistant.core.context.EntityContextPack;
import com.rreganjr.requel.assistant.core.context.EntityContextPackBuilder;
import com.rreganjr.requel.assistant.core.persistence.AssistantUsageEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantUsageRepository;
import com.rreganjr.requel.project.TextEntity;

/**
 * First AI-backed assistant (issue #43, Phase 5): reviews a requirement {@link TextEntity}
 * for quality issues through the provider-neutral {@link AiAnalysisClient}, returning
 * annotation drafts.
 *
 * <p>
 * <strong>Trigger model (first cut): manual only.</strong> The bean is only registered when
 * {@code requel.ai.enabled=true} (see {@code @ConditionalOnProperty}); when AI is off it does
 * not exist, so it never affects the legacy/NLP path or the "no assistants registered" skip.
 * When it is registered, {@code SimpleAssistantRegistry} matches it for every
 * {@link TextEntity} run, so it additionally <em>self-gates</em> and runs the provider only
 * when the run's {@code taskType} is {@code REQUIREMENTS_REVIEW} (a manual dispatch — the
 * ordinary post-edit path leaves it {@code null}) and the project is allowed by
 * {@code requel.ai.projectAllowlist}. Otherwise it returns an empty result without calling the
 * provider, so AI cost/latency stays off the edit hot path.
 *
 * <p>
 * This slice (Phase 5 step 2) wires the gate and the provider call end-to-end; with the
 * default {@code NoopAiAnalysisClient} it produces no findings. Mapping
 * {@code AiFindingDraft}s to {@code AnnotationAction}s lands in the next slice.
 */
@Component
@ConditionalOnProperty(name = "requel.ai.enabled", havingValue = "true")
public class RequirementsReviewAssistant implements RequelAssistant<TextEntity> {

	private static final Logger log = LoggerFactory.getLogger(RequirementsReviewAssistant.class);

	public static final String ASSISTANT_ID = "ai-requirements-review";
	public static final String TASK_TYPE = "REQUIREMENTS_REVIEW";

	static final String OUTPUT_SCHEMA_NAME = "RequirementsReviewOutput";
	static final String OUTPUT_SCHEMA_VERSION = "1";

	private static final String OUTPUT_SCHEMA_RESOURCE =
			"/ai/schemas/requirements-review-output.v1.json";

	/** Upper bound on each AI-suggested annotation text, so oversize output is bounded before
	 * it reaches the applicator (which also caps). */
	static final int MAX_ANNOTATION_TEXT = 4000;

	/** Rough chars-per-token used to estimate input size against {@code maxInputTokens}. */
	private static final int CHARS_PER_TOKEN_ESTIMATE = 4;

	private final AiAnalysisClient aiAnalysisClient;
	private final EntityContextPackBuilder entityContextPackBuilder;
	private final AiProperties aiProperties;
	private final AssistantUsageRepository usageRepository;
	private final ObjectMapper objectMapper;
	private final Clock clock;
	private final JsonNode outputSchema;

	@Autowired
	public RequirementsReviewAssistant(AiAnalysisClient aiAnalysisClient,
			EntityContextPackBuilder entityContextPackBuilder, AiProperties aiProperties,
			AssistantUsageRepository usageRepository, ObjectMapper objectMapper) {
		this(aiAnalysisClient, entityContextPackBuilder, aiProperties, usageRepository, objectMapper,
				Clock.systemUTC());
	}

	RequirementsReviewAssistant(AiAnalysisClient aiAnalysisClient,
			EntityContextPackBuilder entityContextPackBuilder, AiProperties aiProperties,
			AssistantUsageRepository usageRepository, ObjectMapper objectMapper, Clock clock) {
		this.aiAnalysisClient = aiAnalysisClient;
		this.entityContextPackBuilder = entityContextPackBuilder;
		this.aiProperties = aiProperties;
		this.usageRepository = usageRepository;
		this.objectMapper = objectMapper;
		this.clock = clock;
		this.outputSchema = loadOutputSchema(objectMapper);
	}

	private static JsonNode loadOutputSchema(ObjectMapper objectMapper) {
		try (InputStream in = RequirementsReviewAssistant.class
				.getResourceAsStream(OUTPUT_SCHEMA_RESOURCE)) {
			if (in == null) {
				log.warn("REQUIREMENTS_REVIEW output schema resource not found: {}",
						OUTPUT_SCHEMA_RESOURCE);
				return NullNode.getInstance();
			}
			return objectMapper.readTree(in);
		} catch (IOException e) {
			log.warn("Could not load REQUIREMENTS_REVIEW output schema {}: {}",
					OUTPUT_SCHEMA_RESOURCE, e.getMessage(), e);
			return NullNode.getInstance();
		}
	}

	/**
	 * Confirms at startup that the AI requirements-review assistant is active. This bean only
	 * exists when {@code requel.ai.enabled=true}, so its presence is the signal; it logs the
	 * provider and model (never the API key) so operators can verify the wiring from the boot log.
	 */
	@PostConstruct
	void logStartupState() {
		List<String> allowlist = aiProperties.getProjectAllowlist();
		log.info(
				"AI requirements-review assistant enabled (provider={}, model={}, projectAllowlist={})",
				aiProperties.getProvider(), aiProperties.getModel(),
				allowlist == null || allowlist.isEmpty() ? "all projects" : allowlist);
	}

	@Override
	public String assistantId() {
		return ASSISTANT_ID;
	}

	@Override
	public Class<TextEntity> targetType() {
		return TextEntity.class;
	}

	/**
	 * Serves only the {@code REQUIREMENTS_REVIEW} task, so it runs for a manual review dispatch
	 * and never on the ordinary post-edit path (and the lexical assistants, which serve the
	 * default task, do not run on a review).
	 */
	@Override
	public boolean handlesTask(String taskType) {
		return TASK_TYPE.equals(taskType);
	}

	@Override
	public AssistantResult analyze(AssistantContext context, TextEntity target) {
		String skipReason = skipReason(context);
		if (skipReason != null) {
			log.debug("RequirementsReviewAssistant skipping run {}: {}", context.runId(), skipReason);
			return AssistantResult.builder().assistantId(ASSISTANT_ID).runId(context.runId())
					.summary(skipReason).build();
		}

		EntityContextPack pack = entityContextPackBuilder.build(target);
		EntityRef targetRef = EntityRef.of(target.getProjectOrDomainEntityInterface().getSimpleName(),
				target.getId());
		List<Object> contextPacks = List.of(pack);

		AssistantResult.Builder result = AssistantResult.builder().assistantId(ASSISTANT_ID)
				.runId(context.runId());

		// Refuse oversize input rather than send it to the provider (review concern #5).
		int estimatedInputTokens = estimateInputTokens(contextPacks);
		if (estimatedInputTokens > aiProperties.getMaxInputTokens()) {
			log.info("Skipping AI review for run {}: estimated {} input tokens exceeds cap {}",
					context.runId(), estimatedInputTokens, aiProperties.getMaxInputTokens());
			return result.summary("Context exceeds the configured AI input cap; review skipped.")
					.message(AssistantMessage.warning("Estimated " + estimatedInputTokens
							+ " input tokens exceeds requel.ai.maxInputTokens="
							+ aiProperties.getMaxInputTokens()))
					.build();
		}

		AiAnalysisRequest request = new AiAnalysisRequest(ASSISTANT_ID, context.runId(), TASK_TYPE,
				targetRef, context.projectRef(), context.locale(), contextPacks,
				OUTPUT_SCHEMA_NAME, OUTPUT_SCHEMA_VERSION, outputSchema, Map.of(),
				context.attributes());

		try {
			AiAnalysisResponse response = aiAnalysisClient.analyze(request);
			persistUsage(context.runId(), response.usage());
			result.summary(response.summary());
			if (response.messages() != null) {
				response.messages().forEach(result::message);
			}
			if (response.findings() != null) {
				for (AiFindingDraft finding : response.findings()) {
					mapFinding(result, targetRef, finding);
				}
			}
		} catch (AiAnalysisException e) {
			log.warn("AI requirements review failed for run {}: {}", context.runId(), e.getMessage(),
					e);
			result.summary("AI requirements review failed")
					.message(AssistantMessage.error(e.getMessage()));
		}
		return result.build();
	}

	/**
	 * Estimate the input size (in tokens) of the context packs from their serialized JSON
	 * length. Best-effort: if serialization fails the estimate is {@code 0} so a serialization
	 * hiccup never blocks a run on its own.
	 */
	private int estimateInputTokens(List<Object> contextPacks) {
		try {
			int chars = objectMapper.writeValueAsString(contextPacks).length();
			return chars / CHARS_PER_TOKEN_ESTIMATE;
		} catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
			log.warn("Could not estimate AI input size: {}", e.getMessage());
			return 0;
		}
	}

	/**
	 * Persist one {@link AssistantUsageEntity} for the run from the provider's usage telemetry
	 * (provider / model / tokens / cost / latency). Bodies are not captured by default. Best
	 * effort: a persistence failure is logged, never failing the run.
	 */
	private void persistUsage(UUID runId, AiUsage usage) {
		if (usage == null) {
			return;
		}
		try {
			AssistantUsageEntity entity = new AssistantUsageEntity(UUID.randomUUID(), runId,
					clock.instant());
			entity.setProvider(usage.provider());
			entity.setModel(usage.model());
			entity.setInputTokens(usage.inputTokens());
			entity.setOutputTokens(usage.outputTokens());
			entity.setCachedInputTokens(usage.cachedInputTokens());
			entity.setCostEstimate(usage.costEstimate());
			entity.setLatencyMs(usage.latency() == null ? null : usage.latency().toMillis());
			usageRepository.save(entity);
		} catch (RuntimeException e) {
			log.warn("Failed to persist AI usage for run {}: {}", runId, e.getMessage(), e);
		}
	}

	/**
	 * Map one AI finding to annotation actions: {@code suggestedIssueText} →
	 * {@code CREATE_OR_UPDATE_ISSUE} (carrying the finding's severity / confidence / type and
	 * any metadata) with its {@code suggestedPositions} as child positions; and
	 * {@code suggestedNoteText} → {@code CREATE_OR_UPDATE_NOTE}. Action keys are derived from
	 * the finding type + a hash of the text so a re-run updates rather than duplicates. A
	 * finding with neither issue nor note text is skipped. The applicator caps text length and
	 * rejects anything it cannot map, so AI output stays untrusted input.
	 */
	private void mapFinding(AssistantResult.Builder result, EntityRef targetRef,
			AiFindingDraft finding) {
		List<EvidenceRef> evidence = evidenceRefs(finding.evidenceReferences());
		String issueText = boundedText(finding.suggestedIssueText());
		String noteText = boundedText(finding.suggestedNoteText());

		if (issueText != null) {
			String issueKey = actionKey(targetRef, "issue", finding.findingType(), issueText);
			Map<String, Object> issueMeta = new HashMap<String, Object>();
			issueMeta.put("findingType", finding.findingType());
			issueMeta.put("mustResolve", Boolean.TRUE);
			if (finding.metadata() != null) {
				issueMeta.putAll(finding.metadata());
			}
			result.annotationAction(new AnnotationAction(issueKey,
					AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE, targetRef, null, issueText,
					finding.severity(), finding.confidence(), evidence, issueMeta));
			for (String position : finding.suggestedPositions()) {
				String positionText = boundedText(position);
				if (positionText == null) {
					continue;
				}
				result.annotationAction(new AnnotationAction(issueKey + ":pos:" + hash(positionText),
						AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, issueKey,
						positionText, null, null, evidence, Map.of()));
			}
		}

		if (noteText != null) {
			Map<String, Object> noteMeta = new HashMap<String, Object>();
			noteMeta.put("findingType", finding.findingType());
			if (finding.metadata() != null) {
				noteMeta.putAll(finding.metadata());
			}
			result.annotationAction(new AnnotationAction(
					actionKey(targetRef, "note", finding.findingType(), noteText),
					AnnotationAction.ActionType.CREATE_OR_UPDATE_NOTE, targetRef, null, noteText, null,
					finding.confidence(), evidence, noteMeta));
		}

		if (issueText == null && noteText == null) {
			log.debug("Skipping AI finding of type {} with no issue or note text",
					finding.findingType());
		}
	}

	private static List<EvidenceRef> evidenceRefs(List<String> references) {
		if (references == null || references.isEmpty()) {
			return List.of();
		}
		List<EvidenceRef> refs = new ArrayList<EvidenceRef>(references.size());
		for (String reference : references) {
			if (reference != null && !reference.isBlank()) {
				refs.add(EvidenceRef.ofLocator(reference));
			}
		}
		return refs;
	}

	private static String actionKey(EntityRef targetRef, String kind, String findingType,
			String text) {
		return ASSISTANT_ID + ":" + targetRef.entityType() + ":" + targetRef.entityId() + ":" + kind
				+ ":" + (findingType == null ? "" : findingType) + ":" + hash(text);
	}

	private static String hash(String text) {
		return Integer.toHexString(text.hashCode());
	}

	/** Trim to {@code null} when blank, otherwise cap to {@link #MAX_ANNOTATION_TEXT}. */
	private static String boundedText(String text) {
		if (text == null || text.isBlank()) {
			return null;
		}
		String trimmed = text.strip();
		return trimmed.length() <= MAX_ANNOTATION_TEXT ? trimmed
				: trimmed.substring(0, MAX_ANNOTATION_TEXT);
	}

	/**
	 * @return a human-readable reason to skip (no provider call), or {@code null} to proceed.
	 */
	private String skipReason(AssistantContext context) {
		if (!TASK_TYPE.equals(context.taskType())) {
			return "Not a " + TASK_TYPE + " run; skipping AI requirements review.";
		}
		if (!aiProperties.isEnabled()) {
			return "AI analysis is disabled (requel.ai.enabled=false); skipping.";
		}
		if (!projectAllowed(context.projectRef())) {
			return "Project is not on requel.ai.projectAllowlist; skipping AI requirements review.";
		}
		return null;
	}

	/**
	 * Project gate: an empty allowlist permits all projects; otherwise the project's id must be
	 * listed. (Name-based allowlisting / per-project settings are refined in a later slice.)
	 */
	private boolean projectAllowed(EntityRef projectRef) {
		List<String> allowlist = aiProperties.getProjectAllowlist();
		if (allowlist == null || allowlist.isEmpty()) {
			return true;
		}
		return projectRef != null && projectRef.entityId() != null
				&& allowlist.contains(String.valueOf(projectRef.entityId()));
	}
}
