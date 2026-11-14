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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.CleanupPolicy;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.UseCase;

/** #455: the order of a scenario's steps, with each step's #349 reading stubbed. */
class StepSequenceAssistantTest {

	private final Map<String, StepReading> readings = new HashMap<>();
	private final StepSequenceAssistant assistant = new StepSequenceAssistant(
			name -> readings.getOrDefault(name, StepReading.unreadable()));

	private final Project project = mock(Project.class);
	private final Set<Actor> actors = new LinkedHashSet<>();
	private final Set<UseCase> useCases = new LinkedHashSet<>();
	private final Actor member = actor("Member");
	private final Actor clerk = actor("Clerk");

	StepSequenceAssistantTest() {
		when(project.getName()).thenReturn("Tool Library");
		when(project.getActors()).thenReturn(actors);
		when(project.getUseCases()).thenReturn(useCases);
		actors.add(member);
		actors.add(clerk);
	}

	@Test
	void declaresItself() {
		assertThat(assistant.assistantId()).isEqualTo("legacy-step-sequence");
		assertThat(assistant.targetType()).isEqualTo(Scenario.class);
		assertThat(assistant.cleanupPolicy()).isEqualTo(CleanupPolicy.AUTO_RESOLVE_IF_UNTOUCHED);
		assertThat(assistant.projectSwitchable()).isTrue();
		assertThat(assistant.displayName()).isEqualTo("Actor and system turns");
	}

	@Test
	void servesThePostEditRunAndTheStepSequenceTaskOnly() {
		assertThat(assistant.handlesTask(null)).isTrue();
		assertThat(assistant.handlesTask("STEP_SEQUENCE")).isTrue();
		assertThat(assistant.handlesTask("REQUIREMENTS_REVIEW")).isFalse();
		assertThat(assistant.handlesTask("POLICY_REVIEW")).isFalse();
	}

	@Test
	void anActorAndTheSystemTakingTurnsRaisesNothing() {
		actorStep("The member asks for a drill", "The member");
		systemStep("The system shows the drills on the shelf");
		actorStep("Member picks one", "Member");
		systemStep("The system records the loan");

		assertThat(analyze("The member asks for a drill", "The system shows the drills on the shelf",
				"Member picks one", "The system records the loan")).isEmpty();
	}

	@Test
	void twoSystemStepsInARowRaiseOneLowIssueNamingBoth() {
		actorStep("Member asks for a drill", "Member");
		systemStep("The system records the loan");
		systemStep("The system prints a slip");

		List<AnnotationAction> actions = analyze("Member asks for a drill",
				"The system records the loan", "The system prints a slip");

		AnnotationAction issue = actions.get(0);
		assertThat(issue.actionType()).isEqualTo(AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE);
		assertThat(issue.actionKey()).matches("legacy-step-sequence:Scenario:2:sequence:[0-9a-f]{12}");
		assertThat(issue.targetRef()).isEqualTo(EntityRef.of("Scenario", 2L));
		assertThat(issue.severity()).isEqualTo("LOW");
		assertThat(issue.metadata()).containsEntry("mustResolve", false)
				.containsEntry("findingType", "step-sequence");
		assertThat(issue.text()).isEqualTo("The steps of \"Borrow a tool\" don't alternate between"
				+ " an actor and the system: \"The system records the loan\" and \"The system prints"
				+ " a slip\" are both system steps, with no actor step between them. An actor acts"
				+ " and the system responds: add the step that's missing, or combine two steps by"
				+ " the same actor unless something outside the system happens between them.");
		assertThat(positions(actions)).containsExactly(
				"Merge into \"The system records the loan and prints a slip\".", "Leave the steps as they are.");
		assertThat(actions.get(2).parentActionKey()).isEqualTo(issue.actionKey());
	}

	@Test
	void aScenarioThatStartsWithASystemStepRaisesTheIssue() {
		systemStep("The system shows the overdue tools");
		actorStep("Clerk calls the member", "Clerk");

		assertThat(issueText("The system shows the overdue tools", "Clerk calls the member"))
				.contains(": it starts with the system step \"The system shows the overdue tools\".");
	}

	@Test
	void theSameActorTwiceInARowRaisesTheIssueNamingTheActor() {
		actorStep("Member fills in the form", "Member");
		actorStep("The member submits it", "The member");
		systemStep("The system records the request");

		assertThat(issueText("Member fills in the form", "The member submits it",
				"The system records the request"))
				.contains(": \"Member fills in the form\" and \"The member submits it\" are both by"
						+ " Member, with nothing from the system between them.");
	}

	@Test
	void differentActorsInARowAreAHandOffAndRaiseNothing() {
		actorStep("Member hands the tool to the clerk", "Member");
		actorStep("Clerk inspects it for damage", "Clerk");
		systemStep("The system records the return");

		assertThat(analyze("Member hands the tool to the clerk", "Clerk inspects it for damage",
				"The system records the return")).isEmpty();
	}

	@Test
	void anUnknownSubjectCountsAsWrittenWithoutItsArticle() {
		actorStep("The librarian checks the shelf", "The librarian");
		actorStep("Librarian finds the drill", "Librarian");

		assertThat(issueText("The librarian checks the shelf", "Librarian finds the drill"))
				.contains("are both by librarian,");
	}

	@Test
	void aCommandCountsAsTheSinglePrimaryActorOfTheScenariosUseCases() {
		inUseCase(member);
		actorStep("Member fills in the form", "Member");
		readings.put("Submit the form", StepReading.command("Submit"));

		assertThat(issueText("Member fills in the form", "Submit the form"))
				.contains("are both by Member,");
	}

	@Test
	void aCommandWithNoSinglePrimaryActorIsUnknown() {
		inUseCase(member);
		inUseCase(clerk);
		actorStep("Member fills in the form", "Member");
		readings.put("Submit the form", StepReading.command("Submit"));

		assertThat(analyze("Member fills in the form", "Submit the form")).isEmpty();
	}

	@Test
	void aCommandInNoUseCaseIsUnknown() {
		readings.put("Submit the form", StepReading.command("Submit"));
		actorStep("Member submits it again", "Member");

		assertThat(analyze("Submit the form", "Member submits it again")).isEmpty();
	}

	@Test
	void aSubScenarioCountsByItsName() {
		systemStep("The system shows the account");
		systemStep("The system renews the loan");
		Scenario renew = mock(Scenario.class);
		when(renew.getName()).thenReturn("The system renews the loan");
		when(renew.getProjectOrDomain()).thenReturn(project);
		actorStep("Member opens the account page", "Member");
		Step open = step("Member opens the account page");
		Step show = step("The system shows the account");

		assertThat(issueTextOf(scenario(List.of(open, show, renew))))
				.contains("\"The system shows the account\" and \"The system renews the loan\" are"
						+ " both system steps");
	}

	@Test
	void anUnreadableStepBreaksTheSequence() {
		systemStep("The system records the loan");
		systemStep("The system prints a slip");
		actorStep("Member signs", "Member");

		// "Webinar" reads as nothing, so the two system steps either side of it aren't a pair
		assertThat(analyze("Member signs", "The system records the loan", "Webinar",
				"The system prints a slip")).isEmpty();
	}

	@Test
	void anUnknownFirstStepMeansNoStartCheck() {
		systemStep("The system records the loan");

		assertThat(analyze("Webinar", "The system records the loan")).isEmpty();
	}

	@Test
	void theProjectNamedAsTheActorIsUnknown() {
		actorStep("The Tool Library records the loan", "The Tool Library");
		systemStep("The system prints a slip");

		assertThat(analyze("The Tool Library records the loan", "The system prints a slip"))
				.isEmpty();
	}

	@Test
	void everyBreakIsListedInOneIssueInOrder() {
		systemStep("The system shows the form");
		actorStep("Member fills in the form", "Member");
		actorStep("Member submits it", "Member");
		systemStep("The system records the request");
		systemStep("The system emails the clerk");

		List<AnnotationAction> actions = analyze("The system shows the form",
				"Member fills in the form", "Member submits it", "The system records the request",
				"The system emails the clerk");

		assertThat(positions(actions)).containsExactly(
				"Merge into \"Member fills in the form and submits it\".",
				"Merge into \"The system records the request and emails the clerk\".",
				"Leave the steps as they are.");
		assertThat(actions.get(0).text()).contains(": it starts with the system step \"The system"
				+ " shows the form\"; \"Member fills in the form\" and \"Member submits it\" are both"
				+ " by Member, with nothing from the system between them; \"The system records the"
				+ " request\" and \"The system emails the clerk\" are both system steps, with no"
				+ " actor step between them. An actor acts");
	}

	@Test
	void anEmptyScenarioOrASingleActorStepRaisesNothing() {
		actorStep("Member signs", "Member");

		assertThat(analyze()).isEmpty();
		assertThat(analyze("Member signs")).isEmpty();
	}

	@Test
	void aFailureIsLoggedAndRaisesNothing() {
		Scenario scenario = mock(Scenario.class);
		when(scenario.getId()).thenReturn(2L);
		when(scenario.getSteps()).thenThrow(new IllegalStateException("lazy"));

		assertThat(assistant.analyze(context(), scenario).annotationActions()).isEmpty();
	}

	// ---- #472: merging a pair -------------------------------------------------------------------

	@Test
	void aSameActorPairGetsAMergePositionBeforeDoNothing() {
		actorStep("Member fills in the form", "Member");
		actorStep("The member submits it", "The member");
		systemStep("The system records the request");
		Step fills = step("Member fills in the form");
		Step submits = step("The member submits it");

		List<AnnotationAction> actions = assistant.analyze(context(),
				scenario(List.of(fills, submits, step("The system records the request"))))
				.annotationActions();

		AnnotationAction merge = actions.get(1);
		assertThat(merge.actionType())
				.isEqualTo(AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION);
		assertThat(merge.text()).isEqualTo(
				"Merge into \"Member fills in the form and submits it\".");
		assertThat(merge.actionKey()).isEqualTo(actions.get(0).actionKey() + ":merge:"
				+ fills.getId() + ":" + submits.getId());
		assertThat(merge.metadata()).containsEntry("kind", "MERGE_STEPS")
				.containsEntry("proposedName", "Member fills in the form and submits it")
				.containsEntry("expectedName", "Member fills in the form")
				.containsEntry("expectedSecondName", "The member submits it");
		assertThat(actions.get(2).text()).isEqualTo("Leave the steps as they are.");
	}

	@Test
	void aCommandSecondStepIsConjugatedAndATrailingPeriodDropped() {
		inUseCase(member);
		actorStep("Member fills in the form.", "Member");
		readings.put("Submit the form", StepReading.command("Submit"));

		assertThat(positions(analyze("Member fills in the form.", "Submit the form")))
				.containsExactly("Merge into \"Member fills in the form and submits the form\".",
						"Leave the steps as they are.");
	}

	@Test
	void noMergeWhenTheFirstStepIsACommand() {
		inUseCase(member);
		readings.put("Fill in the form", StepReading.command("Fill"));
		actorStep("Member submits it", "Member");

		assertThat(positions(analyze("Fill in the form", "Member submits it")))
				.containsExactly("Leave the steps as they are.");
	}

	@Test
	void noMergeWhenTheSecondStepDoesntStartWithItsSubject() {
		actorStep("Member fills in the form", "Member");
		actorStep("Then the member submits it", "the member");

		assertThat(positions(analyze("Member fills in the form", "Then the member submits it")))
				.containsExactly("Leave the steps as they are.");
	}

	@Test
	void noMergeWhenTheFirstStepIsSharedWithAnotherScenario() {
		actorStep("Member fills in the form", "Member");
		actorStep("Member submits it", "Member");
		Step fills = step("Member fills in the form");
		when(fills.getUsingScenarios()).thenReturn(Set.of(mock(Scenario.class)));

		assertThat(positions(assistant.analyze(context(),
				scenario(List.of(fills, step("Member submits it")))).annotationActions()))
				.containsExactly("Leave the steps as they are.");
	}

	@Test
	void noMergeWithASubScenario() {
		systemStep("The system shows the account");
		systemStep("The system renews the loan");
		Scenario renew = mock(Scenario.class);
		when(renew.getName()).thenReturn("The system renews the loan");
		when(renew.getProjectOrDomain()).thenReturn(project);

		assertThat(positions(assistant.analyze(context(),
				scenario(List.of(step("The system shows the account"), renew)))
				.annotationActions())).containsExactly("Leave the steps as they are.");
	}

	@Test
	void noMergeLongerThanANameCanBe() {
		String longer = "Member fills in the " + "very ".repeat(50) + "long form";
		actorStep(longer, "Member");
		actorStep("Member submits it", "Member");

		assertThat(positions(analyze(longer, "Member submits it"))).containsExactly("Leave the steps as they are.");
	}

	@Test
	void aScenarioStartingWithASystemStepGetsNoMerge() {
		systemStep("The system shows the form");
		actorStep("Member fills in the form", "Member");

		assertThat(positions(analyze("The system shows the form", "Member fills in the form")))
				.containsExactly("Leave the steps as they are.");
	}

	/** #472: the same breaks keep their key; different ones get another. */
	@Test
	void theIssueIsKeyedByTheBreaksItLists() {
		actorStep("Member asks for a drill", "Member");
		systemStep("The system records the loan");
		systemStep("The system prints a slip");
		systemStep("The system emails the member");

		String once = analyze("Member asks for a drill", "The system records the loan",
				"The system prints a slip").get(0).actionKey();
		String again = analyze("Member asks for a drill", "The system records the loan",
				"The system prints a slip").get(0).actionKey();
		String other = analyze("Member asks for a drill", "The system records the loan",
				"The system emails the member").get(0).actionKey();

		assertThat(again).isEqualTo(once);
		assertThat(other).isNotEqualTo(once).startsWith("legacy-step-sequence:Scenario:2:sequence:");
	}

	// ---- helpers --------------------------------------------------------------------------------

	private static List<String> positions(List<AnnotationAction> actions) {
		return actions.stream()
				.filter(a -> a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION)
				.map(AnnotationAction::text).toList();
	}

	private void actorStep(String name, String subject) {
		readings.put(name, StepReading.subject(subject));
	}

	private void systemStep(String name) {
		readings.put(name, StepReading.subject(name.startsWith("The system") ? "The system" : "system"));
	}

	/** A use case with {@code primary}, whose primary scenario is the one under test. */
	private void inUseCase(Actor primary) {
		UseCase useCase = mock(UseCase.class);
		when(useCase.getName()).thenReturn("Use case " + (useCases.size() + 1));
		when(useCase.getPrimaryActor()).thenReturn(primary);
		useCases.add(useCase);
	}

	private List<AnnotationAction> analyze(String... stepNames) {
		List<Step> steps = new ArrayList<>();
		for (String name : stepNames) {
			steps.add(step(name));
		}
		return assistant.analyze(context(), scenario(steps)).annotationActions();
	}

	private String issueText(String... stepNames) {
		List<AnnotationAction> actions = analyze(stepNames);
		assertThat(actions).isNotEmpty();
		return actions.get(0).text();
	}

	private String issueTextOf(Scenario scenario) {
		List<AnnotationAction> actions = assistant.analyze(context(), scenario).annotationActions();
		assertThat(actions).isNotEmpty();
		return actions.get(0).text();
	}

	private Scenario scenario(List<Step> steps) {
		Scenario scenario = mock(Scenario.class);
		when(scenario.getId()).thenReturn(2L);
		when(scenario.getName()).thenReturn("Borrow a tool");
		when(scenario.getSteps()).thenReturn(steps);
		when(scenario.getProjectOrDomain()).thenReturn(project);
		for (UseCase useCase : useCases) {
			when(useCase.getScenario()).thenReturn(scenario);
		}
		return scenario;
	}

	private long nextStepId = 100;

	private Step step(String name) {
		Step step = mock(Step.class);
		when(step.getId()).thenReturn(nextStepId++);
		when(step.getName()).thenReturn(name);
		when(step.getProjectOrDomain()).thenReturn(project);
		return step;
	}

	private static Actor actor(String name) {
		Actor actor = mock(Actor.class);
		when(actor.getName()).thenReturn(name);
		return actor;
	}

	private static AssistantContext context() {
		return new AssistantContext(UUID.randomUUID(), new UserRef(3L, "ron"),
				new UserRef(11L, "assistant"), EntityRef.of("Project", 7L), Locale.US,
				Clock.systemUTC(), Map.of());
	}
}
