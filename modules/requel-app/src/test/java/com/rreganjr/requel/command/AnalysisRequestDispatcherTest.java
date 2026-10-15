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
package com.rreganjr.requel.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.assistant.api.AnalysisRequest;
import com.rreganjr.requel.assistant.api.AssistantDispatcher;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ReportGenerator;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.user.UserRepository;

/** Issue #268: which entities a whole-project analysis dispatches. */
class AnalysisRequestDispatcherTest {

	private final AssistantDispatcher assistantDispatcher = mock(AssistantDispatcher.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final ProjectRepository projectRepository = mock(ProjectRepository.class);
	private final AnalysisRequestDispatcher dispatcher = new AnalysisRequestDispatcher(
			assistantDispatcher, userRepository, projectRepository,
			mock(PlatformTransactionManager.class));
	private final User ron = user(3L, "ron");

	AnalysisRequestDispatcherTest() {
		com.rreganjr.requel.user.User assistant = mock(com.rreganjr.requel.user.User.class);
		when(assistant.getId()).thenReturn(11L);
		when(assistant.getUsername()).thenReturn("assistant");
		when(userRepository.findUserByUsername("assistant")).thenReturn(assistant);
	}

	@Test
	void noProjectIdOrUserDispatchesNothing() {
		Project unsaved = mock(Project.class);
		when(unsaved.getId()).thenReturn(null);

		assertThat(dispatcher.dispatchProject(null, ron)).isZero();
		assertThat(dispatcher.dispatchProject(unsaved, ron)).isZero();
		assertThat(dispatcher.dispatchProject(project(7L, null), null)).isZero();
		verifyNoInteractions(assistantDispatcher, projectRepository);
	}

	@Test
	void aProjectWithNothingToAnalyzeDispatchesNothing() {
		Project project = project(7L, null);

		assertThat(dispatcher.dispatchProject(project, ron)).isZero();
		verify(assistantDispatcher, never()).dispatchAll(any());
	}

	/**
	 * Each analyzable entity once, by type then id; report generators and unsaved entities are
	 * left out, and a step in both lists is dispatched once.
	 */
	@Test
	void eachAnalyzableEntityIsDispatchedOnceInTypeOrder() {
		Step step = entity(Step.class, 4L);
		Set<ProjectOrDomainEntity> entities = new LinkedHashSet<>(List.of(entity(Story.class, 5L),
				entity(Goal.class, 2L), entity(ReportGenerator.class, 9L), entity(Goal.class, null),
				entity(Goal.class, 1L), step));
		Project project = project(7L, entities);
		doReturn(Set.of(step)).when(projectRepository).findStepsByProjectOrDomain(project);

		assertThat(dispatcher.dispatchProject(project, ron)).isEqualTo(4);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<AnalysisRequest>> requests = ArgumentCaptor.forClass(List.class);
		verify(assistantDispatcher).dispatchAll(requests.capture());
		assertThat(requests.getValue()).extracting(AnalysisRequest::targetRef).containsExactly(
				EntityRef.of("Goal", 1L), EntityRef.of("Goal", 2L), EntityRef.of("Story", 5L),
				EntityRef.of("Step", 4L));
		assertThat(requests.getValue()).allSatisfy(request -> {
			assertThat(request.projectRef()).isEqualTo(EntityRef.of("Project", 7L));
			assertThat(request.triggeringUser().userId()).isEqualTo(3L);
			assertThat(request.assistantUser().userId()).isEqualTo(11L);
		});
	}

	/** #266: a corpus run is dispatched for its root with the explicit request's task type. */
	@Test
	void aCorpusRunIsDispatchedForItsRootAndTask() {
		dispatcher.dispatchCorpus(EntityRef.of("Goal", 4L), 7L, ron, "CORPUS_CANDIDATES");

		ArgumentCaptor<AnalysisRequest> request = ArgumentCaptor.forClass(AnalysisRequest.class);
		verify(assistantDispatcher).dispatch(request.capture());
		assertThat(request.getValue().targetRef()).isEqualTo(EntityRef.of("Goal", 4L));
		assertThat(request.getValue().projectRef()).isEqualTo(EntityRef.of("Project", 7L));
		assertThat(request.getValue().taskType()).isEqualTo("CORPUS_CANDIDATES");
		assertThat(request.getValue().triggeringUser().userId()).isEqualTo(3L);
		assertThat(request.getValue().assistantUser().userId()).isEqualTo(11L);
	}

	private Project project(Long id, Set<ProjectOrDomainEntity> entities) {
		Project project = mock(Project.class);
		when(project.getId()).thenReturn(id);
		when(projectRepository.get(project)).thenReturn(project);
		doReturn(entities).when(project).getProjectEntities();
		doReturn(Set.of()).when(projectRepository).findStepsByProjectOrDomain(project);
		return project;
	}

	private static <T extends ProjectOrDomainEntity> T entity(Class<T> type, Long id) {
		T entity = mock(type);
		doReturn(type).when(entity).getProjectOrDomainEntityInterface();
		when(entity.getId()).thenReturn(id);
		return entity;
	}

	private static User user(Long id, String username) {
		User user = mock(User.class);
		when(user.getId()).thenReturn(id);
		when(user.getUsername()).thenReturn(username);
		return user;
	}
}
