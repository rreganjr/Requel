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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.rreganjr.requel.project.ProjectAssistantSettingsStore;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.service.api.dto.AiReviewAvailabilityDto;
import com.rreganjr.requel.service.api.dto.AiReviewRequestDto;

/**
 * Phase 5 (issue #43): the manual REQUIREMENTS_REVIEW trigger dispatches a run for an entity
 * as the current user; with requel.ai.enabled=true the AI assistant runs (Noop provider ->
 * no findings) and the run completes. AI off by default, so this enables it for the test.
 * Issue #387: the request returns its runs, doesn't queue another while one is in flight, and is
 * refused when the type's review is switched off in the project.
 */
@TestPropertySource(properties = "requel.ai.enabled=true")
public class AiReviewDispatchIT extends AbstractIntegrationTestCase {

	private AiReviewService aiReviewService;
	private AssistantRunWorker assistantRunWorker;
	private AssistantRunRepository assistantRunRepository;
	private AssistantUsageRepository assistantUsageRepository;

	@Autowired
	private ProjectAssistantSettingsStore assistantSettingsStore;

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

		java.util.List<AssistantRunEntity> queuedRuns = assistantRunRepository.findAll().stream()
				.filter(run -> "Goal".equals(run.getTargetType())
						&& goal.getId().equals(run.getTargetId())
						&& "QUEUED".equals(run.getStatus()))
				.toList();
		// #265: the bundled terminology policy applies to goals, so the policy pass is queued too.
		// (The goal's creation queued a lexical run, with no task type.)
		assertEquals(java.util.Set.of("REQUIREMENTS_REVIEW", "POLICY_REVIEW"), queuedRuns.stream()
				.map(AssistantRunEntity::getTaskType).filter(java.util.Objects::nonNull)
				.collect(java.util.stream.Collectors.toSet()),
				"the manual trigger should queue the review and the policy pass");
		AssistantRunEntity queued = queuedRuns.stream()
				.filter(run -> "REQUIREMENTS_REVIEW".equals(run.getTaskType())).findFirst()
				.orElseThrow(() -> new AssertionError("no QUEUED review run for the goal"));

		assistantRunWorker.run(queued.getRunId());

		AssistantRunEntity completed = assistantRunRepository.findById(queued.getId())
				.orElseThrow(() -> new AssertionError("review run row vanished"));
		assertEquals("SUCCEEDED", completed.getStatus(),
				"the AI requirements review run (Noop provider) should succeed");

		// Usage telemetry is recorded for the run (Noop reports a usage row).
		assertEquals(false,
				assistantUsageRepository.findByRunId(queued.getRunId().toString()).isEmpty(),
				"an assistant_usages row should be recorded for the review run");
	}

	// ---- #387: availability, the run ids, no duplicate while in flight -----------------------

	@Test
	public void aRequestReturnsTheQueuedRunsAndASecondOneWhileQueuedReturnsTheSame() throws Exception {
		Goal goal = newGoal("ids").goal();
		authenticateAs("project");

		AiReviewAvailabilityDto availability = aiReviewService.availability("Goal", goal.getId());
		assertTrue(availability.available(), "a goal review should be available: " + availability);
		assertNull(availability.reason());

		AiReviewRequestDto first = aiReviewService.requestReview("Goal", goal.getId());
		assertNotNull(first.runId());
		assertNotNull(first.policyRunId(), "the bundled terminology policy applies to goals");
		AssistantRunEntity review = runById(first.runId());
		assertEquals("REQUIREMENTS_REVIEW", review.getTaskType());
		assertEquals(goal.getId(), review.getTargetId());
		assertEquals("POLICY_REVIEW", runById(first.policyRunId()).getTaskType());

		// Still QUEUED: the same runs come back and nothing new is queued.
		long before = assistantRunRepository.count();
		assertEquals(first, aiReviewService.requestReview("Goal", goal.getId()));
		assertEquals(before, assistantRunRepository.count());

		// Once it has finished, a new request queues a new run.
		assistantRunWorker.run(review.getRunId());
		assistantRunWorker.run(runById(first.policyRunId()).getRunId());
		AiReviewRequestDto second = aiReviewService.requestReview("Goal", goal.getId());
		assertNotEquals(first.runId(), second.runId());
	}

	@Test
	public void aTypeWhoseReviewIsSwitchedOffInTheProjectIsUnavailableAndRefused() throws Exception {
		GoalInProject fixture = newGoal("off");
		Goal goal = fixture.goal();
		Long projectId = fixture.project().getId();
		User admin = getUserRepository().findUserByUsername("admin");
		assistantSettingsStore.setEnabled(projectId, "ai-review-goal", false, admin);
		authenticateAs("project");

		AiReviewAvailabilityDto availability = aiReviewService.availability("Goal", goal.getId());
		assertFalse(availability.available());
		assertEquals("AI review of goals is switched off for this project.", availability.reason());

		long before = assistantRunRepository.count();
		AiReviewUnavailableException e = assertThrows(AiReviewUnavailableException.class,
				() -> aiReviewService.requestReview("Goal", goal.getId()));
		assertEquals(availability.reason(), e.getMessage());
		assertEquals(before, assistantRunRepository.count(), "a refused request queues nothing");
	}

	@Test
	public void theProjectFormRefusesAnEntityFromAnotherProject() throws Exception {
		Goal goal = newGoal("scope").goal();
		String otherProject = newGoal("other").project().getName();
		authenticateAs("project");

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> aiReviewService.requestReview(otherProject, "Goal", goal.getId()));
		assertTrue(e.getMessage().contains("belongs to another project"), e.getMessage());
		assertThrows(IllegalArgumentException.class,
				() -> aiReviewService.latestReview(otherProject, "Goal", goal.getId(), null));
	}

	private AssistantRunEntity runById(String runId) {
		return assistantRunRepository.findAll().stream()
				.filter(run -> run.getRunId().toString().equals(runId)).findFirst()
				.orElseThrow(() -> new AssertionError("no run " + runId));
	}

	private record GoalInProject(Goal goal, Project project) {
	}

	private GoalInProject newGoal(String label) throws Exception {
		long ts = System.nanoTime();
		User creator = getUserRepository().findUserByUsername("project");
		EditProjectCommand projectCommand = getProjectCommandFactory().newEditProjectCommand();
		projectCommand.setEditedBy(creator);
		projectCommand.setName("AiReview " + label + " " + ts);
		projectCommand.setOrganizationName("AiReview Org " + ts);
		Project project = getCommandHandler().execute(projectCommand).getProject();
		EditGoalCommand goalCommand = getProjectCommandFactory().newEditGoalCommand();
		goalCommand.setEditedBy(creator);
		goalCommand.setGoalContainer(project);
		goalCommand.setName("Reviewable goal " + ts);
		goalCommand.setText("Users can log in.");
		return new GoalInProject(getCommandHandler().execute(goalCommand).getGoal(), project);
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
