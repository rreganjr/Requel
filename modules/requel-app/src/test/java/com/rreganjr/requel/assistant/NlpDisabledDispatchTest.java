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
package com.rreganjr.requel.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunRepository;
import com.rreganjr.requel.assistant.core.AssistantRunWorker;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;

/**
 * NLP-optional Scope 1 (issue #43, Phase 4.5 Step 7): when {@code requel.nlp.enabled=false},
 * the legacy NLP assistants are gated off ({@code @ConditionalOnProperty}). Editing a Goal
 * still dispatches an analysis run, but the worker finds no assistants registered for the
 * target and records the run as {@code SKIPPED} (with a reason) instead of succeeding with
 * findings — the SPI no-op contract for disabled NLP.
 */
@TestPropertySource(properties = "requel.nlp.enabled=false")
public class NlpDisabledDispatchTest extends AbstractIntegrationTestCase {

	private AssistantRunWorker assistantRunWorker;
	private AssistantRunRepository assistantRunRepository;

	@Autowired
	protected void setAssistantRunWorker(AssistantRunWorker assistantRunWorker) {
		this.assistantRunWorker = assistantRunWorker;
	}

	@Autowired
	protected void setAssistantRunRepository(AssistantRunRepository assistantRunRepository) {
		this.assistantRunRepository = assistantRunRepository;
	}

	@Test
	public void goalAnalysisRunIsSkippedWhenNlpDisabled() throws Exception {
		long ts = System.currentTimeMillis();
		User creator = getUserRepository().findUserByUsername("project");

		EditProjectCommand projectCommand = getProjectCommandFactory().newEditProjectCommand();
		projectCommand.setEditedBy(creator);
		projectCommand.setName("NlpDisabled Project " + ts);
		projectCommand.setOrganizationName("NlpDisabled Org " + ts);
		projectCommand = getCommandHandler().execute(projectCommand);
		Project project = projectCommand.getProject();

		EditGoalCommand goalCommand = getProjectCommandFactory().newEditGoalCommand();
		goalCommand.setEditedBy(creator);
		goalCommand.setGoalContainer(project);
		goalCommand.setName("Some groal " + ts); // would be flagged if NLP were enabled
		goalCommand.setText("a clear requirement.");
		goalCommand = getCommandHandler().execute(goalCommand);
		Goal goal = goalCommand.getGoal();

		// Editing the goal dispatched a run; the test executor discards the async task so it
		// stays QUEUED. Drive the worker synchronously.
		AssistantRunEntity queued = assistantRunRepository.findAll().stream()
				.filter(run -> "Goal".equals(run.getTargetType())
						&& goal.getId().equals(run.getTargetId())
						&& "QUEUED".equals(run.getStatus()))
				.reduce((first, second) -> second)
				.orElseThrow(() -> new AssertionError("no QUEUED assistant run for the goal"));
		assistantRunWorker.runInNewTransaction(queued.getRunId());

		AssistantRunEntity completed = assistantRunRepository.findById(queued.getId())
				.orElseThrow(() -> new AssertionError("assistant run row vanished"));
		assertEquals("SKIPPED", completed.getStatus(),
				"with NLP disabled no lexical assistants are registered, so the run is skipped");
		assertTrue(completed.getErrorSummary() != null
				&& completed.getErrorSummary().contains("No assistants registered"),
				"skip reason should explain that no assistants were registered, was: "
						+ completed.getErrorSummary());
	}
}
