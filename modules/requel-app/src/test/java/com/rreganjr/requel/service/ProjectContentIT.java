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
package com.rreganjr.requel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.command.EditArgumentCommand;
import com.rreganjr.requel.annotation.command.EditIssueCommand;
import com.rreganjr.requel.annotation.command.EditNoteCommand;
import com.rreganjr.requel.annotation.command.EditPositionCommand;
import com.rreganjr.requel.annotation.command.ResolveIssueCommand;
import com.rreganjr.requel.annotation.spi.AnnotationFreshness;
import com.rreganjr.requel.gateway.ProjectContentTooLargeException;
import com.rreganjr.requel.gateway.QueryGateway;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.NonUserStakeholder;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.ScenarioType;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.AddActorToActorContainerCommand;
import com.rreganjr.requel.project.command.AddGoalToGoalContainerCommand;
import com.rreganjr.requel.project.command.AddScenarioToUseCaseCommand;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditGlossaryTermCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditGoalRelationCommand;
import com.rreganjr.requel.project.command.EditNonUserStakeholderCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditScenarioCommand;
import com.rreganjr.requel.project.command.EditScenarioStepCommand;
import com.rreganjr.requel.project.command.EditStoryCommand;
import com.rreganjr.requel.project.command.EditUseCaseCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.service.api.dto.GoalContentDto;
import com.rreganjr.requel.service.api.dto.IssueDto;
import com.rreganjr.requel.service.api.dto.ProjectContentDto;
import com.rreganjr.requel.service.api.dto.ProjectDto;
import com.rreganjr.requel.service.api.dto.ScenarioContentDto;
import com.rreganjr.requel.service.api.dto.StepRefDto;
import com.rreganjr.requel.service.api.dto.UseCaseContentDto;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.service.query.ProjectContentQueryService;
import com.rreganjr.requel.service.query.ProjectQueryController;
import com.rreganjr.requel.tagging.TagRepository;
import com.rreganjr.requel.tagging.Taggable;
import com.rreganjr.requel.tagging.command.AssignTagCommand;
import com.rreganjr.requel.tagging.command.EditTagCommand;
import com.rreganjr.requel.tagging.command.TagCommandFactory;
import com.rreganjr.requel.tagging.spi.TaggableTypeRegistry;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;

/**
 * Issue #274: the project content read through the in-process query gateway — one call carries
 * every entity with its text, scenarios with their steps (a shared step once, under one id), the
 * relations as ids, tags and annotations in three modes; the counts match the project summary; two
 * reads are equal; access is checked; and a project over the cap is refused with the overflow
 * named, not cut short.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProjectContentIT extends AbstractIntegrationTestCase {

	@Autowired
	private QueryGateway queryGateway;

	@Autowired
	private TagCommandFactory tagCommandFactory;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CurrentUserResolver currentUserResolver;

	@Autowired
	private ProjectQueryController projectQueryController;

	@Autowired
	private ObjectProvider<AnnotationFreshness> annotationFreshness;

	@Autowired
	private ObjectProvider<TagRepository> tagRepository;

	@Autowired
	private ObjectProvider<TaggableTypeRegistry> taggableTypeRegistry;

	private String projectName;
	private String readerUsername;
	private String outsiderUsername;

	private Long supportingGoalId;
	private Long supportedGoalId;
	private Long actorId;
	private Long storyId;
	private Long useCaseId;
	private Long primaryScenarioId;
	private Long alternativeScenarioId;
	private Long subScenarioId;
	private Long sharedStepId;
	private Long canonicalTermId;
	private Long alternateTermId;
	private Long stakeholderId;
	private Long resolvedIssueId;
	private Long openIssueId;

	@BeforeAll
	void setUpFixture() throws Exception {
		initializeBaselineData();
		User admin = getUserRepository().findUserByUsername("admin");
		long ts = System.currentTimeMillis();
		projectName = "content-read-" + ts;
		Project project = createProject(admin, projectName);
		readerUsername = "content-reader-" + ts;
		createUser(admin, readerUsername);
		addUserStakeholder(admin, project, readerUsername);
		outsiderUsername = "content-outsider-" + ts;
		createUser(admin, outsiderUsername);

		Goal supporting = createGoal(admin, project, "Operators are replaceable",
				"More than one person can fill the Operator role.");
		Goal supported = createGoal(admin, project, "Conduit owns the stack",
				"Conduit is the registered owner of the repo and the stack.");
		supportingGoalId = supporting.getId();
		supportedGoalId = supported.getId();
		relate(admin, project, supporting.getName(), supported.getName(), "Supports");

		Actor operator = createActor(admin, project, "Operator", "Runs a live room.");
		actorId = operator.getId();
		Story story = createStory(admin, project, "Hand-over",
				"The owner hands the service to Conduit.");
		storyId = story.getId();
		addGoal(admin, story, supporting);
		addActor(admin, story, operator);

		UseCase useCase = createUseCase(admin, project, "End a room", "An operator ends a room.",
				operator.getName(), "Open the admin console", "Press End");
		useCaseId = useCase.getId();
		Scenario primary = useCase.getScenario();
		primaryScenarioId = primary.getId();
		Step shared = primary.getSteps().get(0);
		sharedStepId = shared.getId();

		Scenario sub = createScenario(admin, project, "Confirm the end", List.of(
				newStep(admin, project, "Confirm in the dialog")));
		subScenarioId = sub.getId();
		Scenario alternative = createScenario(admin, project, "End from the room list", List.of(
				existingStep(admin, project, shared), existingStep(admin, project, sub)));
		alternativeScenarioId = alternative.getId();
		addScenario(admin, useCase, alternative);

		GlossaryTerm canonical = createTerm(admin, project, "webinar", "A Zoom webinar.", null);
		canonicalTermId = canonical.getId();
		alternateTermId = createTerm(admin, project, "livestream", "The viewer relay.", canonical)
				.getId();

		NonUserStakeholder sponsor = createNonUserStakeholder(admin, project, "Sponsor",
				"Pays for the event.");
		stakeholderId = sponsor.getId();
		addGoal(admin, sponsor, supporting);

		tag(admin, project, (Taggable) supporting, "area", "admin");

		note(admin, project, supporting, "Raised in the hand-over meeting.");
		openIssueId = issue(admin, project, supporting, "Who holds the second seat?").getId();
		Issue resolved = issue(admin, project, supporting, "Is the role documented?");
		resolvedIssueId = resolved.getId();
		Position position = position(admin, resolved, "Yes, in RUNBOOK.md.");
		argument(admin, position, "The runbook covers every step.");
		resolve(admin, resolved, position, supporting);
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void entityCountsMatchTheProjectSummary() {
		authenticate(readerUsername);
		ProjectContentDto content = queryGateway.getProjectContent(projectName, null);
		ProjectDto summary = content.project();

		assertEquals(summary.stakeholderCount(), content.stakeholders().size());
		assertEquals(summary.goalCount(), content.goals().size());
		assertEquals(summary.storyCount(), content.stories().size());
		assertEquals(summary.actorCount(), content.actors().size());
		assertEquals(summary.useCaseCount(), content.useCases().size());
		assertEquals(summary.scenarioCount(), content.scenarios().size(),
				"every scenario reachable from the project is one of the project's scenarios");
		assertEquals(summary.glossaryTermCount(), content.glossary().size());
		// The summary has no step count: the plain steps are the use case's own step, the shared
		// step and the sub-scenario's step.
		assertEquals(3, content.steps().size());
		assertEquals(summary.goalCount(), 2);
	}

	@Test
	void oneReadCarriesTheTextAndEveryIdItReferencesResolvesInTheSameRead() {
		authenticate(readerUsername);
		ProjectContentDto content = queryGateway.getProjectContent(projectName, "all");

		GoalContentDto supporting = goal(content, supportingGoalId);
		assertEquals("More than one person can fill the Operator role.", supporting.text());
		assertEquals(1, supporting.relations().size());
		assertEquals("Supports", supporting.relations().get(0).relationType());
		assertEquals(supportedGoalId, supporting.relations().get(0).goalId());
		assertEquals(List.of("area:admin"), supporting.tags());

		assertEquals(List.of(supportingGoalId), content.stories().stream()
				.filter(s -> s.id().equals(storyId)).findFirst().orElseThrow().goalIds());
		UseCaseContentDto useCase = content.useCases().stream()
				.filter(u -> u.id().equals(useCaseId)).findFirst().orElseThrow();
		assertEquals(actorId, useCase.primaryActorId());
		assertEquals(primaryScenarioId, useCase.primaryScenarioId());
		assertEquals(List.of(alternativeScenarioId), useCase.additionalScenarioIds());
		assertEquals(canonicalTermId, content.glossary().stream()
				.filter(t -> t.id().equals(alternateTermId)).findFirst().orElseThrow()
				.canonicalTermId());
		assertEquals("Pays for the event.", content.stakeholders().stream()
				.filter(s -> s.id().equals(stakeholderId)).findFirst().orElseThrow().text());

		Set<Long> ids = new HashSet<>();
		content.stakeholders().forEach(e -> ids.add(e.id()));
		content.goals().forEach(e -> ids.add(e.id()));
		content.stories().forEach(e -> ids.add(e.id()));
		content.actors().forEach(e -> ids.add(e.id()));
		content.useCases().forEach(e -> ids.add(e.id()));
		content.scenarios().forEach(e -> ids.add(e.id()));
		content.steps().forEach(e -> ids.add(e.id()));
		content.glossary().forEach(e -> ids.add(e.id()));
		List<Long> referenced = new ArrayList<>();
		content.stakeholders().forEach(e -> referenced.addAll(e.goalIds()));
		content.goals().forEach(e -> e.relations().forEach(r -> referenced.add(r.goalId())));
		content.stories().forEach(e -> {
			referenced.addAll(e.goalIds());
			referenced.addAll(e.actorIds());
		});
		content.actors().forEach(e -> referenced.addAll(e.goalIds()));
		content.useCases().forEach(e -> {
			referenced.add(e.primaryActorId());
			referenced.add(e.primaryScenarioId());
			referenced.addAll(e.additionalScenarioIds());
			referenced.addAll(e.goalIds());
			referenced.addAll(e.actorIds());
			referenced.addAll(e.storyIds());
		});
		content.scenarios().forEach(e -> e.steps().forEach(s -> referenced.add(s.id())));
		content.glossary().forEach(e -> {
			if (e.canonicalTermId() != null) {
				referenced.add(e.canonicalTermId());
			}
		});
		for (Long id : referenced) {
			assertTrue(ids.contains(id), "referenced id " + id + " is in the read");
		}
	}

	@Test
	void aSharedStepHasOneIdInBothScenariosAndIsListedOnce() {
		authenticate(readerUsername);
		ProjectContentDto content = queryGateway.getProjectContent(projectName, "none");

		ScenarioContentDto primary = scenario(content, primaryScenarioId);
		ScenarioContentDto alternative = scenario(content, alternativeScenarioId);
		assertTrue(primary.steps().contains(new StepRefDto("Step", sharedStepId)));
		assertEquals(List.of(new StepRefDto("Step", sharedStepId),
				new StepRefDto("Scenario", subScenarioId)), alternative.steps(),
				"in order, the sub-scenario as a Scenario reference");
		assertEquals(1, content.steps().stream().filter(s -> s.id().equals(sharedStepId)).count());
		assertTrue(content.scenarios().stream().anyMatch(s -> s.id().equals(subScenarioId)));
		assertFalse(content.steps().stream().anyMatch(s -> s.id().equals(subScenarioId)),
				"a scenario used as a step is a scenario, not a step");
	}

	@Test
	void theAnnotationModesChooseWhatComesBack() {
		authenticate(readerUsername);
		GoalContentDto all = goal(queryGateway.getProjectContent(projectName, "ALL"),
				supportingGoalId);
		IssueDto resolved = all.annotations().issues().stream()
				.filter(i -> i.id().equals(resolvedIssueId)).findFirst().orElseThrow();
		assertTrue(resolved.resolved());
		assertEquals("Yes, in RUNBOOK.md.", resolved.positions().get(0).text());
		assertEquals("The runbook covers every step.",
				resolved.positions().get(0).arguments().get(0).text());
		assertEquals(1, all.annotations().notes().size());

		GoalContentDto open = goal(queryGateway.getProjectContent(projectName, "open"),
				supportingGoalId);
		assertEquals(List.of(openIssueId), open.annotations().issues().stream()
				.map(IssueDto::id).toList());
		assertEquals(1, open.annotations().notes().size(), "open keeps the notes");

		ProjectContentDto none = queryGateway.getProjectContent(projectName, "none");
		assertEquals("NONE", none.annotations());
		assertTrue(goal(none, supportingGoalId).annotations().issues().isEmpty());
		assertTrue(goal(none, supportingGoalId).annotations().notes().isEmpty());

		assertThrows(ResponseStatusException.class,
				() -> queryGateway.getProjectContent(projectName, "some"),
				"an unknown mode is a bad request");
	}

	@Test
	void twoReadsOfAnUnchangedProjectAreEqual() throws Exception {
		authenticate(readerUsername);
		String first = objectMapper.writeValueAsString(
				queryGateway.getProjectContent(projectName, null));
		String second = objectMapper.writeValueAsString(
				queryGateway.getProjectContent(projectName, null));
		assertEquals(first, second);
	}

	@Test
	void theReadIsCheckedLikeTheOtherReads() {
		authenticate(outsiderUsername);
		ResponseStatusException refused = assertThrows(ResponseStatusException.class,
				() -> queryGateway.getProjectContent(projectName, null));
		assertEquals(403, refused.getStatusCode().value());

		authenticate(readerUsername);
		ResponseStatusException missing = assertThrows(ResponseStatusException.class,
				() -> queryGateway.getProjectContent("no-such-project-" + System.nanoTime(), null));
		assertEquals(404, missing.getStatusCode().value());
	}

	@Test
	void aProjectOverTheCapIsRefusedWithEachSectionNamed() {
		authenticate(readerUsername);
		ProjectContentDto uncapped = queryGateway.getProjectContent(projectName, null);
		int entitiesOnly = queryGateway.getProjectContent(projectName, "none").characters();
		assertTrue(uncapped.characters() > entitiesOnly, "annotations count against the cap");
		log.info("#274 content read of the fixture: {} characters ({} without annotations)",
				uncapped.characters(), entitiesOnly);

		ProjectContentQueryService capped = new ProjectContentQueryService(getProjectRepository(),
				currentUserResolver, projectQueryController, annotationFreshness, tagRepository,
				taggableTypeRegistry, entitiesOnly);
		ProjectContentTooLargeException tooLarge = assertThrows(
				ProjectContentTooLargeException.class,
				() -> inTransaction(() -> capped.read(projectName, "all")));
		assertEquals(uncapped.characters(), tooLarge.getCharacters());
		assertEquals(entitiesOnly, tooLarge.getMaxCharacters());
		assertEquals(List.of("stakeholders", "goals", "stories", "actors", "useCases",
				"scenarios", "steps", "glossary", "annotations"),
				new ArrayList<>(tooLarge.getSections().keySet()));
		assertEquals(2, tooLarge.getSections().get("goals").count());
		assertEquals(3, tooLarge.getSections().get("annotations").count());
		for (String part : List.of(projectName, "the cap is", "goals 2 / ", "annotations 3 / ",
				"annotations=none")) {
			assertTrue(tooLarge.getMessage().contains(part), tooLarge.getMessage());
		}

		ProjectContentDto withoutAnnotations = inTransaction(() -> capped.read(projectName,
				"none"));
		assertEquals(entitiesOnly, withoutAnnotations.characters(),
				"the same project fits without its annotations");
	}

	// ---- helpers ---------------------------------------------------------------------------------

	private static GoalContentDto goal(ProjectContentDto content, Long id) {
		return content.goals().stream().filter(g -> g.id().equals(id)).findFirst().orElseThrow();
	}

	private static ScenarioContentDto scenario(ProjectContentDto content, Long id) {
		return content.scenarios().stream().filter(s -> s.id().equals(id)).findFirst()
				.orElseThrow();
	}

	private <T> T inTransaction(java.util.function.Supplier<T> work) {
		return new TransactionTemplate(transactionManager).execute(status -> work.get());
	}

	private void authenticate(String username) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(username, "x", List.of()));
	}

	private Project createProject(User admin, String name) throws Exception {
		EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
		cmd.setEditedBy(admin);
		cmd.setName(name);
		cmd.setText("project content read integration test");
		cmd.setOrganizationName("ContentReadOrg-" + System.nanoTime());
		return getCommandHandler().execute(cmd).getProject();
	}

	private void createUser(User admin, String username) throws Exception {
		EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
		cmd.setEditedBy(admin);
		cmd.setUsername(username);
		cmd.setPassword("pw");
		cmd.setRepassword("pw");
		cmd.setName(username);
		cmd.setEmailAddress(username + "@example.com");
		cmd.setPhoneNumber("");
		cmd.setOrganizationName("ContentReadOrg");
		cmd.addUserRoleName("ProjectUserRole");
		getCommandHandler().execute(cmd);
	}

	private void addUserStakeholder(User admin, Project project, String username)
			throws Exception {
		EditUserStakeholderCommand cmd = getProjectCommandFactory().newEditUserStakeholderCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setUsername(username);
		cmd.setStakeholderPermissions(Set.of());
		getCommandHandler().execute(cmd);
	}

	private Goal createGoal(User admin, Project project, String name, String text)
			throws Exception {
		EditGoalCommand cmd = getProjectCommandFactory().newEditGoalCommand();
		cmd.setEditedBy(admin);
		cmd.setGoalContainer(project);
		cmd.setName(name);
		cmd.setText(text);
		return getCommandHandler().execute(cmd).getGoal();
	}

	private void relate(User admin, Project project, String from, String to, String type)
			throws Exception {
		EditGoalRelationCommand cmd = getProjectCommandFactory().newEditGoalRelationCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setFromGoal(from);
		cmd.setToGoal(to);
		cmd.setRelationType(type);
		getCommandHandler().execute(cmd);
	}

	private Actor createActor(User admin, Project project, String name, String text)
			throws Exception {
		EditActorCommand cmd = getProjectCommandFactory().newEditActorCommand();
		cmd.setEditedBy(admin);
		cmd.setActorContainer(project);
		cmd.setName(name);
		cmd.setText(text);
		return getCommandHandler().execute(cmd).getActor();
	}

	private Story createStory(User admin, Project project, String name, String text)
			throws Exception {
		EditStoryCommand cmd = getProjectCommandFactory().newEditStoryCommand();
		cmd.setEditedBy(admin);
		cmd.setStoryContainer(project);
		cmd.setName(name);
		cmd.setText(text);
		cmd.setStoryTypeName("Success");
		return getCommandHandler().execute(cmd).getStory();
	}

	private void addGoal(User admin, com.rreganjr.requel.project.GoalContainer container,
			Goal goal) throws Exception {
		AddGoalToGoalContainerCommand cmd = getProjectCommandFactory()
				.newAddGoalToGoalContainerCommand();
		cmd.setEditedBy(admin);
		cmd.setGoalContainer(container);
		cmd.setGoal(goal);
		getCommandHandler().execute(cmd);
	}

	private void addActor(User admin, com.rreganjr.requel.project.ActorContainer container,
			Actor actor) throws Exception {
		AddActorToActorContainerCommand cmd = getProjectCommandFactory()
				.newAddActorToActorContainerCommand();
		cmd.setEditedBy(admin);
		cmd.setActorContainer(container);
		cmd.setActor(actor);
		getCommandHandler().execute(cmd);
	}

	private EditScenarioStepCommand newStep(User admin, Project project, String name) {
		EditScenarioStepCommand cmd = getProjectCommandFactory().newEditScenarioStepCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName(name);
		cmd.setText("Text for " + name);
		cmd.setScenarioTypeName(ScenarioType.Primary.name());
		return cmd;
	}

	private EditScenarioStepCommand existingStep(User admin, Project project, Step step) {
		EditScenarioStepCommand cmd = getProjectCommandFactory().newEditScenarioStepCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setStep(step);
		return cmd;
	}

	private UseCase createUseCase(User admin, Project project, String name, String text,
			String primaryActorName, String... stepNames) throws Exception {
		List<EditScenarioStepCommand> steps = new ArrayList<>();
		for (String stepName : stepNames) {
			steps.add(newStep(admin, project, stepName));
		}
		EditUseCaseCommand cmd = getProjectCommandFactory().newEditUseCaseCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName(name);
		cmd.setText(text);
		cmd.setPrimaryActorName(primaryActorName);
		cmd.setStepCommands(steps);
		return getCommandHandler().execute(cmd).getUseCase();
	}

	private Scenario createScenario(User admin, Project project, String name,
			List<EditScenarioStepCommand> steps) throws Exception {
		EditScenarioCommand cmd = getProjectCommandFactory().newEditScenarioCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName(name);
		cmd.setText("Scenario " + name);
		cmd.setScenarioTypeName(ScenarioType.Primary.name());
		cmd.setStepCommands(steps);
		return getCommandHandler().execute(cmd).getScenario();
	}

	private void addScenario(User admin, UseCase useCase, Scenario scenario) throws Exception {
		AddScenarioToUseCaseCommand cmd = getProjectCommandFactory()
				.newAddScenarioToUseCaseCommand();
		cmd.setEditedBy(admin);
		cmd.setUseCase(useCase);
		cmd.setScenario(scenario);
		getCommandHandler().execute(cmd);
	}

	private GlossaryTerm createTerm(User admin, Project project, String name, String text,
			GlossaryTerm canonical) throws Exception {
		EditGlossaryTermCommand cmd = getProjectCommandFactory().newEditGlossaryTermCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName(name);
		cmd.setText(text);
		if (canonical != null) {
			cmd.setCanonicalTerm(canonical);
		}
		return getCommandHandler().execute(cmd).getGlossaryTerm();
	}

	private NonUserStakeholder createNonUserStakeholder(User admin, Project project, String name,
			String text) throws Exception {
		EditNonUserStakeholderCommand cmd = getProjectCommandFactory()
				.newEditNonUserStakeholderCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName(name);
		cmd.setText(text);
		return getCommandHandler().execute(cmd).getStakeholder();
	}

	private void tag(User admin, Project project, Taggable target, String category, String value)
			throws Exception {
		EditTagCommand edit = tagCommandFactory.newEditTagCommand();
		edit.setEditedBy(admin);
		edit.setProjectScope(project);
		edit.setCategory(category);
		edit.setValue(value);
		edit = getCommandHandler().execute(edit);
		AssignTagCommand assign = tagCommandFactory.newAssignTagCommand();
		assign.setEditedBy(admin);
		assign.setProjectScope(project);
		assign.setTag(edit.getTag());
		assign.setTaggable(target);
		getCommandHandler().execute(assign);
	}

	private void note(User admin, Project project, Goal goal, String text) throws Exception {
		EditNoteCommand cmd = getAnnotationCommandFactory().newEditNoteCommand();
		cmd.setEditedBy(admin);
		cmd.setGroupingObject(project);
		cmd.setAnnotatable(goal);
		cmd.setText(text);
		getCommandHandler().execute(cmd);
	}

	private Issue issue(User admin, Project project, Goal goal, String text) throws Exception {
		EditIssueCommand cmd = getAnnotationCommandFactory().newEditIssueCommand();
		cmd.setEditedBy(admin);
		cmd.setGroupingObject(project);
		cmd.setAnnotatable(goal);
		cmd.setText(text);
		cmd.setMustBeResolved(false);
		return getCommandHandler().execute(cmd).getIssue();
	}

	private Position position(User admin, Issue issue, String text) throws Exception {
		EditPositionCommand cmd = getAnnotationCommandFactory().newEditPositionCommand();
		cmd.setEditedBy(admin);
		cmd.setIssue(issue);
		cmd.setText(text);
		return getCommandHandler().execute(cmd).getPosition();
	}

	private void argument(User admin, Position position, String text) throws Exception {
		EditArgumentCommand cmd = getAnnotationCommandFactory().newEditArgumentCommand();
		cmd.setEditedBy(admin);
		cmd.setPosition(position);
		cmd.setText(text);
		cmd.setSupportLevelName("StronglyFor");
		getCommandHandler().execute(cmd);
	}

	private void resolve(User admin, Issue issue, Position position, Goal goal) throws Exception {
		ResolveIssueCommand cmd = getAnnotationCommandFactory().newResolveIssueCommand(position);
		cmd.setEditedBy(admin);
		cmd.setIssue(issue);
		cmd.setPosition(position);
		cmd.setAnnotatable(goal);
		getCommandHandler().execute(cmd);
	}
}
