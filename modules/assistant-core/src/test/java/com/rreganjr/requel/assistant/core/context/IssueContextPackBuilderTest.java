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
package com.rreganjr.requel.assistant.core.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;

class IssueContextPackBuilderTest {

	private final Clock fixedClock = Clock.fixed(Instant.parse("2026-05-27T12:00:00Z"),
			ZoneOffset.UTC);
	private final IssueContextPackBuilder builder = new IssueContextPackBuilder(
			new NoOpRedactionPolicy(), new ContextPackSizeLimits(), fixedClock);

	@Test
	void buildsPackWithTargetIssuesAndProjectSweep() {
		Issue targetOpen = stubIssue("ambiguous", false);
		Issue targetResolved = stubIssue("resolved already", true);
		Goal target = mock(Goal.class);
		when(target.getId()).thenReturn(42L);
		when(target.getVersion()).thenReturn(1);
		when(target.getAnnotations())
				.thenReturn(orderedSet(targetOpen, targetResolved));
		when(target.getProjectOrDomainEntityInterface())
				.thenAnswer(invocation -> Goal.class);

		Issue otherIssue = stubIssue("missing actor", false);
		Goal otherGoal = mock(Goal.class);
		when(otherGoal.getId()).thenReturn(100L);
		when(otherGoal.getVersion()).thenReturn(1);
		when(otherGoal.getAnnotations()).thenReturn(orderedSet(otherIssue));
		when(otherGoal.getProjectOrDomainEntityInterface())
				.thenAnswer(invocation -> Goal.class);

		Project project = mock(Project.class);
		when(project.getId()).thenReturn(7L);
		when(project.getVersion()).thenReturn(1);
		when(project.getAnnotations()).thenReturn(Set.of());
		Set<ProjectOrDomainEntity> entities = new LinkedHashSet<>();
		entities.add(target);
		entities.add(otherGoal);
		when(project.getProjectEntities()).thenReturn(entities);

		IssueContextPack pack = builder.build(project, target);

		assertThat(pack.target().entityType()).isEqualTo("Goal");
		assertThat(pack.target().entityId()).isEqualTo(42L);
		assertThat(pack.targetIssues()).extracting(IssueSnapshot::text)
				.containsExactly("ambiguous");
		assertThat(pack.projectOpenIssues()).extracting(IssueSnapshot::text)
				.containsExactly("missing actor");
		assertThat(pack.projectOpenIssues().get(0).target().entityType()).isEqualTo("Goal");
	}

	private static Issue stubIssue(String text, boolean resolved) {
		Issue issue = mock(Issue.class);
		when(issue.getText()).thenReturn(text);
		when(issue.isResolved()).thenReturn(resolved);
		when(issue.isMustBeResolved()).thenReturn(false);
		when(issue.getPositions()).thenReturn(Set.<Position>of());
		return issue;
	}

	private static LinkedHashSet<Annotation> orderedSet(Annotation... annotations) {
		LinkedHashSet<Annotation> set = new LinkedHashSet<>();
		for (Annotation a : annotations) {
			set.add(a);
		}
		return set;
	}
}
