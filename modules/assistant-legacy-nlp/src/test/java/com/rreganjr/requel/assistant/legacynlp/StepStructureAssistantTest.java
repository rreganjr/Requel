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
package com.rreganjr.requel.assistant.legacynlp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.rreganjr.nlp.dictionary.NLPProcessorFactory;
import com.rreganjr.nlp.dictionary.NLPText;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.ActorRole;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.UseCase;

class StepStructureAssistantTest {

	private final NLPProcessorFactory nlpProcessorFactory = mock(NLPProcessorFactory.class);
	private final StepStructureAssistant assistant = new StepStructureAssistant(nlpProcessorFactory);

	@Test
	void declaresIdentityAndTargetType() {
		assertThat(assistant.assistantId()).isEqualTo("legacy-step-structure");
		assertThat(assistant.targetType()).isEqualTo(Step.class);
	}

	@Test
	void blankStepNameProducesNoActions() {
		AssistantResult result = assistant.analyze(context(), step(""));
		assertThat(result.annotationActions()).isEmpty();
	}

	// ---- #349: how the name reads ---------------------------------------------------------------

	@Test
	void aStepWithNoVerbGetsTheLowUnreadableIssueAndNoNote() {
		NLPText text = mock(NLPText.class);
		when(nlpProcessorFactory.processText("Webinar")).thenReturn(text);

		AssistantResult result = assistant.analyze(context(), step("Webinar"));

		AnnotationAction issue = result.annotationActions().get(0);
		assertThat(issue.actionType()).isEqualTo(AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE);
		assertThat(issue.severity()).isEqualTo("LOW");
		assertThat(issue.metadata()).containsEntry("mustResolve", false)
				.containsEntry("findingType", "unreadable");
		assertThat(issue.text()).isEqualTo("Requel couldn't read \"Webinar\" as \"<actor> does"
				+ " something\" or a command. A step names who does it.");
		assertThat(result.annotationActions()).extracting(AnnotationAction::text)
				.contains("Do nothing.");
		assertThat(result.annotationActions()).noneMatch(
				a -> a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_NOTE);
	}

	@Test
	void aCommandStepGetsTheLowNoActorIssue() {
		NLPText text = command();
		when(nlpProcessorFactory.processText("Start the livestream")).thenReturn(text);

		AssistantResult result = assistant.analyze(context(), step("Start the livestream"));

		AnnotationAction issue = result.annotationActions().get(0);
		assertThat(issue.severity()).isEqualTo("LOW");
		assertThat(issue.metadata()).containsEntry("findingType", "no-actor")
				.containsEntry("mustResolve", false);
		assertThat(issue.text()).startsWith(
				"The step \"Start the livestream\" doesn't say who does it.");
		assertThat(issue.actionKey()).isEqualTo("legacy-step-structure:Step:1:no-actor");
	}

	@Test
	void aCommandScenarioNameRaisesNothing() {
		NLPText text = command();
		when(nlpProcessorFactory.processText("Create the room")).thenReturn(text);

		AssistantResult result = assistant.analyze(context(), scenario("Create the room"));

		assertThat(result.annotationActions()).isEmpty();
	}

	@Test
	void aScenarioNameWithNoVerbGetsTheScenarioWording() {
		when(nlpProcessorFactory.processText("Webinar dry run")).thenReturn(mock(NLPText.class));

		AssistantResult result = assistant.analyze(context(), scenario("Webinar dry run"));

		assertThat(result.annotationActions().get(0).text()).isEqualTo("Requel couldn't read"
				+ " \"Webinar dry run\" as \"<actor> does something\" or a command.");
	}

	@Test
	void theSystemIsTheImplicitActor() {
		assertThat(StepStructureAssistant.isSystem("The system")).isTrue();
		assertThat(StepStructureAssistant.isSystem(" SYSTEM ")).isTrue();
		assertThat(StepStructureAssistant.isSystem("The system admin")).isFalse();
	}

	@Test
	void aSubjectNamesAnActorIgnoringArticleAndCase() {
		assertThat(StepStructureAssistant.namesActor("The host", "Host")).isTrue();
		assertThat(StepStructureAssistant.namesActor("host", "The Host")).isTrue();
		assertThat(StepStructureAssistant.namesActor("An admin", "Admin")).isTrue();
		assertThat(StepStructureAssistant.namesActor("The hostess", "Host")).isFalse();
	}

	@Test
	void theProjectNameIsFoundWithOrWithoutTheAndKeptAsWritten() {
		assertThat(StepStructureAssistant.projectNameIn("Roundtable", "Roundtable"))
				.isEqualTo("Roundtable");
		assertThat(StepStructureAssistant.projectNameIn("the roundtable", "Roundtable"))
				.isEqualTo("roundtable");
		assertThat(StepStructureAssistant.projectNameIn("The host", "Roundtable")).isNull();
	}

	private static NLPText command() {
		return command(null);
	}

	private static NLPText command(String verb) {
		NLPText text = mock(NLPText.class);
		NLPText verbText = mock(NLPText.class);
		when(verbText.getText()).thenReturn(verb);
		when(text.getPrimaryVerb()).thenReturn(verbText);
		when(text.isImperative()).thenReturn(true);
		return text;
	}

	// ---- #459: the example and the rewrites -----------------------------------------------------

	private static final String MP4 = "Download the resulting MP4";

	@Test
	void oneUseCaseGivesThePrimaryActorAndSystemRolesAndDoNothing() {
		parses(MP4, "Download");
		Step step = stepIn(MP4, useCase("Run a session", actor("Host")));

		List<AnnotationAction> actions = assistant.analyze(context(), step).annotationActions();

		assertThat(actions.get(0).text()).isEqualTo("The step \"Download the resulting MP4\""
				+ " doesn't say who does it. Set its actor: as the primary actor it reads \"Host"
				+ " downloads the resulting MP4\", or as the system \"The system downloads the"
				+ " resulting MP4\".");
		assertThat(positions(actions)).containsExactly(
				"Set the actor to the primary actor: \"Host downloads the resulting MP4\".",
				"Set the actor to the system: \"The system downloads the resulting MP4\".",
				"Do nothing.");
		AnnotationAction primary = actions.get(1);
		assertThat(primary.actionKey())
				.isEqualTo("legacy-step-structure:Step:1:no-actor:set-actor:primary");
		assertThat(primary.parentActionKey()).isEqualTo("legacy-step-structure:Step:1:no-actor");
		assertThat(primary.metadata()).containsEntry("kind", "SET_STEP_ACTOR")
				.containsEntry("proposedName", "downloads the resulting MP4")
				.containsEntry("expectedName", MP4).containsEntry("actorRole", "PRIMARY_ACTOR")
				.doesNotContainKey("actorName");
	}

	@Test
	void additionalActorsComeAfterThePrimaryActorAndBeforeTheSystem() {
		parses("Read the ticket", "Read");
		Step step = stepIn("Read the ticket",
				useCase("Fix a bug", actor("User"), actor("Jira"), actor("Claude")));

		List<AnnotationAction> actions = assistant.analyze(context(), step).annotationActions();

		assertThat(positions(actions)).containsExactly(
				"Set the actor to the primary actor: \"User reads the ticket\".",
				"Set the actor to Claude: \"Claude reads the ticket\".",
				"Set the actor to Jira: \"Jira reads the ticket\".",
				"Set the actor to the system: \"The system reads the ticket\".", "Do nothing.");
		assertThat(actions.get(2).metadata()).containsEntry("actorRole", "NAMED")
				.containsEntry("actorName", "Claude");
	}

	@Test
	void useCasesWithDifferentPrimaryActorsGetThePrimaryActorRoleToo() {
		parses("End the room", "End");
		Step step = stepIn("End the room", useCase("Run a session", actor("Operator")),
				useCase("Respond to an alarm", actor("Engineer")));

		List<AnnotationAction> actions = assistant.analyze(context(), step).annotationActions();

		assertThat(actions.get(0).text()).isEqualTo("The step \"End the room\" doesn't say who"
				+ " does it, and the use cases that share it have different primary actors (Respond"
				+ " to an alarm: Engineer; Run a session: Operator). Set its actor to the primary"
				+ " actor, so it reads with each use case's own, or to the system: \"The system"
				+ " ends the room\".");
		assertThat(positions(actions)).containsExactly(
				"Set the actor to the primary actor: \"Primary actor ends the room\".",
				"Set the actor to the system: \"The system ends the room\".", "Do nothing.");
	}

	@Test
	void aStepInNoUseCaseGetsThePrimaryActorAndSystemRoles() {
		parses(MP4, "Download");

		List<AnnotationAction> actions = assistant.analyze(context(), step(MP4)).annotationActions();

		assertThat(actions.get(0).text()).isEqualTo("The step \"Download the resulting MP4\""
				+ " doesn't say who does it, and it isn't in a use case. Set its actor to the primary"
				+ " actor, so it reads \"Primary actor downloads the resulting MP4\" until it joins"
				+ " one, or to the system.");
		assertThat(positions(actions)).containsExactly(
				"Set the actor to the primary actor: \"Primary actor downloads the resulting MP4\".",
				"Set the actor to the system: \"The system downloads the resulting MP4\".",
				"Do nothing.");
	}

	// ---- #468: steps with an actor role ---------------------------------------------------------

	@Test
	void aStepWithARoleRaisesNoNoActorIssueEvenInNoUseCase() {
		Step step = step("ends the room");
		when(step.getActorRole()).thenReturn(ActorRole.PRIMARY_ACTOR);

		assertThat(assistant.analyze(context(), step).annotationActions()).isEmpty();
	}

	@Test
	void aNamedActorNotInAUseCaseGetsTheIssueWithAnAddToUseCasePosition() {
		Actor clerk = actor("Clerk");
		Step step = stepIn("files the report", useCase("Close the month", actor("Manager")));
		when(step.getActorRole()).thenReturn(ActorRole.NAMED);
		when(step.getActor()).thenReturn(clerk);

		List<AnnotationAction> actions = assistant.analyze(context(), step).annotationActions();

		assertThat(actions.get(0).text()).isEqualTo("The actor of the step \"Clerk\" is not"
				+ " associated to the use case \"Close the month\".");
		assertThat(positions(actions)).containsExactly(
				"Add \"Clerk\" to the use case \"Close the month\".", "Do nothing.");
		assertThat(actions.get(1).metadata()).containsEntry("kind", "ADD_ACTOR_TO_USE_CASE")
				.containsEntry("actorName", "Clerk").containsEntry("useCaseName", "Close the month");
	}

	@Test
	void aNamedActorInItsUseCaseRaisesNothing() {
		Actor clerk = actor("Clerk");
		Step step = stepIn("files the report", useCase("Close the month", actor("Manager"), clerk));
		when(step.getActorRole()).thenReturn(ActorRole.NAMED);
		when(step.getActor()).thenReturn(clerk);

		assertThat(assistant.analyze(context(), step).annotationActions()).isEmpty();
	}

	@Test
	void aRewriteLongerThanANameCanBeIsNotOffered() {
		String name = "Download " + "x".repeat(246);
		parses(name, "Download");
		Step step = stepIn(name, useCase("Run a session", actor("Host")));

		assertThat(positions(assistant.analyze(context(), step).annotationActions()))
				.containsExactly("Do nothing.");
	}

	private void parses(String name, String verb) {
		NLPText parsed = command(verb);
		when(nlpProcessorFactory.processText(name)).thenReturn(parsed);
	}

	private static List<String> positions(List<AnnotationAction> actions) {
		return actions.stream()
				.filter(a -> a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION)
				.map(AnnotationAction::text).toList();
	}

	private static Actor actor(String name) {
		Actor actor = mock(Actor.class);
		when(actor.getName()).thenReturn(name);
		return actor;
	}

	/** A use case whose primary scenario holds the step; the first actor is the primary. */
	private static UseCase useCase(String name, Actor primary, Actor... additional) {
		UseCase useCase = mock(UseCase.class);
		when(useCase.getName()).thenReturn(name);
		when(useCase.getPrimaryActor()).thenReturn(primary);
		when(useCase.getActors()).thenReturn(new LinkedHashSet<>(List.of(additional)));
		return useCase;
	}

	private static Step stepIn(String name, UseCase... useCases) {
		Step step = step(name);
		Set<Scenario> scenarios = new LinkedHashSet<>();
		for (UseCase useCase : useCases) {
			Scenario scenario = mock(Scenario.class);
			when(scenario.getUsingUseCases()).thenReturn(Set.of(useCase));
			when(useCase.getScenario()).thenReturn(scenario);
			scenarios.add(scenario);
		}
		when(step.getUsingScenarios()).thenReturn(scenarios);
		return step;
	}

	private static Step scenario(String name) {
		Scenario scenario = mock(Scenario.class);
		Class<?> entityInterface = Scenario.class;
		doReturn(entityInterface).when(scenario).getProjectOrDomainEntityInterface();
		when(scenario.getId()).thenReturn(2L);
		when(scenario.getName()).thenReturn(name);
		return scenario;
	}

	private static AssistantContext context() {
		return new AssistantContext(UUID.randomUUID(), new UserRef(3L, "ron"),
				new UserRef(11L, "assistant"), EntityRef.of("Project", 7L), Locale.US,
				Clock.systemUTC(), Map.of());
	}

	private static Step step(String name) {
		Step step = mock(Step.class);
		Class<?> entityInterface = Step.class;
		doReturn(entityInterface).when(step).getProjectOrDomainEntityInterface();
		when(step.getId()).thenReturn(1L);
		when(step.getName()).thenReturn(name);
		return step;
	}
}
