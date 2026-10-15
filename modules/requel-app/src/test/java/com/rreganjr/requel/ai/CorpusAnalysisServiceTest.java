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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.core.corpus.CorpusFinderAssistant;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunReadService;
import com.rreganjr.requel.command.AnalysisRequestDispatcher;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;

/** Issue #266: validating a corpus run's set, mode and access before it is dispatched or read. */
class CorpusAnalysisServiceTest {

	private final ProjectRepository projects = mock(ProjectRepository.class);
	private final CurrentUserResolver users = mock(CurrentUserResolver.class);
	private final AnalysisRequestDispatcher dispatcher = mock(AnalysisRequestDispatcher.class);
	private final AssistantRunReadService runs = mock(AssistantRunReadService.class);
	private final User member = mock(User.class);
	private Project project;
	private CorpusAnalysisService service;

	@BeforeEach
	void setUp() {
		project = project(7L);
		UserStakeholder stakeholder = mock(UserStakeholder.class);
		when(stakeholder.matchesUser(member)).thenReturn(true);
		when(project.getStakeholders()).thenReturn(Set.of(stakeholder));
		when(users.resolve()).thenReturn(member);
		service = new CorpusAnalysisService(projects, users, dispatcher, runs,
				mock(PlatformTransactionManager.class));
	}

	private Project project(Long id) {
		Project project = mock(Project.class);
		when(project.getId()).thenReturn(id);
		when(projects.findById(Project.class, id)).thenReturn(project);
		return project;
	}

	@Test
	void aProjectSetIsTheDefault() {
		service.request(7L, null, null, "candidates");
		service.request(7L, " project ", 99L, "ANALYSIS");

		verify(dispatcher).dispatchCorpus(EntityRef.of("Project", 7L), 7L, member,
				CorpusFinderAssistant.TASK_TYPE);
		verify(dispatcher).dispatchCorpus(EntityRef.of("Project", 7L), 7L, member,
				CorpusAnalysisService.CORPUS_REVIEW);
	}

	@Test
	void aGoalOrUseCaseOfTheProjectRootsItsSet() {
		Goal goal = mock(Goal.class);
		when(goal.getId()).thenReturn(3L);
		when(goal.getProjectOrDomain()).thenReturn(project);
		when(projects.findById(Goal.class, 3L)).thenReturn(goal);
		UseCase useCase = mock(UseCase.class);
		when(useCase.getId()).thenReturn(4L);
		when(useCase.getProjectOrDomain()).thenReturn(project);
		when(projects.findById(UseCase.class, 4L)).thenReturn(useCase);

		service.request(7L, "goal", 3L, "CANDIDATES");
		service.request(7L, "USE_CASE", 4L, "CANDIDATES");

		verify(dispatcher).dispatchCorpus(EntityRef.of("Goal", 3L), 7L, member,
				CorpusFinderAssistant.TASK_TYPE);
		verify(dispatcher).dispatchCorpus(EntityRef.of("UseCase", 4L), 7L, member,
				CorpusFinderAssistant.TASK_TYPE);
	}

	@Test
	void aBadModeOrSetOrRootIsRefused() {
		assertThatThrownBy(() -> service.request(7L, "PROJECT", null, "everything"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Unknown corpus mode: everything (known: CANDIDATES, ANALYSIS)");
		assertThatThrownBy(() -> service.request(7L, "PROJECT", null, null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> service.request(7L, "STORY", 1L, "CANDIDATES"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageStartingWith("Unknown corpus set: STORY");
		assertThatThrownBy(() -> service.request(7L, "GOAL", null, "CANDIDATES"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("A GOAL set needs rootId");
		verifyNoInteractions(dispatcher);
	}

	@Test
	void aRootInAnotherProjectOrInADomainIsRefused() {
		Project other = project(8L);
		Goal elsewhere = mock(Goal.class);
		when(elsewhere.getProjectOrDomain()).thenReturn(other);
		when(projects.findById(Goal.class, 3L)).thenReturn(elsewhere);
		Goal unowned = mock(Goal.class);
		when(projects.findById(Goal.class, 5L)).thenReturn(unowned);

		assertThatThrownBy(() -> service.request(7L, "GOAL", 3L, "CANDIDATES"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Goal 3 is not in project 7");
		assertThatThrownBy(() -> service.request(7L, "GOAL", 5L, "CANDIDATES"))
				.isInstanceOf(IllegalArgumentException.class);
		verifyNoInteractions(dispatcher);
	}

	@Test
	void onlyAUserStakeholderOrAnAdminHasAccess() {
		User stranger = mock(User.class);
		Stakeholder nonUser = mock(Stakeholder.class);
		when(nonUser.matchesUser(stranger)).thenReturn(true);
		when(project.getStakeholders()).thenReturn(Set.of(nonUser));
		when(users.resolve()).thenReturn(stranger);

		assertThatThrownBy(() -> service.request(7L, "PROJECT", null, "CANDIDATES"))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> service.latest(7L, "PROJECT", null, "CANDIDATES"))
				.isInstanceOf(AuthorizationException.class);

		when(stranger.hasRole(SystemAdminUserRole.class)).thenReturn(true);
		service.request(7L, "PROJECT", null, "CANDIDATES");
		verify(dispatcher).dispatchCorpus(any(), any(), any(), any());
	}

	@Test
	void theLatestRunIsReadForTheRootAndTask() {
		AssistantRunReadService.RunView run = new AssistantRunReadService.RunView("run-1",
				"ai-corpus-relationships", "COMPLETED", null, null, null, null, null, null, 0,
				java.util.List.of(), 0, null, null, 0);
		when(runs.latestRun("Project", 7L, CorpusAnalysisService.CORPUS_REVIEW))
				.thenReturn(Optional.of(run));

		assertThat(service.latest(7L, "PROJECT", null, "analysis")).contains(run);
		assertThat(service.latest(7L, "PROJECT", null, "candidates")).isEmpty();
		assertThat(CorpusAnalysisService.Mode.ANALYSIS.taskType())
				.isEqualTo(CorpusAnalysisService.CORPUS_REVIEW);
	}
}
