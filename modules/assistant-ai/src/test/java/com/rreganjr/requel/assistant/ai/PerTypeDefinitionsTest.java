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
package com.rreganjr.requel.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.CleanupPolicy;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.assistant.core.AssistantRunWorker;
import com.rreganjr.requel.assistant.core.context.EntityContextPack;
import com.rreganjr.requel.assistant.core.context.EntityContextPackBuilder;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinition;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinitionStore;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinitionValidator;
import com.rreganjr.requel.assistant.core.definition.BundledDefinitions;
import com.rreganjr.requel.assistant.core.definition.VocabularyEntry;
import com.rreganjr.requel.assistant.core.persistence.AssistantUsageRepository;
import com.rreganjr.requel.project.TextEntity;

/** Issue #263: the bundled per-type definitions and what the executor does with them. */
class PerTypeDefinitionsTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final EntityContextPackBuilder packBuilder = mock(EntityContextPackBuilder.class);
	private AiAnalysisResponse reply = response(List.of());
	private final AiAnalysisClient client = request -> reply;
	private final List<AssistantDefinition> bundled = BundledDefinitions.load(objectMapper,
			new AssistantDefinitionValidator(16000));

	@Test
	void everyReviewableTypeHasItsOwnDefinitionBesideTheFallback() {
		Set<String> covered = new HashSet<>();
		for (AssistantDefinition d : bundled) {
			if (d.key().equals(RequirementsReview.ASSISTANT_ID)) {
				assertThat(d.scope()).isEmpty();
				assertThat(d.outputSchemaVersion()).isEqualTo("1");
				assertThat(d.instructions()).doesNotContain("{{vocabulary}}");
				continue;
			}
			assertThat(d.scope()).hasSize(1);
			covered.addAll(d.scope());
			assertThat(d.outputSchemaVersion()).as(d.key()).isEqualTo("2");
			assertThat(d.instructions()).as(d.key()).contains("{{vocabulary}}")
					.contains("Do NOT restate").contains("suggestedIssueText")
					.contains("empty \"findings\" array").contains("evidenceReferences");
			assertThat(d.contextProviders()).as(d.key()).contains("entity").hasSizeGreaterThan(1);
			assertThat(d.vocabulary()).extracting(VocabularyEntry::type).as(d.key())
					.contains("OTHER");
		}
		assertThat(covered).isEqualTo(AssistantDefinitionValidator.REVIEWABLE_TYPES);
		assertThat(bundled).hasSize(8);
	}

	@Test
	void theVocabularyIsRenderedIntoTheInstructions() {
		DefinitionExecutorAssistant goal = executor("ai-review-goal");

		String instructions = goal.instructions();

		assertThat(instructions).doesNotContain("{{vocabulary}}")
				.contains("- SOLUTION_NOT_OUTCOME: States a mechanism")
				.contains("Extraction types").contains("- EXTRACT_ACTOR: ");
		assertThat(instructions.indexOf("- OTHER:"))
				.isLessThan(instructions.indexOf("Extraction types"));
		assertThat(executor(RequirementsReview.ASSISTANT_ID).instructions())
				.isEqualTo(definition(RequirementsReview.ASSISTANT_ID).instructions());
	}

	@Test
	void anExtractionIsAdvisoryAndAnActorGetsAOneClickPosition() throws Exception {
		when(packBuilder.build(any(), any())).thenReturn(mock(EntityContextPack.class));
		reply = response(List.of(new AiFindingDraft("EXTRACT_ACTOR", "HIGH", 0.8,
				List.of("Dana"), "Dana is a role the project has no actor for.", null,
				List.of("Add a Treasurer actor"),
				Map.of(ReviewResultMapper.SUGGESTED_ENTITY_NAME, "Treasurer"))));

		AssistantResult result = executor("ai-review-goal").analyze(context(), target());

		AnnotationAction issue = only(result, AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE);
		assertThat(issue.severity()).isEqualTo("LOW");
		assertThat(issue.metadata()).containsEntry("mustResolve", false)
				.containsEntry("category", "extraction").containsEntry("kind", "LEXICAL")
				.containsEntry("word", "Treasurer");
		List<AnnotationAction> positions = result.annotationActions().stream()
				.filter(a -> a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION)
				.toList();
		assertThat(positions).extracting(AnnotationAction::text).containsExactly(
				"Add \"Treasurer\" to the project as an actor.", "Add a Treasurer actor");
		assertThat(positions.get(0).metadata()).containsEntry("kind", "ADD_ACTOR_TO_PROJECT");
		assertThat(positions.get(1).metadata()).doesNotContainKey("kind");
	}

	@Test
	void aQualityFindingStaysMustResolveAndKeepsItsSeverity() throws Exception {
		when(packBuilder.build(any(), any())).thenReturn(mock(EntityContextPack.class));
		reply = response(List.of(new AiFindingDraft("SOLUTION_NOT_OUTCOME", "HIGH", 0.8,
				List.of("text message"), "Names a mechanism.", null, List.of(), Map.of())));

		AnnotationAction issue = only(executor("ai-review-goal").analyze(context(), target()),
				AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE);

		assertThat(issue.severity()).isEqualTo("HIGH");
		assertThat(issue.metadata()).containsEntry("mustResolve", true)
				.containsEntry("category", "quality").doesNotContainKey("kind");
	}

	@Test
	void offVocabularyTypesAreCountedAndOtherDefinitionsListedForRetirement() throws Exception {
		when(packBuilder.build(any(), any())).thenReturn(mock(EntityContextPack.class));
		reply = response(List.of(
				new AiFindingDraft("UNTESTABLE", "LOW", 0.5, List.of(), "Not testable.", null,
						List.of(), Map.of()),
				new AiFindingDraft("AMBIGUOUS", "LOW", 0.5, List.of(), "Ambiguous.", null,
						List.of(), Map.of())));
		AssistantDefinitionStore store = mock(AssistantDefinitionStore.class);
		when(store.definitionsFor(2L, "REQUIREMENTS_REVIEW")).thenReturn(bundled);
		AiDefinitionExecutorFactory factory = factory();
		factory.setDefinitionStore(store);

		AssistantResult result = ((DefinitionExecutorAssistant) factory
				.executorFor(definition("ai-review-goal"))).analyze(context(), target());

		assertThat(result.metadata()).containsEntry(AssistantRunWorker.VOCABULARY_MISSES, 1);
		@SuppressWarnings("unchecked")
		List<String> retires = (List<String>) result.metadata()
				.get(AssistantRunWorker.RETIRES_ASSISTANTS);
		assertThat(retires).contains(RequirementsReview.ASSISTANT_ID, "ai-review-story")
				.doesNotContain("ai-review-goal").hasSize(7);
		assertThat(result.messages()).anyMatch(m -> m.text().contains("outside the definition"));
	}

	@Test
	void aReRunRemovesUntouchedIssuesItNoLongerReports() {
		assertThat(executor("ai-review-goal").cleanupPolicy())
				.isEqualTo(CleanupPolicy.AUTO_RESOLVE_IF_UNTOUCHED);
	}

	@Test
	void schemaVersion2AddsTheSuggestedEntityName() throws Exception {
		OutputSchemas schemas = new OutputSchemas(objectMapper);
		assertThat(schemas.schema("RequirementsReviewOutput", "2").toString())
				.contains("suggestedEntityName");
		assertThat(schemas.schema("RequirementsReviewOutput", "1").toString())
				.doesNotContain("suggestedEntityName");

		ReviewResultMapper mapper = new ReviewResultMapper(objectMapper);
		ReviewResultMapper.ReviewResult parsed = mapper.parse("""
				{"summary":"s","findings":[{"findingType":"EXTRACT_GLOSSARY_TERM","severity":null,
				"confidence":null,"evidenceReferences":[],"suggestedIssueText":"Define it",
				"suggestedNoteText":null,"suggestedPositions":[],"suggestedEntityName":" Loan "}]}
				""");
		AiAnalysisResponse response = mapper.toResponse(parsed, AiUsage.noop("t", Duration.ZERO),
				Map.of());
		assertThat(response.findings().get(0).metadata())
				.containsEntry(ReviewResultMapper.SUGGESTED_ENTITY_NAME, "Loan");
	}

	// ---- fixtures ------------------------------------------------------------------------

	private AssistantDefinition definition(String key) {
		return bundled.stream().filter(d -> d.key().equals(key)).findFirst().orElseThrow();
	}

	private AiDefinitionExecutorFactory factory() {
		AiProperties properties = new AiProperties();
		properties.setEnabled(true);
		return new AiDefinitionExecutorFactory(client, packBuilder, properties,
				mock(AssistantUsageRepository.class), objectMapper);
	}

	private DefinitionExecutorAssistant executor(String key) {
		return (DefinitionExecutorAssistant) factory().executorFor(definition(key));
	}

	private static AnnotationAction only(AssistantResult result, AnnotationAction.ActionType type) {
		List<AnnotationAction> actions = result.annotationActions().stream()
				.filter(a -> a.actionType() == type).toList();
		assertThat(actions).hasSize(1);
		return actions.get(0);
	}

	private static AiAnalysisResponse response(List<AiFindingDraft> findings) {
		return new AiAnalysisResponse("summary", NullNode.getInstance(), findings, List.of(),
				AiUsage.noop("noop", Duration.ZERO), Map.of());
	}

	private static AssistantContext context() {
		return new AssistantContext(UUID.randomUUID(), new UserRef(3L, "human"),
				new UserRef(4L, "assistant"), EntityRef.of("Project", 2L), "REQUIREMENTS_REVIEW",
				java.util.Locale.ROOT, Clock.systemUTC(), Map.of());
	}

	private static TextEntity target() {
		TextEntity target = mock(TextEntity.class);
		when(target.getId()).thenReturn(10L);
		when(target.getName()).thenReturn("Send overdue reminders by text message");
		when(target.getText()).thenReturn("Dana sends a text message to each member.");
		doReturn(TextEntity.class).when(target).getProjectOrDomainEntityInterface();
		return target;
	}
}
