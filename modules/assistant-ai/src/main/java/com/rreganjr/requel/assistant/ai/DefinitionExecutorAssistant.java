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

import com.fasterxml.jackson.databind.JsonNode;

import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantException;
import com.rreganjr.requel.assistant.api.AssistantMessage;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.CleanupPolicy;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.EvidenceRef;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.assistant.core.AssistantRunWorker;
import com.rreganjr.requel.assistant.core.context.ContextProviderRegistry;
import com.rreganjr.requel.assistant.core.context.EntityContextPack;
import com.rreganjr.requel.assistant.core.context.PackSpec;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinition;
import com.rreganjr.requel.assistant.core.definition.VocabularyEntry;
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
 * <p>Each finding's evidence is checked against the entity's own name and text and every string
 * in its context pack ({@link EvidenceCheck}; #263 widened it from the entity alone). A finding citing evidence that isn't there is kept; the result
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

	/**
	 * #263 (#360): a re-run removes the untouched issues it no longer reports; one a person
	 * discussed or resolved is kept and reads superseded.
	 */
	@Override
	public CleanupPolicy cleanupPolicy() {
		return CleanupPolicy.AUTO_RESOLVE_IF_UNTOUCHED;
	}

	/** Placeholder in a definition's instructions that the vocabulary is rendered into (#263). */
	static final String VOCABULARY_PLACEHOLDER = "{{vocabulary}}";

	/**
	 * The instructions sent to the model: the definition's text, with {@value
	 * #VOCABULARY_PLACEHOLDER} replaced by its vocabulary. Without the placeholder the text is
	 * sent as written (the fallback definition, whose prompt is pinned).
	 */
	String instructions() {
		String text = definition.instructions();
		if (text == null || !text.contains(VOCABULARY_PLACEHOLDER)) {
			return text;
		}
		StringBuilder quality = new StringBuilder();
		StringBuilder extraction = new StringBuilder();
		for (VocabularyEntry entry : definition.vocabulary()) {
			StringBuilder into = entry.isExtraction() ? extraction : quality;
			into.append("- ").append(entry.type()).append(": ").append(entry.description())
					.append('\n');
		}
		StringBuilder rendered = new StringBuilder("Finding types (use exactly one of these):\n")
				.append(quality);
		if (extraction.length() > 0) {
			rendered.append("\nExtraction types (the text is sound but belongs in another kind of")
					.append(" entity; advisory):\n").append(extraction);
		}
		return text.replace(VOCABULARY_PLACEHOLDER, rendered.toString().stripTrailing());
	}

	@Override
	public AssistantResult analyze(AssistantContext context, TextEntity target)
			throws AssistantException {
		Call call = call(context, target);
		if (call.skipped != null) {
			return call.skipped;
		}
		AssistantResult.Builder result = AssistantResult.builder().assistantId(assistantId())
				.runId(context.runId()).summary(call.response.summary());
		if (call.response.messages() != null) {
			call.response.messages().forEach(result::message);
		}
		int unverified = 0;
		int vocabularyMisses = 0;
		for (AiFindingDraft finding : call.findings()) {
			if (call.evidenceUnverified(finding)) {
				unverified++;
			}
			if (vocabularyEntry(finding.findingType()) == null) {
				vocabularyMisses++;
			}
			mapFinding(result, call.targetRef, finding);
		}
		finish(result, context, unverified, vocabularyMisses);
		return result.build();
	}

	/**
	 * Result metadata and warnings for a run's evidence and vocabulary counts, and the other
	 * definitions whose findings it retires.
	 */
	void finish(AssistantResult.Builder result, AssistantContext context, int unverified,
			int vocabularyMisses) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put(AssistantRunWorker.EVIDENCE_UNVERIFIED, unverified);
		metadata.put(AssistantRunWorker.VOCABULARY_MISSES, vocabularyMisses);
		metadata.put(AssistantRunWorker.RETIRES_ASSISTANTS, otherDefinitions(context));
		result.metadata(metadata);
		if (vocabularyMisses > 0) {
			result.message(AssistantMessage.warning(vocabularyMisses
					+ " finding(s) use a type outside the definition's vocabulary"));
		}
		if (unverified > 0) {
			result.message(AssistantMessage.warning(unverified
					+ " finding(s) cite evidence that is not in the text sent for review"));
		}
	}

	/**
	 * What one provider call was sent and returned (#265 splits it from mapping, so a composed
	 * policy pass can attribute the findings itself). {@link #skipped} is set instead when no call
	 * was made.
	 */
	static final class Call {
		final AssistantResult skipped;
		final AiAnalysisResponse response;
		final EntityRef targetRef;
		private final String sentText;

		private Call(AssistantResult skipped, AiAnalysisResponse response, EntityRef targetRef,
				String sentText) {
			this.skipped = skipped;
			this.response = response;
			this.targetRef = targetRef;
			this.sentText = sentText;
		}

		List<AiFindingDraft> findings() {
			return response == null || response.findings() == null ? List.of()
					: response.findings();
		}

		/**
		 * #263: a scenario's flow is in its steps and a use case's in its scenario, so evidence
		 * is checked against everything the model was sent, not the entity alone.
		 */
		boolean evidenceUnverified(AiFindingDraft finding) {
			return EvidenceCheck.unverified(finding.evidenceReferences(), sentText);
		}
	}

	/** Build the context, make the provider call, and persist usage and redactions. */
	Call call(AssistantContext context, TextEntity target) throws AssistantException {
		String skipReason = skipReason(context);
		if (skipReason != null) {
			log.debug("definition {} skipping run {}: {}", definition.key(), context.runId(),
					skipReason);
			return new Call(AssistantResult.builder().assistantId(assistantId())
					.runId(context.runId()).summary(skipReason).build(), null, null, null);
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

		// Refuse oversize input rather than send it to the provider (review concern #5). After
		// #261 only an oversize base pack gets here.
		if (estimatedInputTokens > cap) {
			log.info("Skipping AI review for run {}: estimated {} input tokens exceeds cap {}",
					context.runId(), estimatedInputTokens, cap);
			return new Call(AssistantResult.builder().assistantId(assistantId())
					.runId(context.runId())
					.summary("Context exceeds the configured AI input cap; review skipped.")
					.message(AssistantMessage.warning("Estimated " + estimatedInputTokens
							+ " input tokens exceeds requel.ai.maxInputTokens=" + cap))
					.build(), null, null, null);
		}

		AiAnalysisRequest request = new AiAnalysisRequest(assistantId(), context.runId(),
				definition.taskType(), targetRef, context.projectRef(), context.locale(),
				contextPacks, definition.outputSchemaName(), definition.outputSchemaVersion(),
				outputSchema(), dataHandlingFlags(context.projectRef()), attributes(context),
				instructions());

		try {
			AiAnalysisResponse response = runtime.aiAnalysisClient.analyze(request);
			persistUsage(context.runId(), response.usage());
			String sentText = target.getName() + "\n" + target.getText() + "\n" + packText(pack);
			return new Call(null, response, targetRef, sentText);
		} catch (AiAnalysisException e) {
			// #259: propagate, so the run records the failure (FAILED when this was the run's only
			// assistant) instead of reading as a successful review that found nothing.
			log.warn("AI review {} failed for run {}: {}", definition.key(), context.runId(),
					e.getMessage(), e);
			throw new AssistantException("AI requirements review failed: " + e.getMessage(), e);
		}
	}

	/** The output schema sent with the request; a composed policy pass narrows it (#265). */
	JsonNode outputSchema() {
		return runtime.outputSchemas.schema(definition.outputSchemaName(),
				definition.outputSchemaVersion());
	}

	/** The run's attributes plus the definition that produced the request (#260). */
	Map<String, Object> attributes(AssistantContext context) {
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
	void mapFinding(AssistantResult.Builder result, EntityRef targetRef,
			AiFindingDraft finding) {
		List<EvidenceRef> evidence = evidenceRefs(finding.evidenceReferences());
		String issueText = boundedText(finding.suggestedIssueText());
		String noteText = boundedText(finding.suggestedNoteText());

		VocabularyEntry entry = vocabularyEntry(finding.findingType());
		boolean extraction = entry != null && entry.isExtraction();
		String category = extraction ? VocabularyEntry.EXTRACTION : VocabularyEntry.QUALITY;
		// #263: an extraction is advisory - the text is sound, it belongs in another entity type.
		String severity = extraction ? "LOW" : finding.severity();
		String entityName = finding.metadata() == null ? null
				: (String) finding.metadata().get(ReviewResultMapper.SUGGESTED_ENTITY_NAME);
		String oneClickKind = extraction && entityName != null ? ONE_CLICK.get(finding.findingType())
				: null;

		if (issueText != null) {
			String issueKey = actionKey(targetRef, "issue", finding.findingType(), issueText);
			Map<String, Object> issueMeta = new HashMap<String, Object>();
			issueMeta.put("findingType", finding.findingType());
			issueMeta.put(DEFINITION_KEY, definition.key());
			issueMeta.put(DEFINITION_VERSION, definition.version());
			issueMeta.put("mustResolve", !extraction);
			issueMeta.put(CATEGORY, category);
			if (finding.metadata() != null) {
				issueMeta.putAll(finding.metadata());
			}
			if (oneClickKind != null) {
				// The add-actor and add-to-glossary positions resolve a lexical issue's word.
				issueMeta.put("kind", "LEXICAL");
				issueMeta.put("word", boundedName(entityName));
			}
			result.annotationAction(new AnnotationAction(issueKey,
					AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE, targetRef, null, issueText,
					severity, finding.confidence(), evidence, issueMeta));
			if (oneClickKind != null) {
				String name = boundedName(entityName);
				String text = "ADD_ACTOR_TO_PROJECT".equals(oneClickKind)
						? "Add \"" + name + "\" to the project as an actor."
						: "Add \"" + name + "\" to the project glossary.";
				result.annotationAction(new AnnotationAction(issueKey + ":pos:" + hash(text),
						AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, issueKey, text,
						null, null, evidence, Map.of("kind", oneClickKind)));
			}
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
			noteMeta.put(CATEGORY, category);
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

	/** Finding metadata key: {@code quality} or {@code extraction} (#263). */
	static final String CATEGORY = "category";

	/** Extraction types with a one-click position, by the position kind the applicator knows. */
	private static final Map<String, String> ONE_CLICK = Map.of("EXTRACT_ACTOR",
			"ADD_ACTOR_TO_PROJECT", "EXTRACT_GLOSSARY_TERM", "ADD_WORD_TO_GLOSSARY");

	/** The definition's entry for {@code type}, or null when it is outside the vocabulary. */
	VocabularyEntry vocabularyEntry(String type) {
		for (VocabularyEntry entry : definition.vocabulary()) {
			if (entry.type().equals(type)) {
				return entry;
			}
		}
		return null;
	}

	/**
	 * #263: the other definitions of this task the project can see; their findings on the target
	 * are retired once this one has reviewed it. Empty without a store.
	 */
	private List<String> otherDefinitions(AssistantContext context) {
		// #265: policies sit side by side; one never retires another's findings.
		if (runtime.definitionStore == null || definition.isPolicy()) {
			return List.of();
		}
		Long projectId = context.projectRef() == null ? null : context.projectRef().entityId();
		List<String> keys = new ArrayList<String>();
		for (AssistantDefinition other : runtime.definitionStore.definitionsFor(projectId,
				definition.taskType())) {
			if (!other.key().equals(definition.key())) {
				keys.add(other.key());
			}
		}
		return keys;
	}

	/** A suggested entity name, single-line and short enough for an actor or term name. */
	private static String boundedName(String name) {
		String oneLine = name.replaceAll("\\s+", " ").strip();
		return oneLine.length() <= 255 ? oneLine : oneLine.substring(0, 255);
	}

	/**
	 * Every string in the pack's snapshot and context sections, one per line: the text the model
	 * could quote (#263).
	 */
	private String packText(EntityContextPack pack) {
		StringBuilder text = new StringBuilder();
		try {
			if (pack.snapshot() != null) {
				appendStrings(runtime.objectMapper.valueToTree(pack.snapshot()), text);
			}
			if (pack.context() != null) {
				appendStrings(runtime.objectMapper.valueToTree(pack.context()), text);
			}
		} catch (IllegalArgumentException e) {
			log.debug("could not read the context pack's text for the evidence check", e);
		}
		return text.toString();
	}

	private static void appendStrings(JsonNode node, StringBuilder text) {
		if (node == null) {
			return;
		}
		if (node.isTextual()) {
			text.append(node.asText()).append('\n');
		} else if (node.isContainerNode()) {
			node.forEach(child -> appendStrings(child, text));
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
