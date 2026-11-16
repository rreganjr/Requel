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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.command.AnalysisRequestSource;
import com.rreganjr.requel.project.command.AnalyzeProjectCommand;

/** Issue #268: the whole-project path of the handler; #455: the step-sequence re-checks. */
class AnalysisInvokingCommandHandlerTest {

	private final CommandHandler inner = mock(CommandHandler.class);
	private final AnalysisRequestDispatcher dispatcher = mock(AnalysisRequestDispatcher.class);
	private final AnalysisInvokingCommandHandler handler = new AnalysisInvokingCommandHandler(inner,
			dispatcher);
	private final AnalyzeProjectCommand command = mock(AnalyzeProjectCommand.class);

	@Test
	void aProjectCommandDispatchesTheWholeProject() throws Exception {
		Project project = mock(Project.class);
		User ron = mock(User.class);
		when(command.getAnalysisProject()).thenReturn(project);
		when(command.getAnalysisTriggeredBy()).thenReturn(ron);
		when(inner.execute(command)).thenReturn(command);

		assertThat(handler.execute(command)).isSameAs(command);
		verify(dispatcher).dispatchProject(project, ron);
	}

	@Test
	void aProjectCommandWithoutAProjectDispatchesNothing() throws Exception {
		when(inner.execute(command)).thenReturn(command);

		handler.execute(command);

		verify(dispatcher, never()).dispatchProject(any(), any());
	}

	/** A failed dispatch never fails the command, which has already committed. */
	@Test
	void aFailedProjectDispatchIsLoggedNotThrown() throws Exception {
		Project project = mock(Project.class);
		when(command.getAnalysisProject()).thenReturn(project);
		when(inner.execute(command)).thenReturn(command);
		when(dispatcher.dispatchProject(any(), any())).thenThrow(new IllegalStateException("down"));

		assertThat(handler.execute(command)).isSameAs(command);
	}

	/** #455: a command that is an analysis source, as the step and scenario commands are. */
	interface SourceCommand extends Command, AnalysisRequestSource {
	}

	@Test
	void stepSequenceTargetsAreDispatchedWithTheStepSequenceTask() throws Exception {
		SourceCommand edit = mock(SourceCommand.class);
		Step step = mock(Step.class);
		Scenario first = mock(Scenario.class);
		Scenario second = mock(Scenario.class);
		User ron = mock(User.class);
		when(edit.getAnalysisTarget()).thenReturn(step);
		when(edit.getAnalysisTriggeredBy()).thenReturn(ron);
		when(edit.getStepSequenceTargets()).thenReturn(List.of(first, second));
		when(inner.execute(edit)).thenReturn(edit);

		handler.execute(edit);

		verify(dispatcher).dispatch(step, ron);
		verify(dispatcher).dispatch(first, ron, AnalysisRequestSource.STEP_SEQUENCE_TASK);
		verify(dispatcher).dispatch(second, ron, AnalysisRequestSource.STEP_SEQUENCE_TASK);
	}

	/** A delete analyzes nothing itself but still re-checks the scenarios that lost the step. */
	@Test
	void stepSequenceTargetsAreDispatchedWithoutAnAnalysisTarget() throws Exception {
		SourceCommand delete = mock(SourceCommand.class);
		Scenario scenario = mock(Scenario.class);
		User ron = mock(User.class);
		when(delete.getAnalysisTriggeredBy()).thenReturn(ron);
		when(delete.getStepSequenceTargets()).thenReturn(List.of(scenario));
		when(inner.execute(delete)).thenReturn(delete);

		handler.execute(delete);

		verify(dispatcher, never()).dispatch(any(), any());
		verify(dispatcher).dispatch(scenario, ron, AnalysisRequestSource.STEP_SEQUENCE_TASK);
	}

	@Test
	void aFailedStepSequenceDispatchIsLoggedAndTheRestStillGo() throws Exception {
		SourceCommand edit = mock(SourceCommand.class);
		Scenario failing = mock(Scenario.class);
		Scenario next = mock(Scenario.class);
		User ron = mock(User.class);
		when(edit.getAnalysisTriggeredBy()).thenReturn(ron);
		when(edit.getStepSequenceTargets()).thenReturn(List.of(failing, next));
		when(inner.execute(edit)).thenReturn(edit);
		doThrow(new IllegalStateException("down")).when(dispatcher).dispatch(eq(failing), any(),
				any());

		assertThat(handler.execute(edit)).isSameAs(edit);
		verify(dispatcher).dispatch(next, ron, AnalysisRequestSource.STEP_SEQUENCE_TASK);
	}
}
