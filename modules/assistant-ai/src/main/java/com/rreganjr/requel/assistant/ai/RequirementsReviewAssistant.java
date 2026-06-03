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
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

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

	private final AiAnalysisClient aiAnalysisClient;
	private final EntityContextPackBuilder entityContextPackBuilder;
	private final AiProperties aiProperties;

	@Autowired
	public RequirementsReviewAssistant(AiAnalysisClient aiAnalysisClient,
			EntityContextPackBuilder entityContextPackBuilder, AiProperties aiProperties) {
		this.aiAnalysisClient = aiAnalysisClient;
		this.entityContextPackBuilder = entityContextPackBuilder;
		this.aiProperties = aiProperties;
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
		// Placeholder schema node until the REQUIREMENTS_REVIEW JSON schema resource lands
		// (next-but-one slice); the request contract requires a non-null outputSchema.
		AiAnalysisRequest request = new AiAnalysisRequest(ASSISTANT_ID, context.runId(), TASK_TYPE,
				targetRef, context.projectRef(), context.locale(), List.of(pack),
				OUTPUT_SCHEMA_NAME, OUTPUT_SCHEMA_VERSION, NullNode.getInstance(), Map.of(),
				context.attributes());

		AssistantResult.Builder result = AssistantResult.builder().assistantId(ASSISTANT_ID)
				.runId(context.runId());
		try {
			AiAnalysisResponse response = aiAnalysisClient.analyze(request);
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
		String issueText = trimToNull(finding.suggestedIssueText());
		String noteText = trimToNull(finding.suggestedNoteText());

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
				String positionText = trimToNull(position);
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

	private static String trimToNull(String text) {
		return text == null || text.isBlank() ? null : text;
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
