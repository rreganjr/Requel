/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.List;

import com.rreganjr.requel.assistant.core.persistence.AssistantRunReadService;
import com.rreganjr.requel.service.api.dto.AiReviewDto;

/** Issue #387: an assistant run as the {@link AiReviewDto} the review reads return. */
public final class AiReviewDtos {

	private AiReviewDtos() {
	}

	public static AiReviewDto of(AssistantRunReadService.RunView run) {
		List<AiReviewDto.Finding> findings = new ArrayList<AiReviewDto.Finding>();
		for (AssistantRunReadService.FindingView finding : run.findings()) {
			findings.add(new AiReviewDto.Finding(finding.findingType(), finding.kind(),
					finding.severity(), finding.confidence(), finding.text(), finding.state(),
					finding.annotationId(), finding.targetType(), finding.targetId()));
		}
		return new AiReviewDto(run.runId(), run.status(), run.createdAt(), run.completedAt(),
				run.latencyMs(), run.errorKind(), run.errorSummary(), run.resultSummary(),
				run.redactionCount(), List.copyOf(findings), run.evidenceUnverified(),
				run.definitionKeys(), run.definitionVersions(), run.vocabularyMisses(),
				run.definitionSources());
	}
}
