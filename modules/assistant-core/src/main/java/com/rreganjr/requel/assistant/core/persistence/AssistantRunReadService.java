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
package com.rreganjr.requel.assistant.core.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issue #355: read a target's latest run of one task type with the findings it reported, for the
 * review read ({@code GET /api/ai/reviews}) and the evaluation script built on it. Read-only.
 */
@Component
public class AssistantRunReadService {

	private final AssistantRunRepository runRepository;
	private final AssistantFindingRepository findingRepository;

	@Autowired
	public AssistantRunReadService(AssistantRunRepository runRepository,
			AssistantFindingRepository findingRepository) {
		this.runRepository = Objects.requireNonNull(runRepository, "runRepository");
		this.findingRepository = Objects.requireNonNull(findingRepository, "findingRepository");
	}

	/**
	 * The newest run of {@code taskType} on the target, with every finding that run reported on
	 * it (new or seen again), or empty when the target has no such run.
	 */
	@Transactional(readOnly = true)
	public Optional<RunView> latestRun(String targetType, Long targetId, String taskType) {
		Objects.requireNonNull(targetType, "targetType");
		Objects.requireNonNull(targetId, "targetId");
		Objects.requireNonNull(taskType, "taskType");
		return runRepository
				.findFirstByTargetTypeAndTargetIdAndTaskTypeOrderByCreatedAtDescIdDesc(targetType,
						targetId, taskType)
				.map(run -> toView(run, targetType, targetId));
	}

	private RunView toView(AssistantRunEntity run, String targetType, Long targetId) {
		List<FindingView> findings = new ArrayList<FindingView>();
		for (AssistantFindingEntity finding : findingRepository
				.findByLastSeenRunIdAndTargetTypeAndTargetIdOrderByCreatedAtAscIdAsc(run.getId(),
						targetType, targetId)) {
			findings.add(new FindingView(finding.getId(), finding.getFindingType(),
					kindOf(finding.getIdempotencyKey()), finding.getSeverity(),
					confidence(finding.getConfidence()), finding.getSummary(), finding.getState(),
					finding.getAppliedAnnotationId()));
		}
		return new RunView(run.getId(), run.getAssistantId(), run.getStatus(), run.getCreatedAt(),
				run.getCompletedAt(), run.getLatencyMs(),
				run.getErrorKind(), run.getErrorSummary(), run.getResultSummary(),
				run.getRedactionCount(), List.copyOf(findings), run.getEvidenceUnverified(),
				run.getTemplateId(), run.getTemplateVersion(), run.getVocabularyMisses());
	}

	/**
	 * The annotation kind from an action key. AI review keys read
	 * {@code assistant:Type:id:issue|note:findingType:hash}; anything else has no kind.
	 */
	static String kindOf(String idempotencyKey) {
		if (idempotencyKey == null) {
			return null;
		}
		String[] parts = idempotencyKey.split(":", 5);
		if (parts.length < 4) {
			return null;
		}
		return switch (parts[3]) {
			case "issue" -> "ISSUE";
			case "note" -> "NOTE";
			default -> null;
		};
	}

	private static Double confidence(BigDecimal value) {
		return value == null ? null : value.doubleValue();
	}

	/**
	 * A run and what it reported. (No provider or model: the run row's columns are never filled;
	 * the provider and model are on the run's usage rows.) #260 adds how many findings cited
	 * evidence not in the entity text, and the definitions the run used.
	 */
	public record RunView(String runId, String assistantId, String status, Instant createdAt,
			Instant completedAt, Long latencyMs, String errorKind,
			String errorSummary, String resultSummary, int redactionCount,
			List<FindingView> findings, int evidenceUnverified, String definitionKeys,
			String definitionVersions, int vocabularyMisses) {
	}

	/** One finding a run reported. {@code text} is the issue or note text, up to 500 characters. */
	public record FindingView(String findingId, String findingType, String kind, String severity,
			Double confidence, String text, String state, Long annotationId) {
	}
}
