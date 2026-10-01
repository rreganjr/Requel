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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.assistant.core.persistence.AssistantRunReadService.FindingView;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunReadService.RunView;

/** #355: the review read's body is a straight copy of the run view. */
class AiReviewDtoTest {

	@Test
	void everyFieldIsCopied() {
		Instant created = Instant.parse("2026-10-01T12:00:00Z");
		RunView run = new RunView("run-1", "ai-requirements-review", "SUCCEEDED", created,
				created.plusSeconds(20), 20000L, null, null, "Two problems.", 1, List.of(
						new FindingView("f-1", "AMBIGUOUS", "ISSUE", "HIGH", 0.8, "Define it.",
								"ACTIVE", 42L),
						new FindingView("f-2", "CONTEXT", "NOTE", null, null, "A note.", "ACTIVE",
								null)));

		AiReviewDto dto = AiReviewDto.of(run);

		assertEquals("run-1", dto.runId());
		assertEquals("SUCCEEDED", dto.status());
		assertEquals(created, dto.createdAt());
		assertEquals(created.plusSeconds(20), dto.completedAt());
		assertEquals(20000L, dto.latencyMs());
		assertNull(dto.errorKind());
		assertNull(dto.errorSummary());
		assertEquals("Two problems.", dto.summary());
		assertEquals(1, dto.redactionCount());
		assertEquals(List.of(
				new AiReviewDto.Finding("AMBIGUOUS", "ISSUE", "HIGH", 0.8, "Define it.", "ACTIVE",
						42L),
				new AiReviewDto.Finding("CONTEXT", "NOTE", null, null, "A note.", "ACTIVE", null)),
				dto.findings());
	}
}
