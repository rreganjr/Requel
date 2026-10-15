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
package com.rreganjr.requel.ai;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.rreganjr.requel.assistant.core.persistence.AssistantRunReadService;

/**
 * Issue #355: the body of {@code GET /api/ai/reviews} - one review run and the findings it
 * reported. #260 adds the findings citing evidence not in the entity's text and the definitions
 * (keys and versions, comma-joined) the run used; #264 their sources (BUNDLED or PROJECT).
 */
public record AiReviewDto(String runId, String status, Instant createdAt, Instant completedAt,
		Long latencyMs, String errorKind, String errorSummary,
		String summary, int redactionCount, List<Finding> findings, int evidenceUnverified,
		String definitionKeys, String definitionVersions, int vocabularyMisses,
		String definitionSources) {

	/**
	 * One finding. {@code kind} is {@code ISSUE} or {@code NOTE}; {@code text} its text. The target
	 * is the entity it is on: a corpus run (#266) has one finding per participant, sharing the
	 * annotation.
	 */
	public record Finding(String findingType, String kind, String severity, Double confidence,
			String text, String state, Long annotationId, String targetType, Long targetId) {
	}

	static AiReviewDto of(AssistantRunReadService.RunView run) {
		List<Finding> findings = new ArrayList<Finding>();
		for (AssistantRunReadService.FindingView finding : run.findings()) {
			findings.add(new Finding(finding.findingType(), finding.kind(), finding.severity(),
					finding.confidence(), finding.text(), finding.state(), finding.annotationId(),
					finding.targetType(), finding.targetId()));
		}
		return new AiReviewDto(run.runId(), run.status(), run.createdAt(), run.completedAt(), run.latencyMs(), run.errorKind(),
				run.errorSummary(), run.resultSummary(), run.redactionCount(), List.copyOf(findings),
				run.evidenceUnverified(), run.definitionKeys(), run.definitionVersions(),
				run.vocabularyMisses(), run.definitionSources());
	}
}
