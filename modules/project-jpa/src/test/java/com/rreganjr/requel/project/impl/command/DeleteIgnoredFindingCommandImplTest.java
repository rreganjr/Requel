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
package com.rreganjr.requel.project.impl.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.AuthorizationExemptable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.IgnoredFinding;
import com.rreganjr.requel.project.IgnoredFindingStore;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;

/** Issue #268: un-ignoring a glossary candidate un-ignores it on every entity. */
class DeleteIgnoredFindingCommandImplTest {

	private static final String GLOSSARY = "legacy-lexical-glossary-term";
	private static final String SUFFIX = "glossary-term:admin console";

	private final ProjectRepository projectRepository = mock(ProjectRepository.class);
	private final AnnotationCommandFactory annotationCommandFactory = mock(
			AnnotationCommandFactory.class);
	private final CommandHandler commandHandler = mock(CommandHandler.class);
	private final IgnoredFindingStore store = mock(IgnoredFindingStore.class);
	private final Project project = mock(Project.class);

	@Test
	void onlyTheSameAssistantsIgnoresOfTheSamePhraseAreRemoved() throws Exception {
		when(project.getId()).thenReturn(7L);
		IgnoredFinding ignored = finding(1L, GLOSSARY, SUFFIX, "Goal", 10L, null);
		when(store.find(7L, 1L)).thenReturn(Optional.of(ignored));
		Goal target = goal(10L, null);
		Goal withIssue = goal(11L, 60L);
		goal(12L, null);
		List<IgnoredFinding> all = List.of(ignored,
				finding(2L, "legacy-lexical", SUFFIX, "Goal", 10L, null),
				finding(3L, GLOSSARY, null, "Goal", 10L, null),
				finding(4L, GLOSSARY, "glossary-term:stream key", "Goal", 10L, null),
				finding(5L, GLOSSARY, "Glossary-Term:Admin Console", "Nope", 13L, 61L),
				finding(6L, GLOSSARY, SUFFIX, "Goal", 11L, 60L),
				finding(7L, GLOSSARY, SUFFIX, "Goal", 12L, null));
		when(store.list(7L)).thenReturn(all);
		RemoveAnnotationFromAnnotatableCommand remove = mock(
				RemoveAnnotationFromAnnotatableCommand.class,
				withSettings().extraInterfaces(AuthorizationExemptable.class));
		when(annotationCommandFactory.newRemoveAnnotationFromAnnotatableCommand()).thenReturn(remove);

		DeleteIgnoredFindingCommandImpl command = new DeleteIgnoredFindingCommandImpl(null, null,
				projectRepository, null, annotationCommandFactory, commandHandler);
		command.setIgnoredFindingStore(store);
		command.setProject(project);
		command.setIgnoredFindingId(1L);
		command.execute();

		verify(store).delete(7L, 1L);
		verify(store).delete(7L, 5L);
		verify(store).delete(7L, 6L);
		verify(store).delete(7L, 7L);
		verify(store, times(4)).delete(org.mockito.ArgumentMatchers.eq(7L), anyLong());
		verify(store, never()).delete(7L, 2L);
		verify(remove).setAnnotatable(withIssue);
		verify(commandHandler, times(1)).execute(remove);
		assertThat(command.getAnalysisTarget()).isSameAs(target);
	}

	private Goal goal(Long id, Long annotationId) {
		Goal goal = mock(Goal.class);
		if (annotationId != null) {
			Annotation annotation = mock(Annotation.class);
			when(annotation.getId()).thenReturn(annotationId);
			doReturn(Set.of(annotation)).when(goal).getAnnotations();
		}
		doReturn(goal).when(projectRepository).findById(Goal.class, id);
		return goal;
	}

	private static IgnoredFinding finding(Long id, String assistantId, String suffix,
			String targetType, Long targetId, Long annotationId) {
		IgnoredFinding finding = mock(IgnoredFinding.class);
		when(finding.getId()).thenReturn(id);
		when(finding.getAssistantId()).thenReturn(assistantId);
		when(finding.getKeySuffix()).thenReturn(suffix);
		when(finding.getFindingType()).thenReturn("glossary-term");
		when(finding.getTargetType()).thenReturn(targetType);
		when(finding.getTargetId()).thenReturn(targetId);
		when(finding.getAnnotationId()).thenReturn(annotationId);
		return finding;
	}
}
