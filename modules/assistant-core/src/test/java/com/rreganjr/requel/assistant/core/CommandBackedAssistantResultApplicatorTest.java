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
package com.rreganjr.requel.assistant.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingRepository;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunRepository;
import com.rreganjr.requel.user.UserRepository;

/**
 * Unit coverage for the parts of {@link CommandBackedAssistantResultApplicator}
 * that do not need the full command stack: empty results and action types that
 * are intentionally skipped until the Step 6 finding state machine lands. The
 * create-or-update paths are exercised end-to-end by the Step 5 integration
 * test against the real command handler.
 */
class CommandBackedAssistantResultApplicatorTest {

	private final CommandHandler commandHandler = mock(CommandHandler.class);
	private final AnnotationCommandFactory annotationCommandFactory = mock(
			AnnotationCommandFactory.class);
	private final ProjectCommandFactory projectCommandFactory = mock(ProjectCommandFactory.class);
	private final AnnotationRepository annotationRepository = mock(AnnotationRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final AssistantFindingRepository findingRepository = mock(
			AssistantFindingRepository.class);
	private final AssistantRunRepository runRepository = mock(AssistantRunRepository.class);
	private final Clock fixedClock = Clock.fixed(Instant.parse("2026-05-29T00:00:00Z"),
			ZoneOffset.UTC);

	private CommandBackedAssistantResultApplicator newApplicator() {
		return new CommandBackedAssistantResultApplicator(commandHandler, annotationCommandFactory,
				projectCommandFactory, annotationRepository, userRepository, findingRepository,
				runRepository, List.of(), fixedClock);
	}

	@Test
	void emptyResultAppliesNothing() {
		AssistantResult result = AssistantResult.builder().assistantId("legacy-lexical")
				.summary("nothing to do").build();

		AppliedAssistantResult applied = newApplicator().apply(context(), result);

		assertThat(applied.appliedActionCount()).isZero();
		assertThat(applied.annotationIds()).isEmpty();
		verifyNoInteractions(commandHandler);
		verifyNoInteractions(annotationCommandFactory);
		verifyNoInteractions(findingRepository);
	}

	@Test
	void unsupportedActionTypesAreSkippedNotApplied() {
		AnnotationAction delete = new AnnotationAction("legacy-lexical:Goal:1:stale",
				AnnotationAction.ActionType.DELETE_NOTE, EntityRef.of("Goal", 1L), null,
				"obsolete", null, null, List.of(), Map.of());
		AssistantResult result = AssistantResult.builder().assistantId("legacy-lexical")
				.summary("one stale finding").annotationAction(delete).build();

		AppliedAssistantResult applied = newApplicator().apply(context(), result);

		assertThat(applied.appliedActionCount()).isZero();
		verifyNoInteractions(annotationCommandFactory);
		verifyNoInteractions(findingRepository);
	}

	private static AssistantContext context() {
		return new AssistantContext(UUID.randomUUID(), new UserRef(3L, "ron"),
				new UserRef(11L, "assistant"), EntityRef.of("Project", 7L), Locale.US,
				Clock.systemUTC(), Map.of());
	}
}
