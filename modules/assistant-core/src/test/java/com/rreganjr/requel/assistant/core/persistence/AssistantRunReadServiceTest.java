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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** #355: the review read's mapping, without a database. */
class AssistantRunReadServiceTest {

	private final Instant now = Instant.parse("2026-10-01T12:00:00Z");
	private final AssistantRunRepository runs = mock(AssistantRunRepository.class);
	private final AssistantFindingRepository findings = mock(AssistantFindingRepository.class);
	private final AssistantRunReadService service = new AssistantRunReadService(runs, findings);

	@Test
	void noRunReadsAsEmpty() {
		when(runs.findFirstByTargetTypeAndTargetIdAndTaskTypeOrderByCreatedAtDescIdDesc("Goal", 7L,
				"REQUIREMENTS_REVIEW")).thenReturn(Optional.empty());

		assertThat(service.latestRun("Goal", 7L, "REQUIREMENTS_REVIEW")).isEmpty();
	}

	@Test
	void theRunAndItsFindingsAreMapped() {
		UUID runId = UUID.randomUUID();
		AssistantRunEntity run = new AssistantRunEntity(runId, "ai-requirements-review", "FAILED",
				now, now.plusSeconds(3));
		run.setCompletedAt(now.plusSeconds(3));
		run.setLatencyMs(3000L);
		run.setErrorKind("AssistantWorkerException");
		run.setErrorSummary("model reply is not valid JSON");
		run.setResultSummary("The goal is vague.");
		run.setRedactionCount(2);
		run.setEvidenceUnverified(1);
		run.setTemplateId("ai-requirements-review");
		run.setTemplateVersion("1");
		when(runs.findFirstByTargetTypeAndTargetIdAndTaskTypeOrderByCreatedAtDescIdDesc("Goal", 7L,
				"REQUIREMENTS_REVIEW")).thenReturn(Optional.of(run));

		AssistantFindingEntity issue = finding(runId,
				"ai-requirements-review:Goal:7:issue:AMBIGUOUS:1f", "AMBIGUOUS");
		issue.setSeverity("HIGH");
		issue.setConfidence(new BigDecimal("0.750"));
		issue.setSummary("Define 'fast'.");
		issue.setAppliedAnnotationId(42L);
		AssistantFindingEntity note = finding(runId,
				"ai-requirements-review:Goal:7:note:CONTEXT:2a", "CONTEXT");
		note.setSummary("Owned by the desk.");
		when(findings.findByLastSeenRunIdAndTargetTypeAndTargetIdOrderByCreatedAtAscIdAsc(
				runId.toString(), "Goal", 7L)).thenReturn(List.of(issue, note));

		AssistantRunReadService.RunView view = service
				.latestRun("Goal", 7L, "REQUIREMENTS_REVIEW").orElseThrow();

		assertThat(view.runId()).isEqualTo(runId.toString());
		assertThat(view.assistantId()).isEqualTo("ai-requirements-review");
		assertThat(view.status()).isEqualTo("FAILED");
		assertThat(view.createdAt()).isEqualTo(now);
		assertThat(view.completedAt()).isEqualTo(now.plusSeconds(3));
		assertThat(view.latencyMs()).isEqualTo(3000L);
		assertThat(view.errorKind()).isEqualTo("AssistantWorkerException");
		assertThat(view.errorSummary()).isEqualTo("model reply is not valid JSON");
		assertThat(view.resultSummary()).isEqualTo("The goal is vague.");
		assertThat(view.redactionCount()).isEqualTo(2);
		assertThat(view.evidenceUnverified()).isEqualTo(1);
		assertThat(view.definitionKeys()).isEqualTo("ai-requirements-review");
		assertThat(view.definitionVersions()).isEqualTo("1");
		assertThat(view.findings()).hasSize(2);

		AssistantRunReadService.FindingView first = view.findings().get(0);
		assertThat(first.findingId()).isEqualTo(issue.getId());
		assertThat(first.findingType()).isEqualTo("AMBIGUOUS");
		assertThat(first.kind()).isEqualTo("ISSUE");
		assertThat(first.severity()).isEqualTo("HIGH");
		assertThat(first.confidence()).isEqualTo(0.75);
		assertThat(first.text()).isEqualTo("Define 'fast'.");
		assertThat(first.state()).isEqualTo("ACTIVE");
		assertThat(first.annotationId()).isEqualTo(42L);

		AssistantRunReadService.FindingView second = view.findings().get(1);
		assertThat(second.kind()).isEqualTo("NOTE");
		assertThat(second.confidence()).isNull();
		assertThat(second.severity()).isNull();
		assertThat(second.annotationId()).isNull();
	}

	/** #266: a corpus run's findings are on its participants, so all of them are listed. */
	@Test
	void aCorpusRunListsItsFindingsOnEveryParticipant() {
		UUID runId = UUID.randomUUID();
		AssistantRunEntity run = new AssistantRunEntity(runId, "corpus-finder", "COMPLETED", now,
				now);
		when(runs.findFirstByTargetTypeAndTargetIdAndTaskTypeOrderByCreatedAtDescIdDesc("Project",
				3L, "CORPUS_CANDIDATES")).thenReturn(Optional.of(run));
		AssistantFindingEntity onGoal = new AssistantFindingEntity(UUID.randomUUID(),
				"corpus-finder:Goal:7:POSSIBLE_OVERLAP-ab", "corpus-finder", "Goal", 7L,
				"POSSIBLE_OVERLAP", "ACTIVE", runId, now);
		AssistantFindingEntity onStory = new AssistantFindingEntity(UUID.randomUUID(),
				"corpus-finder:Story:2:POSSIBLE_OVERLAP-ab", "corpus-finder", "Story", 2L,
				"POSSIBLE_OVERLAP", "ACTIVE", runId, now);
		when(findings.findByLastSeenRunIdOrderByCreatedAtAscIdAsc(runId.toString()))
				.thenReturn(List.of(onGoal, onStory));

		AssistantRunReadService.RunView view = service
				.latestRun("Project", 3L, "CORPUS_CANDIDATES").orElseThrow();

		assertThat(view.findings()).extracting(f -> f.targetType() + ":" + f.targetId())
				.containsExactly("Goal:7", "Story:2");
	}

	@Test
	void kindOfAShortOrForeignKeyIsNull() {
		assertThat(AssistantRunReadService.kindOf("a:b:c")).isNull();
		assertThat(AssistantRunReadService.kindOf("ai:Goal:7:position:x")).isNull();
	}

	private AssistantFindingEntity finding(UUID runId, String key, String type) {
		return new AssistantFindingEntity(UUID.randomUUID(), key, "ai-requirements-review", "Goal",
				7L, type, "ACTIVE", runId, now);
	}
}
