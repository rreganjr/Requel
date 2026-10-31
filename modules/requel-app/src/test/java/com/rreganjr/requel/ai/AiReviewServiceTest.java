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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.rreganjr.requel.assistant.ai.AiProperties;
import com.rreganjr.requel.assistant.core.SimpleAssistantRegistry;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinition;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunReadService;
import com.rreganjr.requel.command.AnalysisRequestDispatcher;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;

/**
 * Issue #387: when a review can run, and what a request does when it can't or when one is
 * already in flight. Collaborators are mocked; {@link AiReviewDispatchIT} runs the real thing.
 */
class AiReviewServiceTest {

	private final ProjectRepository projectRepository = mock(ProjectRepository.class);
	private final CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
	private final AnalysisRequestDispatcher dispatcher = mock(AnalysisRequestDispatcher.class);
	private final AssistantRunReadService runs = mock(AssistantRunReadService.class);
	private final SimpleAssistantRegistry registry = mock(SimpleAssistantRegistry.class);
	private final AiProperties ai = new AiProperties();
	private final AiReviewService service = new AiReviewService(projectRepository,
			currentUserResolver, dispatcher, runs);
	private final User admin = mock(User.class);
	private Goal goal;

	@BeforeEach
	void setUp() {
		when(admin.hasRole(SystemAdminUserRole.class)).thenReturn(true);
		when(currentUserResolver.resolve()).thenReturn(admin);
		Project project = mock(Project.class);
		when(project.getId()).thenReturn(7L);
		goal = mock(Goal.class);
		when(goal.getId()).thenReturn(3L);
		doReturn(Goal.class).when(goal).getProjectOrDomainEntityInterface();
		when(goal.getProjectOrDomain()).thenReturn(project);
		when(projectRepository.findById(Goal.class, 3L)).thenReturn(goal);
		ai.setEnabled(true);
		service.setAiProperties(ai);
		service.setRegistry(registry);
		when(registry.reviewsFor(goal, 7L)).thenReturn(List.of(mock(AssistantDefinition.class)));
		when(registry.policiesFor(goal, 7L)).thenReturn(List.of());
		when(runs.latestRun(anyString(), anyLong(), anyString())).thenReturn(Optional.empty());
	}

	@Test
	void availableWhenAiIsOnTheProjectIsAllowedAndTheTypesReviewIsOn() {
		assertThat(service.availability("Goal", 3L).available()).isTrue();
		assertThat(service.availability("Goal", 3L).reason()).isNull();
	}

	@Test
	void unavailableWhenAiIsOffOnTheServer() {
		ai.setEnabled(false);
		assertThat(service.availability("Goal", 3L).reason()).isEqualTo(AiReviewService.AI_OFF);

		service.setAiProperties(null);
		assertThat(service.availability("Goal", 3L).reason()).isEqualTo(AiReviewService.AI_OFF);
	}

	@Test
	void unavailableWhenTheProjectIsNotOnTheAllowlist() {
		ai.setProjectAllowlist(List.of("8"));
		assertThat(service.availability("Goal", 3L).reason())
				.isEqualTo(AiReviewService.NOT_ALLOWLISTED);

		ai.setProjectAllowlist(List.of("8", "7"));
		assertThat(service.availability("Goal", 3L).available()).isTrue();
	}

	@Test
	void unavailableWhenTheTypesReviewIsSwitchedOffInTheProject() {
		when(registry.reviewsFor(goal, 7L)).thenReturn(List.of());
		assertThat(service.availability("Goal", 3L).reason())
				.isEqualTo("AI review of goals is switched off for this project.");
	}

	@Test
	void anUnavailableRequestIsRefusedAndDispatchesNothing() {
		ai.setEnabled(false);
		assertThatThrownBy(() -> service.requestReview("Goal", 3L))
				.isInstanceOf(AiReviewUnavailableException.class)
				.hasMessage(AiReviewService.AI_OFF);
		verify(dispatcher, never()).dispatch(any(), any(), any());
	}

	@Test
	void aRequestReturnsTheDispatchedRunIds() {
		when(dispatcher.dispatch(goal, admin, AiReviewService.TASK_TYPE)).thenReturn("run-1");
		when(registry.policiesFor(goal, 7L)).thenReturn(List.of(mock(AssistantDefinition.class)));
		when(dispatcher.dispatch(goal, admin, AiReviewService.POLICY_TASK_TYPE)).thenReturn("run-2");

		assertThat(service.requestReview("Goal", 3L))
				.isEqualTo(new com.rreganjr.requel.service.api.dto.AiReviewRequestDto("run-1", "run-2"));
	}

	@Test
	void aRunStillQueuedOrRunningIsReturnedInsteadOfDispatchingAnother() {
		for (String status : List.of("QUEUED", "RUNNING")) {
			when(runs.latestRun("Goal", 3L, AiReviewService.TASK_TYPE))
					.thenReturn(Optional.of(run("in-flight", status)));
			assertThat(service.requestReview("Goal", 3L).runId()).isEqualTo("in-flight");
		}
		verify(dispatcher, never()).dispatch(any(), any(), eq(AiReviewService.TASK_TYPE));

		when(runs.latestRun("Goal", 3L, AiReviewService.TASK_TYPE))
				.thenReturn(Optional.of(run("done", "SUCCEEDED")));
		when(dispatcher.dispatch(goal, admin, AiReviewService.TASK_TYPE)).thenReturn("fresh");
		assertThat(service.requestReview("Goal", 3L).runId()).isEqualTo("fresh");
	}

	@Test
	void aTypeThatIsNotReviewableIsABadRequest() {
		assertThatThrownBy(() -> service.availability("Report", 3L))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private static AssistantRunReadService.RunView run(String id, String status) {
		return new AssistantRunReadService.RunView(id, "a", status, null, null, null, null, null,
				null, 0, List.of(), 0, null, null, 0);
	}
}
