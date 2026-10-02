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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.assistant.core.AssistantRunWorker;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinition;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinitionStore;
import com.rreganjr.requel.assistant.core.definition.DefinitionSource;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunRepository;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalContainer;
import com.rreganjr.requel.project.NonUserStakeholder;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.ScenarioType;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.command.AddActorToActorContainerCommand;
import com.rreganjr.requel.project.command.AddGoalToGoalContainerCommand;
import com.rreganjr.requel.project.command.AddScenarioToUseCaseCommand;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditGoalRelationCommand;
import com.rreganjr.requel.project.command.EditNonUserStakeholderCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditScenarioCommand;
import com.rreganjr.requel.project.command.EditScenarioStepCommand;
import com.rreganjr.requel.project.command.EditStoryCommand;
import com.rreganjr.requel.project.command.EditUseCaseCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.user.command.EditUserCommand;

/**
 * Issue #261 end to end: each context provider runs under {@link AssistantRunWorker} (so inside
 * the analyze transaction, with lazy associations), and what it adds reaches the prompt the fake
 * CLI receives. The project gets its own copy of the review definition naming the providers under
 * test, as #263 will for the bundled ones.
 */
@EnabledOnOs({ OS.LINUX, OS.MAC })
@TestPropertySource(properties = {
		"requel.ai.enabled=true",
		"requel.ai.provider=cli",
		"requel.ai.model=claude-cli",
		"spring.ai.model.chat=none",
		"requel.ai.cli.output-format=claude-json",
		"requel.ai.cli.timeout=30s" })
public class ContextProvidersIT extends AbstractIntegrationTestCase {

	private static final String REVIEW = "ai-requirements-review";
	private static final Path FAKE_CLI_DIR = createFakeCli();

	@DynamicPropertySource
	static void cliCommand(DynamicPropertyRegistry registry) {
		registry.add("requel.ai.cli.command", () -> FAKE_CLI_DIR.resolve("fake-claude").toString());
	}

	private AiReviewService aiReviewService;
	private AssistantRunWorker assistantRunWorker;
	private AssistantRunRepository assistantRunRepository;
	private AssistantDefinitionStore definitionStore;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private User admin;

	@Autowired
	protected void setDependencies(AiReviewService aiReviewService,
			AssistantRunWorker assistantRunWorker, AssistantRunRepository assistantRunRepository,
			AssistantDefinitionStore definitionStore) {
		this.aiReviewService = aiReviewService;
		this.assistantRunWorker = assistantRunWorker;
		this.assistantRunRepository = assistantRunRepository;
		this.definitionStore = definitionStore;
	}

	@BeforeEach
	public void replyWithNoFindings() throws IOException {
		admin = getUserRepository().findUserByUsername("admin");
		String reply = objectMapper.writeValueAsString(
				Map.of("summary", "none", "findings", List.of(), "warnings", List.of()));
		Files.writeString(FAKE_CLI_DIR.resolve("reply.json"), objectMapper.writeValueAsString(
				Map.of("type", "result", "is_error", false, "result", reply,
						"total_cost_usd", 0.001,
						"usage", Map.of("input_tokens", 100, "output_tokens", 20))));
	}

	@AfterEach
	public void reset() throws IOException {
		SecurityContextHolder.clearContext();
		Files.deleteIfExists(FAKE_CLI_DIR.resolve("last-prompt"));
	}

	@Test
	public void goalProvidersAddRelationsSiblingsAndStakeholders() throws Exception {
		Project project = newProject("CtxGoal");
		String n = stamp();
		Goal target = goal(project, "Renew tools online " + n, "Members renew borrowed tools.");
		goal(project, "Parent goal " + n, "Members keep tools longer. Mail ops@example.com.");
		goal(project, "Rival goal " + n, "Tools must come back within a week.");
		Goal held = goal(project, "Regulator goal " + n, "Records are kept for seven years.");
		relate(project, target, "Parent goal " + n, "Refines");
		relate(project, target, "Rival goal " + n, "Conflicts");
		NonUserStakeholder regulator = nonUserStakeholder(project, "Town council " + n,
				"Funds the library.");
		addGoal(regulator, target);
		addGoal(regulator, held);
		String username = "ctxuser" + n;
		createUser(username);
		UserStakeholder member = userStakeholder(project, username, "Volunteers");
		addGoal(member, target);
		useProviders(project, "goal-relations", "goal-siblings", "goal-stakeholders");

		String prompt = review("Goal", target);

		assertTrue(prompt.contains("goal-relations") && prompt.contains("goal-siblings")
				&& prompt.contains("goal-stakeholders"), prompt);
		assertTrue(prompt.contains("Refines") && prompt.contains("Parent goal " + n), prompt);
		assertTrue(prompt.contains("Conflicts") && prompt.contains("Rival goal " + n), prompt);
		assertTrue(prompt.contains("Town council " + n) && prompt.contains("also holds")
				&& prompt.contains("Regulator goal " + n), prompt);
		assertTrue(prompt.contains("(team Volunteers)"), "user stakeholder by role and team");
		assertFalse(prompt.contains(username), "a user stakeholder's username never reaches the model");
		assertFalse(prompt.contains("ops@example.com"), "provider text is redacted");
	}

	@Test
	public void useCaseScenarioAndStepProvidersAddTheFlow() throws Exception {
		Project project = newProject("CtxFlow");
		String n = stamp();
		actor(project, "Member " + n, "Borrows tools.");
		UseCase useCase = useCase(project, "Borrow a tool " + n, "Member borrows a tool.",
				"Member " + n, "Find the tool " + n, "Check it out " + n, "Take it home " + n);
		Scenario alternate = scenario(project, "Tool is out " + n, "Join waitlist " + n);
		addScenario(useCase, alternate);
		useProviders(project, "usecase-scenarios", "scenario-usecases", "step-sequence");

		String useCasePrompt = review("UseCase", useCase);
		assertTrue(useCasePrompt.contains("primary scenario")
				&& useCasePrompt.contains("additional scenario")
				&& useCasePrompt.contains("Tool is out " + n), useCasePrompt);
		assertTrue(useCasePrompt.contains("step 2 of 3") && useCasePrompt.contains("Check it out " + n),
				useCasePrompt);

		UseCase fresh = getProjectRepository().findById(UseCase.class, useCase.getId());
		Scenario primary = fresh.getScenario();
		String scenarioPrompt = review("Scenario", primary);
		assertTrue(scenarioPrompt.contains("uses this scenario")
				&& scenarioPrompt.contains("Borrow a tool " + n)
				&& scenarioPrompt.contains("primary actor"), scenarioPrompt);

		Step middle = primary.getSteps().get(1);
		String stepPrompt = review("Step", middle);
		assertTrue(stepPrompt.contains("previous step") && stepPrompt.contains("Find the tool " + n)
				&& stepPrompt.contains("next step") && stepPrompt.contains("Take it home " + n)
				&& stepPrompt.contains("Borrow a tool " + n), stepPrompt);
	}

	@Test
	public void storyAndActorProvidersAddTheCast() throws Exception {
		Project project = newProject("CtxCast");
		String n = stamp();
		Actor member = actor(project, "Member " + n, "Borrows tools.");
		Actor clerk = actor(project, "Clerk " + n, "Runs the desk.");
		Story story = story(project, "Saturday rush " + n, "Lots of members at once.",
				"Member " + n);
		addActor(story, clerk);
		Goal goal = goal(project, "Short queues " + n, "Nobody waits long.");
		addGoal(member, goal);
		useCase(project, "Return a tool " + n, "Member returns a tool.", "Member " + n,
				"Hand it over " + n);
		useProviders(project, "story-actors", "actor-references");

		String storyPrompt = review("Story", story);
		assertTrue(storyPrompt.contains("primary actor") && storyPrompt.contains("Member " + n)
				&& storyPrompt.contains("Clerk " + n), storyPrompt);

		String actorPrompt = review("Actor", member);
		assertTrue(actorPrompt.contains("Saturday rush " + n)
				&& actorPrompt.contains("primary actor of use case")
				&& actorPrompt.contains("Return a tool " + n)
				&& actorPrompt.contains("actor's goal") && actorPrompt.contains("Short queues " + n),
				actorPrompt);
	}

	@Test
	public void aLargeProjectIsTrimmedNotSkippedAndTheNearDuplicateSurvives() throws Exception {
		Project project = newProject("CtxLarge");
		String n = stamp();
		Goal target = goal(project, "Renew borrowed tools online " + n,
				"Members renew a borrowed tool from the website.");
		for (int i = 0; i < 40; i++) {
			goal(project, "Filler goal " + i + " " + n, "Unrelated topic number " + i
					+ " about parking, opening hours and volunteer rotas.");
		}
		goal(project, "Online renewal of tools " + n, "A member can renew borrowed tools online.");
		useProviders(project, Map.of("goal-siblings", 1200), "goal-siblings");

		AssistantRunEntity run = reviewRun("Goal", target);
		String prompt = lastPrompt();

		assertEquals("SUCCEEDED", run.getStatus(), run.getErrorSummary());
		assertTrue(prompt.contains("Online renewal of tools " + n),
				"the near-duplicate ranks first and survives trimming");
		assertTrue(prompt.matches("(?s).*goal-siblings: \\d+ of 41 shown.*"), prompt);
	}

	// ---- helpers -------------------------------------------------------------------------

	private void useProviders(Project project, String... providers) throws Exception {
		useProviders(project, Map.of(), providers);
	}

	private void useProviders(Project project, Map<String, Integer> budgets, String... providers) {
		AssistantDefinition bundled = definitionStore.bundled().stream()
				.filter(d -> REVIEW.equals(d.key())).findFirst().orElseThrow();
		List<String> ids = new ArrayList<>(List.of("entity"));
		ids.addAll(List.of(providers));
		definitionStore.save(new AssistantDefinition(bundled.key(), bundled.displayName(),
				bundled.kind(), bundled.taskType(), bundled.scope(), ids, bundled.instructions(),
				bundled.vocabulary(), bundled.outputSchemaName(), bundled.outputSchemaVersion(),
				true, 1, DefinitionSource.PROJECT, project.getId(), bundled.version(), null,
				budgets), "admin");
	}

	private String review(String type, ProjectOrDomainEntity entity) throws IOException {
		AssistantRunEntity run = reviewRun(type, entity);
		assertEquals("SUCCEEDED", run.getStatus(), type + " run: " + run.getErrorSummary());
		return lastPrompt();
	}

	private AssistantRunEntity reviewRun(String type, ProjectOrDomainEntity entity) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("admin", null, List.of()));
		aiReviewService.requestReview(type, entity.getId());
		AssistantRunEntity queued = assistantRunRepository.findAll().stream()
				.filter(run -> entity.getId().equals(run.getTargetId())
						&& "REQUIREMENTS_REVIEW".equals(run.getTaskType())
						&& "QUEUED".equals(run.getStatus()))
				.reduce((first, second) -> second)
				.orElseThrow(() -> new AssertionError("no QUEUED review run for " + type));
		assistantRunWorker.run(queued.getRunId());
		return assistantRunRepository.findById(queued.getId())
				.orElseThrow(() -> new AssertionError("review run row vanished"));
	}

	private String lastPrompt() throws IOException {
		return Files.readString(FAKE_CLI_DIR.resolve("last-prompt"));
	}

	private static String stamp() {
		return Long.toString(System.nanoTime(), 36);
	}

	private Project newProject(String prefix) throws Exception {
		String n = stamp();
		EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
		cmd.setEditedBy(admin);
		cmd.setName(prefix + " Project " + n);
		cmd.setOrganizationName(prefix + " Org " + n);
		return getCommandHandler().execute(cmd).getProject();
	}

	private Goal goal(Project project, String name, String text) throws Exception {
		EditGoalCommand cmd = getProjectCommandFactory().newEditGoalCommand();
		cmd.setEditedBy(admin);
		cmd.setGoalContainer(project);
		cmd.setName(name);
		cmd.setText(text);
		return getCommandHandler().execute(cmd).getGoal();
	}

	private void relate(Project project, Goal from, String toName, String type) throws Exception {
		EditGoalRelationCommand cmd = getProjectCommandFactory().newEditGoalRelationCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setFromGoal(from.getName());
		cmd.setToGoal(toName);
		cmd.setRelationType(type);
		getCommandHandler().execute(cmd);
	}

	private NonUserStakeholder nonUserStakeholder(Project project, String name, String text)
			throws Exception {
		EditNonUserStakeholderCommand cmd = getProjectCommandFactory()
				.newEditNonUserStakeholderCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName(name);
		cmd.setText(text);
		return getCommandHandler().execute(cmd).getStakeholder();
	}

	private void createUser(String username) throws Exception {
		EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
		cmd.setEditedBy(admin);
		cmd.setUsername(username);
		cmd.setPassword("pw");
		cmd.setRepassword("pw");
		cmd.setName("Pat " + username);
		cmd.setEmailAddress(username + "@example.com");
		cmd.setPhoneNumber("");
		cmd.setOrganizationName("CtxOrg");
		cmd.addUserRoleName("ProjectUserRole");
		getCommandHandler().execute(cmd);
	}

	private UserStakeholder userStakeholder(Project project, String username, String team)
			throws Exception {
		EditUserStakeholderCommand cmd = getProjectCommandFactory().newEditUserStakeholderCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setUsername(username);
		cmd.setTeamName(team);
		cmd.setStakeholderPermissions(Set.of());
		return getCommandHandler().execute(cmd).getStakeholder();
	}

	private void addGoal(GoalContainer container, Goal goal) throws Exception {
		AddGoalToGoalContainerCommand cmd = getProjectCommandFactory()
				.newAddGoalToGoalContainerCommand();
		cmd.setEditedBy(admin);
		cmd.setGoalContainer(container);
		cmd.setGoal(goal);
		getCommandHandler().execute(cmd);
	}

	private Actor actor(Project project, String name, String text) throws Exception {
		EditActorCommand cmd = getProjectCommandFactory().newEditActorCommand();
		cmd.setEditedBy(admin);
		cmd.setActorContainer(project);
		cmd.setName(name);
		cmd.setText(text);
		return getCommandHandler().execute(cmd).getActor();
	}

	private void addActor(com.rreganjr.requel.project.ActorContainer container, Actor actor)
			throws Exception {
		AddActorToActorContainerCommand cmd = getProjectCommandFactory()
				.newAddActorToActorContainerCommand();
		cmd.setEditedBy(admin);
		cmd.setActorContainer(container);
		cmd.setActor(actor);
		getCommandHandler().execute(cmd);
	}

	private Story story(Project project, String name, String text, String primaryActor)
			throws Exception {
		EditStoryCommand cmd = getProjectCommandFactory().newEditStoryCommand();
		cmd.setEditedBy(admin);
		cmd.setStoryContainer(project);
		cmd.setName(name);
		cmd.setText(text);
		cmd.setStoryTypeName("Success");
		cmd.setPrimaryActorName(primaryActor);
		return getCommandHandler().execute(cmd).getStory();
	}

	private EditScenarioStepCommand step(Project project, String name) {
		EditScenarioStepCommand cmd = getProjectCommandFactory().newEditScenarioStepCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName(name);
		cmd.setText("Text for " + name);
		cmd.setScenarioTypeName(ScenarioType.Primary.name());
		return cmd;
	}

	private UseCase useCase(Project project, String name, String text, String primaryActor,
			String... stepNames) throws Exception {
		List<EditScenarioStepCommand> steps = new ArrayList<>();
		for (String stepName : stepNames) {
			steps.add(step(project, stepName));
		}
		EditUseCaseCommand cmd = getProjectCommandFactory().newEditUseCaseCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName(name);
		cmd.setText(text);
		cmd.setPrimaryActorName(primaryActor);
		cmd.setStepCommands(steps);
		return getCommandHandler().execute(cmd).getUseCase();
	}

	private Scenario scenario(Project project, String name, String... stepNames)
			throws Exception {
		List<EditScenarioStepCommand> steps = new ArrayList<>();
		for (String stepName : stepNames) {
			steps.add(step(project, stepName));
		}
		EditScenarioCommand cmd = getProjectCommandFactory().newEditScenarioCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName(name);
		cmd.setText("Scenario " + name);
		cmd.setScenarioTypeName(ScenarioType.Alternative.name());
		cmd.setStepCommands(steps);
		return getCommandHandler().execute(cmd).getScenario();
	}

	private void addScenario(UseCase useCase, Scenario scenario) throws Exception {
		AddScenarioToUseCaseCommand cmd = getProjectCommandFactory()
				.newAddScenarioToUseCaseCommand();
		cmd.setEditedBy(admin);
		cmd.setUseCase(useCase);
		cmd.setScenario(scenario);
		getCommandHandler().execute(cmd);
	}

	private static Path createFakeCli() {
		try {
			Path dir = Files.createTempDirectory("requel-fake-cli-");
			dir.toFile().deleteOnExit();
			Path script = dir.resolve("fake-claude");
			Files.writeString(script, String.join("\n",
					"#!/bin/sh",
					"# Fake claude for ContextProvidersIT: keep the prompt, then reply.",
					"here=$(dirname \"$0\")",
					"cat > \"$here/last-prompt\"",
					"cat \"$here/reply.json\"",
					""));
			Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"));
			return dir;
		} catch (IOException e) {
			throw new IllegalStateException("could not create the fake CLI", e);
		}
	}
}
