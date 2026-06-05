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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.assistant.core.AssistantRunWorker;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunRepository;
import com.rreganjr.requel.assistant.core.persistence.AssistantUsageRepository;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;

/**
 * Phase 5 (issue #43): the manual REQUIREMENTS_REVIEW trigger dispatches a run for an entity
 * as the current user; with requel.ai.enabled=true the AI assistant runs (Noop provider ->
 * no findings) and the run completes. AI off by default, so this enables it for the test.
 */
@TestPropertySource(properties = "requel.ai.enabled=true")
public class AiReviewDispatchIT extends AbstractIntegrationTestCase {

	private AiReviewService aiReviewService;
	private AssistantRunWorker assistantRunWorker;
	private AssistantRunRepository assistantRunRepository;
	private AssistantUsageRepository assistantUsageRepository;

	@Autowired
	protected void setAiReviewService(AiReviewService aiReviewService) {
		this.aiReviewService = aiReviewService;
	}

	@Autowired
	protected void setAssistantUsageRepository(AssistantUsageRepository assistantUsageRepository) {
		this.assistantUsageRepository = assistantUsageRepository;
	}

	@Autowired
	protected void setAssistantRunWorker(AssistantRunWorker assistantRunWorker) {
		this.assistantRunWorker = assistantRunWorker;
	}

	@Autowired
	protected void setAssistantRunRepository(AssistantRunRepository assistantRunRepository) {
		this.assistantRunRepository = assistantRunRepository;
	}

	@AfterEach
	public void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	public void manualReviewDispatchesRequirementsReviewRunThatSucceeds() throws Exception {
		long ts = System.currentTimeMillis();
		User creator = getUserRepository().findUserByUsername("project");

		EditProjectCommand projectCommand = getProjectCommandFactory().newEditProjectCommand();
		projectCommand.setEditedBy(creator);
		projectCommand.setName("AiReview Project " + ts);
		projectCommand.setOrganizationName("AiReview Org " + ts);
		projectCommand = getCommandHandler().execute(projectCommand);
		Project project = projectCommand.getProject();

		EditGoalCommand goalCommand = getProjectCommandFactory().newEditGoalCommand();
		goalCommand.setEditedBy(creator);
		goalCommand.setGoalContainer(project);
		goalCommand.setName("Reviewable goal " + ts);
		goalCommand.setText("Users can log in.");
		goalCommand = getCommandHandler().execute(goalCommand);
		Goal goal = goalCommand.getGoal();

		// Trigger as the project creator (a stakeholder -> authorized).
		authenticateAs("project");
		aiReviewService.requestReview("Goal", goal.getId());

		AssistantRunEntity queued = assistantRunRepository.findAll().stream()
				.filter(run -> "Goal".equals(run.getTargetType())
						&& goal.getId().equals(run.getTargetId())
						&& "QUEUED".equals(run.getStatus()))
				.reduce((first, second) -> second)
				.orElseThrow(() -> new AssertionError("no QUEUED review run for the goal"));
		assertEquals("REQUIREMENTS_REVIEW", queued.getTaskType(),
				"the manual trigger should set the run's task type");

		assistantRunWorker.runInNewTransaction(queued.getRunId());

		AssistantRunEntity completed = assistantRunRepository.findById(queued.getId())
				.orElseThrow(() -> new AssertionError("review run row vanished"));
		assertEquals("SUCCEEDED", completed.getStatus(),
				"the AI requirements review run (Noop provider) should succeed");

		// Usage telemetry is recorded for the run (Noop reports a usage row).
		assertEquals(false,
				assistantUsageRepository.findByRunId(queued.getRunId().toString()).isEmpty(),
				"an assistant_usages row should be recorded for the review run");
	}

	@Test
	public void requestReviewRequiresAuthentication() {
		SecurityContextHolder.clearContext();
		assertThrows(IllegalStateException.class,
				() -> aiReviewService.requestReview("Goal", 1L));
	}

	private static void authenticateAs(String username) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(username, null, List.of()));
	}
}
