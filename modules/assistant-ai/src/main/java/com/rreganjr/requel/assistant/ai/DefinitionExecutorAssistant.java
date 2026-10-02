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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantException;
import com.rreganjr.requel.assistant.api.AssistantMessage;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.EvidenceRef;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.assistant.core.AssistantRunWorker;
import com.rreganjr.requel.assistant.core.context.ContextProviderRegistry;
import com.rreganjr.requel.assistant.core.context.EntityContextPack;
import com.rreganjr.requel.assistant.core.context.PackSpec;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinition;
import com.rreganjr.requel.assistant.core.definition.DefinitionBacked;
import com.rreganjr.requel.assistant.core.persistence.AssistantUsageEntity;
import com.rreganjr.requel.project.DataHandlingSettings;
import com.rreganjr.requel.project.TextEntity;

/**
 * Issue #260: runs one {@link AssistantDefinition} - what {@code RequirementsReviewAssistant} did
 * with a hard-coded prompt, now parameterised by the definition: task gating, the project
 * allowlist, the input-token estimate, the provider call, usage persistence, mapping
 * {@link AiFindingDraft}s to annotation actions, and text caps. {@link #assistantId()} is the
 * definition key, so finding idempotency keys and run records stay attributable (the default
 * keeps {@code ai-requirements-review}).
 *
 * <p>Each finding's evidence is checked against the entity's own name and text
 * ({@link EvidenceCheck}). A finding citing evidence that isn't there is kept; the result
 * metadata counts it ({@link AssistantRunWorker#EVIDENCE_UNVERIFIED}) and the run records it.
 *
 * <p>Built per run by {@link AiDefinitionExecutorFactory}; not a bean.
 */
public class DefinitionExecutorAssistant implements RequelAssistant<TextEntity>, DefinitionBacked {

	private static final Logger log = LoggerFactory.getLogger(DefinitionExecutorAssistant.class);

	/** Upper bound on each AI-suggested annotation text, so oversize output is bounded before
	 * it reaches the applicator (which also caps). */
	static final int MAX_ANNOTATION_TEXT = 4000;

	/** Rough chars-per-token used to estimate input size against {@code maxInputTokens}. */
	private static final int CHARS_PER_TOKEN_ESTIMATE = 4;

	/** Request attribute and finding metadata keys naming the definition (#260). */
	static final String DEFINITION_KEY = "definitionKey";
	static final String DEFINITION_VERSION = "definitionVersion";
	static final String DEFINITION_SOURCE = "definitionSource";

	private final AssistantDefinition definition;
	private final AiDefinitionExecutorFactory runtime;

	DefinitionExecutorAssistant(AssistantDefinition definition, AiDefinitionExecutorFactory runtime) {
		this.definition = definition;
		this.runtime = runtime;
	}

	@Override
	public AssistantDefinition definition() {
		return definition;
	}

	@Override
	public String assistantId() {
		return definition.key();
	}

	@Override
	public String displayName() {
		return definition.displayName();
	}

	@Override
	public Class<TextEntity> targetType() {
		return TextEntity.class;
	}

	/** Serves only the definition's task, so it never runs on the ordinary post-edit path. */
	@Override
	public boolean handlesTask(String taskType) {
		return definition.taskType().equals(taskType);
	}

	/** #268: a project can switch a definition off; the registry applies it. */
	@Override
	public boolean projectSwitchable() {
		return true;
	}

	@Override
	public AssistantResult analyze(AssistantContext context, TextEntity target)
			throws AssistantException {
		String skipReason = skipReason(context);
		if (skipReason != null) {
			log.debug("definition {} skipping run {}: {}", definition.key(), context.runId(),
					skipReason);
			return AssistantResult.builder().assistantId(assistantId()).runId(context.runId())
					.summary(skipReason).build();
		}

		int cap = runtime.aiProperties.getMaxInputTokens();
		EntityContextPack pack = runtime.entityContextPackBuilder.build(target, packSpec(cap));
		// #261: providers never cause a skip - drop their sections, last first, until it fits.
		int estimatedInputTokens = estimateInputTokens(List.of(pack));
		while (estimatedInputTokens > cap && !pack.context().isEmpty()) {
			pack = pack.withoutLastSection();
			estimatedInputTokens = estimateInputTokens(List.of(pack));
		}
		recordRedactions(context.runId(), pack);
		EntityRef targetRef = EntityRef.of(target.getProjectOrDomainEntityInterface().getSimpleName(),
				target.getId());
		List<Object> contextPacks = List.of(pack);

		AssistantResult.Builder result = AssistantResult.builder().assistantId(assistantId())
				.runId(context.runId());

		// Refuse oversize input rather than send it to the provider (review concern #5). After
		// #261 only an oversize base pack gets here.
		if (estimatedInputTokens > cap) {
			log.info("Skipping AI review for run {}: estimated {} input tokens exceeds cap {}",
					context.runId(), estimatedInputTokens, cap);
			return result.summary("Context exceeds the configured AI input cap; review skipped.")
					.message(AssistantMessage.warning("Estimated " + estimatedInputTokens
							+ " input tokens exceeds requel.ai.maxInputTokens=" + cap))
					.build();
		}

		AiAnalysisRequest request = new AiAnalysisRequest(assistantId(), context.runId(),
				definition.taskType(), targetRef, context.projectRef(), context.locale(),
				contextPacks, definition.outputSchemaName(), definition.outputSchemaVersion(),
				runtime.outputSchemas.schema(definition.outputSchemaName(),
						definition.outputSchemaVersion()),
				dataHandlingFlags(context.projectRef()), attributes(context),
				definition.instructions());

		try {
			AiAnalysisResponse response = runtime.aiAnalysisClient.analyze(request);
			persistUsage(context.runId(), response.usage());
			result.summary(response.summary());
			if (response.messages() != null) {
				response.messages().forEach(result::message);
			}
			int unverified = 0;
			if (response.findings() != null) {
				String entityText = target.getName() + "\n" + target.getText();
				for (AiFindingDraft finding : response.findings()) {
					if (EvidenceCheck.unverified(finding.evidenceReferences(), entityText)) {
						unverified++;
					}
					mapFinding(result, targetRef, finding);
				}
			}
			result.metadata(Map.of(AssistantRunWorker.EVIDENCE_UNVERIFIED, unverified));
			if (unverified > 0) {
				result.message(AssistantMessage.warning(unverified
						+ " finding(s) cite evidence that is not in the entity's text"));
			}
		} catch (AiAnalysisException e) {
			// #259: propagate, so the run records the failure (FAILED when this was the run's only
			// assistant) instead of reading as a successful review that found nothing.
			log.warn("AI review {} failed for run {}: {}", definition.key(), context.runId(),
					e.getMessage(), e);
			throw new AssistantException("AI requirements review failed: " + e.getMessage(), e);
		}
		return result.build();
	}

	/** The run's attributes plus the definition that produced the request (#260). */
	private Map<String, Object> attributes(AssistantContext context) {
		Map<String, Object> attributes = new LinkedHashMap<String, Object>(context.attributes());
		attributes.put(DEFINITION_KEY, definition.key());
		attributes.put(DEFINITION_VERSION, definition.version());
		attributes.put(DEFINITION_SOURCE, definition.source().name());
		return attributes;
	}

	/**
	 * Issue #262: what this request may do with the project's text, from the project's settings
	 * and the configured provider. Every remote client refuses a request without
	 * {@code externalProviderAllowed=true} ({@link DataHandlingGuard}).
	 */
	Map<String, Object> dataHandlingFlags(EntityRef projectRef) {
		Long projectId = projectRef == null ? null : projectRef.entityId();
		DataHandlingSettings settings = DataHandlingSettings.forProject(projectId, runtime.settingsStore);
		AiProviderLocality locality = runtime.providerLocality != null ? runtime.providerLocality
				: AiProviderLocality.classify(runtime.aiProperties.getProvider(), null);
		List<String> categories = new ArrayList<String>();
		for (DataHandlingSettings.RedactionCategory category : settings.redactionCategories()) {
			categories.add(category.id());
		}
		Map<String, Object> flags = new java.util.LinkedHashMap<String, Object>();
		flags.put(DataHandlingGuard.EXTERNAL_PROVIDER_ALLOWED, settings.externalProviderAllowed());
		flags.put(DataHandlingGuard.PROVIDER_LOCALITY, locality.id());
		flags.put(DataHandlingGuard.PROVIDER, runtime.aiProperties.getProvider());
		flags.put(DataHandlingGuard.REDACTION_CATEGORIES, List.copyOf(categories));
		return flags;
	}

	/** Best effort, like usage: a failure to record never fails the run. */
	private void recordRedactions(UUID runId, EntityContextPack pack) {
		if (runtime.runStore == null || pack == null || pack.metadata() == null) {
			return;
		}
		try {
			runtime.runStore.recordRedactions(runId, pack.metadata().redactionCount(),
					pack.metadata().redactionCategories());
		} catch (RuntimeException e) {
			log.warn("Failed to record redactions for run {}: {}", runId, e.getMessage(), e);
		}
	}

	/**
	 * Issue #261: the definition's providers and budgets, capped at the input budget in
	 * characters. A definition naming only {@code entity} keeps the pack's own cap, so its pack
	 * is exactly what it was before providers existed.
	 */
	private PackSpec packSpec(int maxInputTokens) {
		boolean entityOnly = definition.contextProviders().stream()
				.allMatch(ContextProviderRegistry.ENTITY::equals);
		return new PackSpec(definition.contextProviders(), definition.contextBudgets(),
				entityOnly ? 0 : maxInputTokens * CHARS_PER_TOKEN_ESTIMATE);
	}

	/**
	 * Estimate the input size (in tokens) of the context packs from their serialized JSON
	 * length. Best-effort: if serialization fails the estimate is {@code 0} so a serialization
	 * hiccup never blocks a run on its own.
	 */
	private int estimateInputTokens(List<Object> contextPacks) {
		try {
			int chars = runtime.objectMapper.writeValueAsString(contextPacks).length();
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
					runtime.clock.instant());
			entity.setProvider(usage.provider());
			entity.setModel(usage.model());
			entity.setInputTokens(usage.inputTokens());
			entity.setOutputTokens(usage.outputTokens());
			entity.setCachedInputTokens(usage.cachedInputTokens());
			entity.setCostEstimate(usage.costEstimate());
			entity.setLatencyMs(usage.latency() == null ? null : usage.latency().toMillis());
			runtime.usageRepository.save(entity);
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
			issueMeta.put(DEFINITION_KEY, definition.key());
			issueMeta.put(DEFINITION_VERSION, definition.version());
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
			noteMeta.put(DEFINITION_KEY, definition.key());
			noteMeta.put(DEFINITION_VERSION, definition.version());
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

	private String actionKey(EntityRef targetRef, String kind, String findingType,
			String text) {
		return assistantId() + ":" + targetRef.entityType() + ":" + targetRef.entityId() + ":" + kind
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
		if (!definition.taskType().equals(context.taskType())) {
			return "Not a " + definition.taskType() + " run; skipping AI review "
					+ definition.key() + ".";
		}
		if (!runtime.aiProperties.isEnabled()) {
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
		List<String> allowlist = runtime.aiProperties.getProjectAllowlist();
		if (allowlist == null || allowlist.isEmpty()) {
			return true;
		}
		return projectRef != null && projectRef.entityId() != null
				&& allowlist.contains(String.valueOf(projectRef.entityId()));
	}
}
