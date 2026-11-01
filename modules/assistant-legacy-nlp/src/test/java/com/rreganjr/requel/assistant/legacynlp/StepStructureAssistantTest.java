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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.rreganjr.nlp.dictionary.NLPProcessorFactory;
import com.rreganjr.nlp.dictionary.NLPText;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;

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
		NLPText text = mock(NLPText.class);
		when(text.getPrimaryVerb()).thenReturn(mock(NLPText.class));
		when(text.isImperative()).thenReturn(true);
		return text;
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
