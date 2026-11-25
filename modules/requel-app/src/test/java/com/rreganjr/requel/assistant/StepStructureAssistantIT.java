/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import org.hibernate.Hibernate;

import com.rreganjr.requel.annotation.ArgumentPositionSupportLevel;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.command.EditArgumentCommand;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.impl.LexicalIssue;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunEntity;
import com.rreganjr.requel.assistant.legacynlp.StepStructureAssistant;
import com.rreganjr.requel.command.AnalysisRequestDispatcher;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.ScenarioType;
import com.rreganjr.requel.project.impl.ScenarioImpl;
import com.rreganjr.requel.project.impl.StepImpl;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.AddActorToActorContainerCommand;
import com.rreganjr.requel.project.command.AddScenarioToUseCaseCommand;
import com.rreganjr.requel.project.command.EditUseCaseCommand;
import com.rreganjr.validator.EntityValidationException;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditScenarioCommand;
import com.rreganjr.requel.project.command.EditScenarioStepCommand;
import com.rreganjr.platform.identity.User;

/**
 * Issue #349: how the step-structure check reads step and scenario names, with the real parser.
 * Each case runs the assistant on a saved step or scenario and reads the actions it returns.
 */
public class StepStructureAssistantIT extends AbstractLexicalAssistantTest {

	@Autowired
	private StepStructureAssistant assistant;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private AnalysisRequestDispatcher analysisRequestDispatcher;

	private Project project;

	@BeforeEach
	void fixture() throws Exception {
		ensureDictionaryLoaded();
		project = newProject("Steps");
		newActor(project, "Host");
	}

	@Test
	void aCommandStepGetsTheNoActorIssueAndNoNote() throws Exception {
		List<AnnotationAction> actions = analyzeStep("Start the livestream");
		AnnotationAction issue = only(actions, "no-actor");
		assertEquals("LOW", issue.severity());
		assertEquals(Boolean.FALSE, issue.metadata().get("mustResolve"));
		assertTrue(texts(actions).contains("Do nothing."), texts(actions).toString());
		assertTrue(actions.stream().noneMatch(
				a -> a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_NOTE));
	}

	@Test
	void aStepNamingAKnownActorRaisesNothing() throws Exception {
		assertEquals(List.of(), findingTypes(analyzeStep("The host starts the livestream")));
	}

	@Test
	void aStepWhereTheSystemRespondsRaisesNothing() throws Exception {
		assertEquals(List.of(), findingTypes(analyzeStep("The system starts the livestream")));
	}

	@Test
	void aStepWithNoVerbGetsTheUnreadableIssue() throws Exception {
		List<AnnotationAction> actions = analyzeStep("Webinar");
		assertEquals("LOW", only(actions, "unreadable").severity());
		assertEquals(List.of(), findingTypes(analyzeStep(
				"[Ingested verbatim from the source, criterion 1.]")).stream()
				.filter("no-actor"::equals).toList());
	}

	/**
	 * #486: the parser finds no verb in "Member downloads the invoice" ("downloads" as a noun),
	 * but a step that starts with a known actor reads as that actor's.
	 */
	@Test
	void aStepStartingWithAKnownActorReadsAsTheirsWhenTheParserCant() throws Exception {
		newActor(project, "Member");
		assertEquals(List.of(), findingTypes(analyzeStep("Member downloads the invoice " + stamp())));
		assertEquals(List.of(), findingTypes(analyzeStep("The system downloads the invoice " + stamp())));
	}

	/** #486: every role the check offers reads back as an actor's or the system's step. */
	@Test
	void everyRewriteTheCheckOffersReadsBack() throws Exception {
		newActor(project, "Member");
		Step step = firstStep(newUseCase("Invoices " + stamp(), "Member",
				List.of("Download the invoice " + stamp())));
		List<String> rewrites = rewritesOf(step);
		assertEquals(2, rewrites.size(), rewrites.toString());
		for (String rewrite : rewrites) {
			String proposed = rewrite.substring(rewrite.indexOf('"') + 1, rewrite.length() - 2);
			List<String> findings = findingTypes(analyzeStep(proposed));
			assertTrue(findings.stream().noneMatch(f -> f.equals("unreadable") || f.equals("no-actor")
					|| f.equals("subject-not-actor")), proposed + " -> " + findings);
		}
	}

	@Test
	void aStepNamingAnUnknownSubjectGetsTheMustResolveIssue() throws Exception {
		assertEquals(List.of("subject-not-actor"),
				findingTypes(analyzeStep("The producer starts the stream")));
	}

	@Test
	void aCommandScenarioNameRaisesNothing() throws Exception {
		assertEquals(List.of(), findingTypes(analyzeScenario("Create the room")));
	}

	/** Commands whose verb the tagger reads as a noun are retried as commands (roundtable names). */
	@Test
	void aCommandTheTaggerMisreadIsStillACommand() throws Exception {
		assertEquals(List.of("no-actor"), findingTypes(analyzeStep("View the test room as a viewer")));
		assertEquals(List.of(), findingTypes(analyzeScenario("Archive a room")));
	}

	@Test
	void aScenarioNameWithNoVerbGetsTheUnreadableIssue() throws Exception {
		assertEquals(List.of("unreadable"), findingTypes(analyzeScenario("Webinar dry run")));
	}

	@Test
	void aScenarioNameNamingAKnownActorRaisesNothing() throws Exception {
		assertEquals(List.of(), findingTypes(analyzeScenario("The host runs the session")));
	}

	// ---- the project's name as the actor ----------------------------------------------------------

	@Test
	void aStepNamingTheProjectGetsTheProjectAsActorIssueWithItsThreePositions() throws Exception {
		project = namedProject("Roundtable");
		String name = project.getName();
		List<AnnotationAction> actions = analyzeStep(name + " starts the stream");
		AnnotationAction issue = only(actions, "project-as-actor");
		assertEquals(Boolean.TRUE, issue.metadata().get("mustResolve"));
		assertEquals("LEXICAL", issue.metadata().get("kind"));
		assertEquals(name, issue.metadata().get("word"));
		assertTrue(texts(actions).containsAll(List.of("Replace \"" + name + "\" with \"the system\".",
				"Add \"" + name + "\" as an actor.", "Do nothing.")), texts(actions).toString());
		assertTrue(findingTypes(actions).stream().noneMatch("subject-not-actor"::equals));

		// With an actor of that name, it is a known actor.
		newActor(project, name);
		assertEquals(List.of(), findingTypes(analyzeStep(name + " starts the livestream")));
	}

	@Test
	void replacingTheProjectNameWithTheSystemRewritesTheNameAndAReRunRaisesNothing()
			throws Exception {
		project = namedProject("Switchboard");
		Scenario scenario = newScenario(project.getName() + " runs the session", List.of());
		analyzeAndApply(scenario);

		LexicalIssue issue = projectAsActorIssue(scenario.getId());
		resolve(issue, positionWithText(issue, "Replace \"" + project.getName()
				+ "\" with \"the system\"."), null, projectUser());

		assertEquals("The system runs the session", freshScenarioName(scenario.getId()));
		analyzeAndApply(scenario);
		assertTrue(lexicalIssuesOn(scenario.getId()).stream()
				.noneMatch(i -> isProjectAsActor(i) && !i.isResolved()),
				"the rewrite leaves no project-as-actor issue open");
	}

	@Test
	void addingTheProjectNameAsAnActorCreatesTheActor() throws Exception {
		project = namedProject("Lighthouse");
		Scenario scenario = newScenario(project.getName() + " runs the session", List.of());
		analyzeAndApply(scenario);

		LexicalIssue issue = projectAsActorIssue(scenario.getId());
		resolve(issue, positionWithText(issue, "Add \"" + project.getName() + "\" as an actor."),
				null, projectUser());

		assertTrue(inTransaction(() -> getProjectRepository().findById(Project.class,
				project.getId()).getActors().stream()
				.anyMatch(actor -> actor.getName().equals(project.getName()))));
	}

	// ---- #459: the example and the one-click rewrites -----------------------------------------

	private static final String MP4 = "Download the resulting MP4";

	@Test
	void aCommandStepInAUseCaseGetsRewritesForItsActorAndTheSystem() throws Exception {
		Step step = stepInNewUseCase(MP4);
		analyzeAndApplyStep(step);

		Issue issue = noActorIssue(step.getId());
		assertEquals("The step \"Download the resulting MP4\" doesn't say who does it. Set its"
				+ " actor: as the primary actor it reads \"Host downloads the resulting MP4\", or as"
				+ " the system \"The system downloads the resulting MP4\".", issue.getText());
		assertEquals(Set.of("Set the actor to the primary actor: \"Host downloads the resulting MP4\".",
				"Set the actor to the system: \"The system downloads the resulting MP4\".", "Do nothing."),
				positionTexts(issue));
	}

	@Test
	void theVerbTakesItsThirdPersonFormInTheRewrite() throws Exception {
		assertTrue(rewritesOf(stepInNewUseCase("Force-stop the active stream"))
				.contains("Set the actor to the primary actor: \"Host force-stops the active stream\"."));
		assertTrue(rewritesOf(stepInNewUseCase("Issue a new takeover key"))
				.contains("Set the actor to the primary actor: \"Host issues a new takeover key\"."));
	}

	@Test
	void acceptingTheActorRewriteRenamesTheStepAndAReRunRaisesNothing() throws Exception {
		Step step = stepInNewUseCase(MP4);
		analyzeAndApplyStep(step);
		Issue issue = noActorIssue(step.getId());

		resolve(issue, positionWithText(issue, "Set the actor to the primary actor: \"Host downloads the resulting MP4\"."),
				null, projectUser());

		// #468: the role says who does it; the name keeps only the action.
		assertEquals("downloads the resulting MP4", freshStepName(step.getId()));
		assertEquals(com.rreganjr.requel.project.ActorRole.PRIMARY_ACTOR,
				freshStepRole(step.getId()));
		analyzeAndApplyStep(step);
		assertTrue(stepIssues(step.getId()).stream()
				.noneMatch(i -> !i.isResolved() && i.getText().startsWith("The step \"")),
				"nothing open on the renamed step: " + stepIssues(step.getId()).stream()
						.map(Issue::getText).toList());
	}

	@Test
	void aReRunUpdatesTheIssueInPlaceWithoutDuplicatingPositions() throws Exception {
		Step step = stepInNewUseCase(MP4);
		analyzeAndApplyStep(step);
		Issue first = noActorIssue(step.getId());
		Set<Long> positions = positionIds(first);

		analyzeAndApplyStep(step);

		Issue again = noActorIssue(step.getId());
		assertEquals(first.getId(), again.getId());
		assertEquals(positions, positionIds(again));
	}

	@Test
	void renamingTheStepReplacesItsRewrites() throws Exception {
		Step step = stepInNewUseCase(MP4);
		analyzeAndApplyStep(step);
		Issue issue = noActorIssue(step.getId());

		renameStep(step, "Upload the resulting MP4");
		analyzeAndApplyStep(step);

		Issue again = noActorIssue(step.getId());
		assertEquals(issue.getId(), again.getId(), "the same issue, updated in place");
		assertEquals(Set.of("Set the actor to the primary actor: \"Host uploads the resulting MP4\".",
				"Set the actor to the system: \"The system uploads the resulting MP4\".", "Do nothing."),
				positionTexts(again));
	}

	@Test
	void changingThePrimaryActorReplacesItsRewrite() throws Exception {
		newActor(project, "Producer");
		UseCase useCase = newUseCase("Deliver " + stamp(), "Host", List.of(MP4));
		Step step = firstStep(useCase);
		analyzeAndApplyStep(step);

		EditUseCaseCommand edit = getProjectCommandFactory().newEditUseCaseCommand();
		edit.setEditedBy(projectUser());
		edit.setProjectOrDomain(project);
		edit.setUseCase(useCase);
		edit.setPrimaryActorName("Producer");
		getCommandHandler().execute(edit);
		analyzeAndApplyStep(step);

		Set<String> texts = positionTexts(noActorIssue(step.getId()));
		assertTrue(texts.contains("Set the actor to the primary actor: \"Producer downloads the resulting MP4\"."),
				texts.toString());
		assertTrue(texts.stream().noneMatch(text -> text.contains("Host")), texts.toString());
	}

	@Test
	void anAdditionalActorGetsARewriteToo() throws Exception {
		newActor(project, "Zoom");
		UseCase useCase = newUseCase("Hand off " + stamp(), "Host",
				List.of("Hand the stream key to the encoder"));
		AddActorToActorContainerCommand add = getProjectCommandFactory()
				.newAddActorToActorContainerCommand();
		add.setEditedBy(projectUser());
		add.setActorContainer(useCase);
		add.setActor(actorNamed("Zoom"));
		getCommandHandler().execute(add);

		assertEquals(List.of("Set the actor to the primary actor: \"Host hands the stream key to the encoder\".",
				"Set the actor to Zoom: \"Zoom hands the stream key to the encoder\".",
				"Set the actor to the system: \"The system hands the stream key to the encoder\"."),
				rewritesOf(firstStep(useCase)));
	}

	@Test
	void aStepInAnAdditionalScenarioOfAUseCaseGetsItsActor() throws Exception {
		UseCase useCase = newUseCase("Verify " + stamp(), "Host", List.of());
		Scenario extra = newScenario("Manual verification " + stamp(), List.of(MP4));
		AddScenarioToUseCaseCommand add = getProjectCommandFactory().newAddScenarioToUseCaseCommand();
		add.setEditedBy(projectUser());
		add.setUseCase(useCase);
		add.setScenario(extra);
		getCommandHandler().execute(add);

		assertTrue(rewritesOf(firstStep(extra))
				.contains("Set the actor to the primary actor: \"Host downloads the resulting MP4\"."));
	}

	@Test
	void aStepInNoUseCaseGetsThePrimaryActorAndSystemRoles() throws Exception {
		Scenario scenario = newScenario("Loose " + stamp(), List.of(MP4));
		List<AnnotationAction> actions = inTransaction(() -> assistant.analyze(context(),
				getProjectRepository().findById(Scenario.class, scenario.getId()).getSteps().get(0))
				.annotationActions());
		assertEquals("The step \"Download the resulting MP4\" doesn't say who does it, and it"
				+ " isn't in a use case. Set its actor to the primary actor, so it reads \"Primary"
				+ " actor downloads the resulting MP4\" until it joins one, or to the system.",
				only(actions, "no-actor").text());
		assertEquals(List.of("Set the actor to the primary actor: \"Primary actor downloads the resulting MP4\".",
				"Set the actor to the system: \"The system downloads the resulting MP4\".", "Do nothing."), actions.stream()
				.filter(a -> a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION)
				.map(AnnotationAction::text).toList());
	}

	@Test
	void aDroppedRewriteSomebodyArguedOnIsKept() throws Exception {
		Step step = stepInNewUseCase(MP4);
		analyzeAndApplyStep(step);
		Issue issue = noActorIssue(step.getId());
		Position host = positionWithText(issue, "Set the actor to the primary actor: \"Host downloads the resulting MP4\".");
		inTransaction(() -> {
			EditArgumentCommand argue = getAnnotationCommandFactory().newEditArgumentCommand();
			argue.setEditedBy(projectUser());
			argue.setPosition(getAnnotationRepository().get(host));
			argue.setText("Hosts don't download recordings.");
			argue.setSupportLevelName(ArgumentPositionSupportLevel.StronglyAgainst.name());
			try {
				return getCommandHandler().execute(argue);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		});

		renameStep(step, "Upload the resulting MP4");
		analyzeAndApplyStep(step);

		Set<String> texts = positionTexts(noActorIssue(step.getId()));
		assertTrue(texts.contains("Set the actor to the primary actor: \"Host downloads the resulting MP4\"."), texts.toString());
		assertTrue(texts.contains("Set the actor to the primary actor: \"Host uploads the resulting MP4\"."), texts.toString());
	}

	@Test
	void aRewriteBuiltFromAnOlderNameIsRefused() throws Exception {
		Step step = stepInNewUseCase(MP4);
		analyzeAndApplyStep(step);
		Issue issue = noActorIssue(step.getId());
		Position stale = positionWithText(issue, "Set the actor to the primary actor: \"Host downloads the resulting MP4\".");
		// Renamed with no re-run, so the rewrite on the issue is the old one.
		renameStep(step, "Upload the resulting MP4");

		assertThrows(EntityValidationException.class,
				() -> resolve(issue, stale, null, projectUser()));
		assertEquals("Upload the resulting MP4", freshStepName(step.getId()));
	}

	// ---- #468: a step's actor as a role -----------------------------------------------------------

	/**
	 * AC1: "Operator ends the room" shared by an Operator use case and a Conduit Engineer use case
	 * reads wrong in the second. The check offers the primary-actor role; accepting it keeps one
	 * step, named "ends the room", that reads with each use case's own primary actor.
	 */
	@Test
	void aSharedStepNamingOnePrimaryActorGetsThePrimaryActorRoleAndReadsPerUseCase()
			throws Exception {
		newActor(project, "Operator");
		newActor(project, "Conduit Engineer");
		String ts = stamp();
		UseCase operatorCase = newUseCase("Run a session " + ts, "Operator",
				List.of("Operator ends the room " + ts));
		UseCase engineerCase = newUseCase("Respond to an alarm " + ts, "Conduit Engineer",
				List.of());
		AddScenarioToUseCaseCommand share = getProjectCommandFactory()
				.newAddScenarioToUseCaseCommand();
		share.setEditedBy(projectUser());
		share.setUseCase(engineerCase);
		share.setScenario(inTransaction(() -> getProjectRepository()
				.findById(UseCase.class, operatorCase.getId()).getScenario()));
		getCommandHandler().execute(share);
		Step step = firstStep(operatorCase);
		analyzeAndApplyStep(step);

		Issue issue = openIssueStartingWith(step.getId(), "\"Operator ends the room " + ts
				+ "\" names Operator");
		assertEquals(Set.of("Set the actor to the primary actor: \"Primary actor ends the room "
				+ ts + "\".", "Set the actor to Operator: \"Operator ends the room " + ts + "\".",
				"Do nothing."), positionTexts(issue));

		resolve(issue, positionWithText(issue, "Set the actor to the primary actor: \"Primary"
				+ " actor ends the room " + ts + "\"."), null, projectUser());

		assertEquals("ends the room " + ts, freshStepName(step.getId()));
		inTransaction(() -> {
			Step fresh = getProjectRepository().findById(Step.class, step.getId());
			assertEquals(1, fresh.getUsingScenarios().size(), "still one shared step");
			assertEquals("Operator ends the room " + ts, com.rreganjr.requel.project.StepNames
					.render(fresh, getProjectRepository().findById(UseCase.class,
							operatorCase.getId()).getPrimaryActor()));
			assertEquals("Conduit Engineer ends the room " + ts, com.rreganjr.requel.project
					.StepNames.render(fresh, getProjectRepository().findById(UseCase.class,
							engineerCase.getId()).getPrimaryActor()));
			return null;
		});
		// The role is in effect for both use cases, so a re-run raises nothing (the earlier
		// actor-not-in-usecase finding is superseded by it).
		assertEquals(List.of(), findingTypes(inTransaction(() -> assistant.analyze(context(),
				getProjectRepository().findById(Step.class, step.getId())).annotationActions())));
	}

	/** #468: a step whose wording is right in every use case it is in gets no role nudge. */
	@Test
	void aStepNamingThePrimaryActorOfAllItsUseCasesGetsNoRoleNudge() throws Exception {
		Step step = firstStep(newUseCase("Host it " + stamp(), "Host",
				List.of("The host starts the livestream " + stamp())));
		assertEquals(List.of(), findingTypes(inTransaction(() -> assistant.analyze(context(),
				getProjectRepository().findById(Step.class, step.getId())).annotationActions())));
	}

	/** AC2: a scenario in no use case shows the role, and its role steps raise no no-actor. */
	@Test
	void aRoleStepInNoUseCaseRaisesNoNoActorIssue() throws Exception {
		Scenario scenario = newScenario("Loose " + stamp(), List.of(MP4));
		Step step = firstStep(scenario);
		setRole(step, "PRIMARY_ACTOR", null, "downloads the resulting MP4 " + stamp());
		List<AnnotationAction> actions = inTransaction(() -> assistant.analyze(context(),
				getProjectRepository().findById(Step.class, step.getId())).annotationActions());
		assertEquals(List.of(), findingTypes(actions));
		inTransaction(() -> {
			Step fresh = getProjectRepository().findById(Step.class, step.getId());
			assertTrue(com.rreganjr.requel.project.StepNames.display(fresh)
					.startsWith("Primary actor downloads the resulting MP4"));
			return null;
		});
	}

	/** #468: a named actor its use case lacks gets the issue, and one click adds the actor. */
	@Test
	void aNamedActorNotInTheUseCaseCanBeAddedInOneClick() throws Exception {
		newActor(project, "Clerk");
		UseCase useCase = newUseCase("Close the month " + stamp(), "Host",
				List.of("File the report " + stamp()));
		Step step = firstStep(useCase);
		setRole(step, "NAMED", "Clerk", "files the report " + stamp());
		analyzeAndApplyStep(step);

		Issue issue = openIssueStartingWith(step.getId(), "The actor of the step \"Clerk\"");
		resolve(issue, positionWithText(issue, "Add \"Clerk\" to the use case \""
				+ useCase.getName() + "\"."), null, projectUser());

		assertTrue(inTransaction(() -> getProjectRepository().findById(UseCase.class,
				useCase.getId()).getActors().stream()
				.anyMatch(actor -> "Clerk".equals(actor.getName()))));
		analyzeAndApplyStep(step);
		assertTrue(stepIssues(step.getId()).stream().noneMatch(i -> !i.isResolved()));
	}

	/** Decision 2: a role whose action is already another step's name is refused, naming it. */
	@Test
	void aRoleWhoseActionIsTakenIsRefusedNamingTheStep() throws Exception {
		String ts = stamp();
		UseCase useCase = newUseCase("Archive " + ts, "Host", List.of("Archive the room " + ts));
		newScenario("Other " + ts, List.of("archives the room " + ts));
		Step step = firstStep(useCase);
		analyzeAndApplyStep(step);
		Issue issue = noActorIssue(step.getId());

		com.rreganjr.platform.exception.EntityException refused = assertThrows(
				com.rreganjr.platform.exception.EntityException.class,
				() -> resolve(issue, positionWithText(issue, "Set the actor to the primary actor:"
						+ " \"Host archives the room " + ts + "\"."), null, projectUser()));
		assertTrue(refused.getMessage().contains("archives the room " + ts),
				refused.getMessage());
		assertEquals("Archive the room " + ts, freshStepName(step.getId()));
	}

	// ---- helpers --------------------------------------------------------------------------------

	/**
	 * A project named with one dictionary word, once per test class run. A made-up name would also
	 * raise a spelling issue on the same word, and a lexical issue is reused by word and property,
	 * so the two would share one issue.
	 */
	/** #459: a step in a new use case whose primary actor is Host. */
	private Step stepInNewUseCase(String name) throws Exception {
		return firstStep(newUseCase("Holder " + stamp(), "Host", List.of(name)));
	}

	private UseCase newUseCase(String name, String primaryActor, List<String> stepNames)
			throws Exception {
		EditUseCaseCommand command = getProjectCommandFactory().newEditUseCaseCommand();
		command.setEditedBy(projectUser());
		command.setProjectOrDomain(project);
		command.setName(name);
		command.setText("");
		command.setPrimaryActorName(primaryActor);
		command.setStepCommands(stepCommands(stepNames));
		return getCommandHandler().execute(command).getUseCase();
	}

	private Step firstStep(UseCase useCase) {
		return inTransaction(() -> getProjectRepository().findById(UseCase.class, useCase.getId())
				.getScenario().getSteps().get(0));
	}

	private Step firstStep(Scenario scenario) {
		return inTransaction(() -> getProjectRepository().findById(Scenario.class, scenario.getId())
				.getSteps().get(0));
	}

	private Actor actorNamed(String name) {
		return inTransaction(() -> getProjectRepository().findById(Project.class, project.getId())
				.getActors().stream().filter(actor -> name.equals(actor.getName())).findFirst()
				.orElseThrow());
	}

	/** The rewrite positions the assistant offers on the step, in order. */
	private List<String> rewritesOf(Step step) {
		return inTransaction(() -> assistant.analyze(context(),
				getProjectRepository().findById(Step.class, step.getId())).annotationActions())
				.stream().filter(a -> "SET_STEP_ACTOR".equals(a.metadata().get("kind")))
				.map(AnnotationAction::text).toList();
	}

	private void renameStep(Step step, String name) throws Exception {
		EditScenarioStepCommand command = getProjectCommandFactory().newEditScenarioStepCommand();
		command.setEditedBy(projectUser());
		command.setProjectOrDomain(project);
		command.setStep(inTransaction(() -> getProjectRepository().findById(Step.class, step.getId())));
		command.setName(name);
		getCommandHandler().execute(command);
	}

	private void analyzeAndApplyStep(Step step) {
		Step loaded = inTransaction(() -> getProjectRepository().findById(Step.class, step.getId()));
		analysisRequestDispatcher.dispatch(loaded, projectUser(), null);
		AssistantRunEntity queued = assistantRunRepository.findAll().stream()
				.filter(run -> "Step".equals(run.getTargetType())
						&& step.getId().equals(run.getTargetId())
						&& "QUEUED".equals(run.getStatus()))
				.reduce((first, second) -> second)
				.orElseThrow(() -> new AssertionError("no QUEUED run for step " + step.getId()));
		assistantRunWorker.run(queued.getRunId());
	}

	private List<Issue> stepIssues(Long stepId) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			StepImpl step = em.find(StepImpl.class, stepId);
			List<Issue> issues = step.getAnnotations().stream().map(Hibernate::unproxy)
					.filter(Issue.class::isInstance).map(Issue.class::cast)
					.collect(Collectors.toList());
			issues.forEach(issue -> {
				issue.isResolved();
				issue.getPositions().forEach(position -> position.getText());
			});
			return issues;
		} finally {
			em.close();
		}
	}

	private void setRole(Step step, String role, String actorName, String name) throws Exception {
		EditScenarioStepCommand command = getProjectCommandFactory().newEditScenarioStepCommand();
		command.setEditedBy(projectUser());
		command.setProjectOrDomain(project);
		command.setStep(inTransaction(() -> getProjectRepository().findById(Step.class,
				step.getId())));
		command.setName(name);
		command.setActorRoleName(role);
		command.setActorName(actorName);
		command.setAnalysisEnabled(false);
		getCommandHandler().execute(command);
	}

	private Issue openIssueStartingWith(Long stepId, String prefix) {
		List<Issue> open = stepIssues(stepId).stream()
				.filter(i -> !i.isResolved() && i.getText().startsWith(prefix)).toList();
		assertEquals(1, open.size(), "one open issue starting " + prefix + ": " + stepIssues(stepId)
				.stream().map(Issue::getText).toList());
		return open.get(0);
	}

	private Issue noActorIssue(Long stepId) {
		List<Issue> open = stepIssues(stepId).stream()
				.filter(i -> !i.isResolved() && i.getText().contains("doesn't say who does it"))
				.toList();
		assertEquals(1, open.size(), "one open no-actor issue: " + open.stream()
				.map(Issue::getText).toList());
		return open.get(0);
	}

	private static Set<String> positionTexts(Issue issue) {
		return issue.getPositions().stream().map(Position::getText).collect(Collectors.toSet());
	}

	private static Set<Long> positionIds(Issue issue) {
		return issue.getPositions().stream().map(Position::getId).collect(Collectors.toSet());
	}

	private com.rreganjr.requel.project.ActorRole freshStepRole(Long stepId) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			return em.find(StepImpl.class, stepId).getActorRole();
		} finally {
			em.close();
		}
	}

	private String freshStepName(Long stepId) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			return em.find(StepImpl.class, stepId).getName();
		} finally {
			em.close();
		}
	}

	private List<EditScenarioStepCommand> stepCommands(List<String> stepNames) {
		User user = projectUser();
		List<EditScenarioStepCommand> steps = new ArrayList<>();
		for (String stepName : stepNames) {
			EditScenarioStepCommand step = getProjectCommandFactory().newEditScenarioStepCommand();
			step.setEditedBy(user);
			step.setProjectOrDomain(project);
			step.setName(stepName);
			step.setText("");
			step.setScenarioTypeName(ScenarioType.Primary.name());
			steps.add(step);
		}
		return steps;
	}

	private Project namedProject(String name) throws Exception {
		EditProjectCommand command = getProjectCommandFactory().newEditProjectCommand();
		command.setEditedBy(projectUser());
		command.setName(name);
		command.setOrganizationName("Org " + name + " " + stamp());
		return getCommandHandler().execute(command).getProject();
	}

	/** Run the step check on the scenario through the real dispatch, run and apply path. */
	private void analyzeAndApply(Scenario scenario) {
		Scenario loaded = inTransaction(
				() -> getProjectRepository().findById(Scenario.class, scenario.getId()));
		analysisRequestDispatcher.dispatch(loaded, projectUser(), null);
		AssistantRunEntity queued = assistantRunRepository.findAll().stream()
				.filter(run -> "Scenario".equals(run.getTargetType())
						&& scenario.getId().equals(run.getTargetId())
						&& "QUEUED".equals(run.getStatus()))
				.reduce((first, second) -> second)
				.orElseThrow(() -> new AssertionError("no QUEUED run for " + scenario.getId()));
		assistantRunWorker.run(queued.getRunId());
	}

	private LexicalIssue projectAsActorIssue(Long scenarioId) {
		return lexicalIssuesOn(scenarioId).stream()
				.filter(issue -> isProjectAsActor(issue) && !issue.isResolved())
				.findFirst().orElseThrow(() -> new AssertionError("no project-as-actor issue: "
						+ lexicalIssuesOn(scenarioId).stream().map(LexicalIssue::getText).toList()));
	}

	private static boolean isProjectAsActor(LexicalIssue issue) {
		return issue.getText() != null && issue.getText().contains("names the project as its actor");
	}

	private List<LexicalIssue> lexicalIssuesOn(Long scenarioId) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			ScenarioImpl scenario = em.find(ScenarioImpl.class, scenarioId);
			List<LexicalIssue> issues = scenario.getAnnotations().stream().map(Hibernate::unproxy)
					.filter(LexicalIssue.class::isInstance).map(LexicalIssue.class::cast)
					.collect(Collectors.toList());
			issues.forEach(issue -> {
				issue.isResolved();
				Hibernate.initialize(issue.getPositions());
			});
			return issues;
		} finally {
			em.close();
		}
	}

	private String freshScenarioName(Long scenarioId) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			return em.find(ScenarioImpl.class, scenarioId).getName();
		} finally {
			em.close();
		}
	}

	private List<AnnotationAction> analyzeStep(String name) throws Exception {
		Scenario scenario = newScenario("Holder " + stamp(), List.of(name));
		return inTransaction(() -> {
			Scenario loaded = getProjectRepository().findById(Scenario.class, scenario.getId());
			Step step = loaded.getSteps().get(0);
			return assistant.analyze(context(), step).annotationActions();
		});
	}

	private List<AnnotationAction> analyzeScenario(String name) throws Exception {
		Scenario scenario = newScenario(name, List.of());
		return inTransaction(() -> assistant.analyze(context(),
				getProjectRepository().findById(Scenario.class, scenario.getId()))
				.annotationActions());
	}

	private <T> T inTransaction(java.util.function.Supplier<T> work) {
		return new TransactionTemplate(transactionManager).execute(status -> work.get());
	}

	private static AnnotationAction only(List<AnnotationAction> actions, String findingType) {
		List<AnnotationAction> issues = actions.stream()
				.filter(a -> findingType.equals(a.metadata().get("findingType"))).toList();
		assertEquals(1, issues.size(), "expected one " + findingType + " issue: " + texts(actions));
		return issues.get(0);
	}

	private static List<String> findingTypes(List<AnnotationAction> actions) {
		return actions.stream()
				.filter(a -> a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE
						|| a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_NOTE)
				.map(a -> String.valueOf(a.metadata().get("findingType")))
				.collect(Collectors.toList());
	}

	private static List<String> texts(List<AnnotationAction> actions) {
		return actions.stream().map(AnnotationAction::text).toList();
	}

	private AssistantContext context() {
		return new AssistantContext(UUID.randomUUID(), new UserRef(3L, "project"),
				new UserRef(11L, "assistant"), EntityRef.of("Project", project.getId()), Locale.US,
				Clock.systemUTC(), Map.of());
	}

	private void newActor(Project project, String name) throws Exception {
		EditActorCommand command = getProjectCommandFactory().newEditActorCommand();
		command.setEditedBy(projectUser());
		command.setActorContainer(project);
		command.setName(name);
		command.setText("The " + name.toLowerCase(Locale.ROOT) + ".");
		getCommandHandler().execute(command);
	}

	private Scenario newScenario(String name, List<String> stepNames) throws Exception {
		User user = projectUser();
		List<EditScenarioStepCommand> steps = new ArrayList<>();
		for (String stepName : stepNames) {
			EditScenarioStepCommand step = getProjectCommandFactory().newEditScenarioStepCommand();
			step.setEditedBy(user);
			step.setProjectOrDomain(project);
			step.setName(stepName);
			step.setText("");
			step.setScenarioTypeName(ScenarioType.Primary.name());
			steps.add(step);
		}
		EditScenarioCommand command = getProjectCommandFactory().newEditScenarioCommand();
		command.setEditedBy(user);
		command.setProjectOrDomain(project);
		command.setName(name);
		command.setScenarioTypeName(ScenarioType.Primary.name());
		command.setStepCommands(steps);
		return getCommandHandler().execute(command).getScenario();
	}
}
