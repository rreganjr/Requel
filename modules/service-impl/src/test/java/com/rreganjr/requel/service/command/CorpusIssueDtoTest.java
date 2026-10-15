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
package com.rreganjr.requel.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.SwitchableAssistantCatalog;
import com.rreganjr.requel.project.SwitchableAssistantCatalog.SwitchableAssistant;
import com.rreganjr.requel.service.api.dto.AnnotationSubjectDto;
import com.rreganjr.requel.service.api.dto.IssueDto;

/**
 * Issue #266: an issue raised by a corpus run reads as kind CORPUS and names every entity it is
 * on.
 */
class CorpusIssueDtoTest {

	@AfterEach
	void forgetTheCatalog() {
		new AnnotationSources(null);
	}

	private static void catalog(SwitchableAssistant... assistants) {
		// the catalog's own defaults decide the project-aware lookups (#264)
		SwitchableAssistantCatalog catalog = mock(SwitchableAssistantCatalog.class,
				org.mockito.Mockito.CALLS_REAL_METHODS);
		for (SwitchableAssistant assistant : assistants) {
			doReturn(Optional.of(assistant)).when(catalog).describe(assistant.assistantId());
		}
		@SuppressWarnings("unchecked")
		ObjectProvider<SwitchableAssistantCatalog> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(catalog);
		new AnnotationSources(provider);
	}

	private static <T extends ProjectOrDomainEntity> T entity(Class<T> type, Long id, String name) {
		T entity = mock(type);
		when(entity.getId()).thenReturn(id);
		when(entity.getName()).thenReturn(name);
		doReturn(type).when(entity).getProjectOrDomainEntityInterface();
		return entity;
	}

	private static Issue issue(String source, Annotatable... on) {
		Issue issue = mock(Issue.class);
		when(issue.getSource()).thenReturn(source);
		when(issue.getPositions()).thenReturn(new java.util.TreeSet<>());
		when(issue.getAnnotatables()).thenReturn(new LinkedHashSet<>(List.of(on)));
		return issue;
	}

	@Test
	void eachAssistantGroupReadsAsItsKind() {
		catalog(new SwitchableAssistant("corpus-finder", "Find overlaps",
				SwitchableAssistantCatalog.CORPUS),
				new SwitchableAssistant("p", "Policy", SwitchableAssistantCatalog.POLICIES),
				new SwitchableAssistant("r", "Review", SwitchableAssistantCatalog.AI_REVIEW),
				new SwitchableAssistant("l", "Lexical"));

		assertThat(AnnotationSources.kind("ASSISTANT:corpus-finder")).isEqualTo("CORPUS");
		assertThat(AnnotationSources.name("ASSISTANT:corpus-finder")).isEqualTo("Find overlaps");
		assertThat(AnnotationSources.kind("ASSISTANT:p")).isEqualTo("POLICY");
		assertThat(AnnotationSources.kind("ASSISTANT:r")).isEqualTo("REVIEW");
		assertThat(AnnotationSources.kind("ASSISTANT:l")).isEqualTo("LEXICAL");
		assertThat(AnnotationSources.kind("ASSISTANT:unknown")).isNull();
		assertThat(AnnotationSources.kind("ron")).isNull();
	}

	@Test
	void withoutACatalogNothingIsKnown() {
		@SuppressWarnings("unchecked")
		ObjectProvider<SwitchableAssistantCatalog> empty = mock(ObjectProvider.class);
		new AnnotationSources(empty);
		assertThat(AnnotationSources.kind("ASSISTANT:corpus-finder")).isNull();

		@SuppressWarnings("unchecked")
		ObjectProvider<SwitchableAssistantCatalog> failing = mock(ObjectProvider.class);
		when(failing.getIfAvailable()).thenThrow(new IllegalStateException("closing"));
		new AnnotationSources(failing);
		assertThat(AnnotationSources.name("ASSISTANT:corpus-finder")).isNull();
	}

	@Test
	void anIssueOnSeveralEntitiesNamesThemAllInTypeAndIdOrder() {
		catalog(new SwitchableAssistant("corpus-finder", "Find overlaps",
				SwitchableAssistantCatalog.CORPUS));
		Issue shared = issue("ASSISTANT:corpus-finder", entity(Story.class, 3L, "Borrowing"),
				entity(Goal.class, 9L, "Track loans"), entity(Goal.class, 2L, "Lend books"),
				entity(Goal.class, null, "unsaved"), mock(Annotatable.class));

		IssueDto dto = AnnotationCommandRegistrar.toIssueDto(shared, false);

		assertThat(dto.sourceKind()).isEqualTo("CORPUS");
		assertThat(dto.subjects()).containsExactly(new AnnotationSubjectDto("Goal", 2L, "Lend books"),
				new AnnotationSubjectDto("Goal", 9L, "Track loans"),
				new AnnotationSubjectDto("Story", 3L, "Borrowing"));
	}

	/** #264: an issue a project's own definition raised is labelled from that project. */
	@Test
	void aProjectDefinitionsIssueIsLabelledFromItsProject() {
		SwitchableAssistantCatalog catalog = mock(SwitchableAssistantCatalog.class);
		when(catalog.describe("house-style", 7L)).thenReturn(Optional.of(new SwitchableAssistant(
				"house-style", "House style", SwitchableAssistantCatalog.POLICIES)));
		when(catalog.describe("house-style", null)).thenReturn(Optional.empty());
		@SuppressWarnings("unchecked")
		ObjectProvider<SwitchableAssistantCatalog> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(catalog);
		new AnnotationSources(provider);
		com.rreganjr.requel.project.Project project = mock(com.rreganjr.requel.project.Project.class);
		when(project.getId()).thenReturn(7L);
		Goal goal = entity(Goal.class, 2L, "Lend books");
		when(goal.getProjectOrDomain()).thenReturn(project);

		IssueDto dto = AnnotationCommandRegistrar.toIssueDto(issue("ASSISTANT:house-style", goal),
				false);

		assertThat(dto.sourceName()).isEqualTo("House style");
		assertThat(dto.sourceKind()).isEqualTo("POLICY");
		assertThat(AnnotationSources.kind("ASSISTANT:house-style")).isNull();
		Issue loose = issue(null, mock(Annotatable.class));
		assertThat(AnnotationCommandRegistrar.projectId(loose)).isNull();
		Issue none = mock(Issue.class);
		assertThat(AnnotationCommandRegistrar.projectId(none)).isNull();
	}

	@Test
	void anIssueOnOneEntityNamesNone() {
		assertThat(AnnotationCommandRegistrar.subjects(issue(null, entity(Goal.class, 2L, "a"))))
				.isEmpty();
		// two annotatables, but only one is an entity
		assertThat(AnnotationCommandRegistrar.subjects(issue(null, entity(Goal.class, 2L, "a"),
				mock(Annotatable.class)))).isEmpty();
		Issue none = mock(Issue.class);
		when(none.getAnnotatables()).thenReturn(null);
		assertThat(AnnotationCommandRegistrar.subjects(none)).isEmpty();
	}
}
