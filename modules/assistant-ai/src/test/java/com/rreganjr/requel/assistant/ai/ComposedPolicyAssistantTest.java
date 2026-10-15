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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantException;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.assistant.core.AssistantRunWorker;
import com.rreganjr.requel.assistant.core.StagedAssistant;
import com.rreganjr.requel.assistant.core.context.EntityContextPack;
import com.rreganjr.requel.assistant.core.context.EntityContextPackBuilder;
import com.rreganjr.requel.assistant.core.context.PackSpec;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinition;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinitionValidator;
import com.rreganjr.requel.assistant.core.definition.BundledDefinitions;
import com.rreganjr.requel.assistant.core.definition.DefinitionKind;
import com.rreganjr.requel.assistant.core.definition.DefinitionSource;
import com.rreganjr.requel.assistant.core.definition.VocabularyEntry;
import com.rreganjr.requel.assistant.core.persistence.AssistantUsageRepository;
import com.rreganjr.requel.project.ProjectAssistantSettingsStore;
import com.rreganjr.requel.project.TextEntity;

/**
 * #265: the composed policy pass - one provider call for every applicable policy, findings
 * attributed back per policy, the ceiling, and local-only policies on a remote provider.
 */
class ComposedPolicyAssistantTest {

	private final EntityContextPackBuilder packBuilder = mock(EntityContextPackBuilder.class);
	private final RecordingAiClient aiClient = new RecordingAiClient();
	private final ObjectMapper objectMapper = new ObjectMapper();
	private AiProperties properties;
	private AiDefinitionExecutorFactory factory;

	@BeforeEach
	void setUp() {
		when(packBuilder.build(any(), any(PackSpec.class))).thenReturn(mock(EntityContextPack.class));
		properties = new AiProperties();
		properties.setEnabled(true);
		factory = new AiDefinitionExecutorFactory(aiClient, packBuilder, properties,
				mock(AssistantUsageRepository.class), objectMapper);
	}

	@Test
	void threePoliciesMakeOneProviderCallWithTheirRulesAndKeys() throws Exception {
		ComposedPolicyAssistant composed = composed(policy("p-a", "Rule A.", false),
				policy("p-b", "Rule B.", false), policy("p-c", "Rule C.", false));

		List<AssistantResult> results = composed.analyzeAll(context(), target());

		assertThat(aiClient.calls).isEqualTo(1);
		AiAnalysisRequest request = aiClient.lastRequest;
		assertThat(request.taskType()).isEqualTo(AssistantDefinition.POLICY_REVIEW);
		assertThat(request.instructions()).contains("## Policy p-a", "Rule A.", "## Policy p-b",
				"Rule B.", "## Policy p-c", "Rule C.", "\"policyKey\"");
		assertThat(request.outputSchema().at("/properties/findings/items/properties/policyKey/enum"))
				.extracting(n -> n.asText()).containsExactly("p-a", "p-b", "p-c");
		assertThat(request.attributes()).containsEntry("policies", List.of("p-a", "p-b", "p-c"));
		assertThat(results).extracting(AssistantResult::assistantId).containsExactly("p-a", "p-b",
				"p-c");
		assertThat(composed.members()).extracting(m -> m.assistantId()).containsExactly("p-a",
				"p-b", "p-c");
	}

	@Test
	void findingsAreAttributedToTheirPolicyAndAnUnknownKeyIsDroppedAndCounted() throws Exception {
		aiClient.response = new AiAnalysisResponse("two policies fired", NullNode.getInstance(),
				List.of(finding("p-a", "RULE_BROKEN", "A is broken"),
						finding("p-b", "RULE_BROKEN", "B is broken"),
						finding("p-b", "NOT_A_TYPE", "B off vocabulary"),
						finding("p-zzz", "RULE_BROKEN", "nobody's")),
				List.of(), AiUsage.noop("noop", Duration.ZERO), Map.of());

		List<AssistantResult> results = composed(policy("p-a", "Rule A.", false),
				policy("p-b", "Rule B.", false)).analyzeAll(context(), target());

		AssistantResult a = results.get(0);
		AssistantResult b = results.get(1);
		assertThat(issues(a)).extracting(AnnotationAction::text).containsExactly("A is broken");
		assertThat(issues(a).get(0).metadata()).containsEntry("definitionKey", "p-a");
		assertThat(issues(b)).extracting(AnnotationAction::text)
				.containsExactly("B is broken", "B off vocabulary");
		assertThat(a.summary()).isEqualTo("two policies fired");
		// p-a carries the dropped one; p-b its own off-vocabulary finding.
		assertThat(a.metadata()).containsEntry(AssistantRunWorker.VOCABULARY_MISSES, 1);
		assertThat(b.metadata()).containsEntry(AssistantRunWorker.VOCABULARY_MISSES, 1);
		// Policies never retire one another's findings.
		assertThat(a.metadata()).containsEntry(AssistantRunWorker.RETIRES_ASSISTANTS, List.of());
	}

	/**
	 * #363: the policies are chosen and the request built in prepare; the stage makes the one call
	 * and attributes the findings without touching the target or the pack builder.
	 */
	@Test
	void theStageTouchesNoEntity() throws Exception {
		aiClient.response = new AiAnalysisResponse("one policy fired", NullNode.getInstance(),
				List.of(finding("p-b", "RULE_BROKEN", "B is broken")), List.of(),
				AiUsage.noop("noop", Duration.ZERO), Map.of());
		ProjectAssistantSettingsStore settings = mock(ProjectAssistantSettingsStore.class);
		factory.setSettingsStore(settings);
		TextEntity target = target();

		StagedAssistant.Stage stage = composed(policy("p-a", "Rule A.", false),
				policy("p-b", "Rule B.", false)).prepare(context(), target);

		assertThat(aiClient.calls).isZero();
		clearInvocations(target, packBuilder, settings);

		List<AssistantResult> results = stage.complete();

		assertThat(aiClient.calls).isEqualTo(1);
		verifyNoInteractions(target, packBuilder, settings);
		assertThat(results).extracting(AssistantResult::assistantId).containsExactly("p-a", "p-b");
		assertThat(issues(results.get(1))).extracting(AnnotationAction::text)
				.containsExactly("B is broken");
	}

	@Test
	void overTheCeilingTheRunFailsAndNoPolicyRuns() {
		properties.getPolicies().setMaxComposed(1);
		ComposedPolicyAssistant composed = composed(policy("p-a", "A", false),
				policy("p-b", "B", false));

		assertThatThrownBy(() -> composed.analyzeAll(context(), target()))
				.isInstanceOf(AssistantException.class)
				.hasMessageContaining("2 policies apply to TextEntity 10; the limit is 1")
				.hasMessageContaining("requel.ai.policies.max-composed")
				.hasMessageContaining("No policy ran.");
		assertThat(aiClient.calls).isZero();
	}

	@Test
	void aLocalOnlyPolicyIsLeftOutOnARemoteProvider() throws Exception {
		factory.setProviderLocality(AiProviderLocality.REMOTE);

		List<AssistantResult> mixed = composed(policy("p-pii", "Find PII.", true),
				policy("p-a", "A", false)).analyzeAll(context(), target());
		assertThat(aiClient.calls).isEqualTo(1);
		assertThat(aiClient.lastRequest.instructions()).doesNotContain("Find PII.");
		assertThat(mixed).extracting(AssistantResult::assistantId).containsExactly("p-a");
		assertThat(mixed.get(0).summary()).contains("p-pii is local-only");

		List<AssistantResult> none = composed(policy("p-pii", "Find PII.", true))
				.analyzeAll(context(), target());
		assertThat(aiClient.calls).isEqualTo(1);
		assertThat(none).extracting(AssistantResult::assistantId)
				.containsExactly(ComposedPolicyAssistant.ASSISTANT_ID);
		assertThat(none.get(0).annotationActions()).isEmpty();

		factory.setProviderLocality(AiProviderLocality.LOCAL);
		composed(policy("p-pii", "Find PII.", true)).analyzeAll(context(), target());
		assertThat(aiClient.calls).isEqualTo(2);
		assertThat(aiClient.lastRequest.instructions()).contains("Find PII.");
	}

	@Test
	void theBundledTerminologyPolicyLoadsAndValidates() {
		AssistantDefinition terminology = BundledDefinitions.load(objectMapper,
				new AssistantDefinitionValidator(16000)).stream()
				.filter(d -> d.key().equals("ai-policy-terminology")).findFirst().orElseThrow();

		assertThat(terminology.kind()).isEqualTo(DefinitionKind.POLICY);
		assertThat(terminology.scope()).isEmpty();
		assertThat(terminology.appliesTo("Goal")).isTrue();
		assertThat(terminology.contextProviders()).containsExactly("entity", "project-glossary");
		assertThat(terminology.vocabulary()).extracting(VocabularyEntry::type)
				.containsExactly("NON_CANONICAL_TERM", "CONFLICTING_USAGE", "OTHER");
		assertThat(terminology.localOnly()).isFalse();
	}

	private ComposedPolicyAssistant composed(AssistantDefinition... policies) {
		return (ComposedPolicyAssistant) factory.composedExecutorFor(List.of(policies));
	}

	private static AssistantDefinition policy(String key, String rule, boolean localOnly) {
		return new AssistantDefinition(key, "Policy " + key, DefinitionKind.POLICY,
				AssistantDefinition.POLICY_REVIEW, Set.of(), List.of("entity"),
				rule + "\n\n{{vocabulary}}", List.of(new VocabularyEntry("RULE_BROKEN", "broken")),
				AssistantDefinition.POLICY_OUTPUT_SCHEMA, "1", true, 1, DefinitionSource.BUNDLED,
				null, null, null, Map.of(), localOnly);
	}

	private static AiFindingDraft finding(String policyKey, String type, String text) {
		return new AiFindingDraft(type, "MEDIUM", 0.8, List.of(), text, null, List.of(),
				Map.of(ReviewResultMapper.POLICY_KEY, policyKey));
	}

	private static List<AnnotationAction> issues(AssistantResult result) {
		return result.annotationActions().stream()
				.filter(a -> a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE).toList();
	}

	private static AssistantContext context() {
		return new AssistantContext(UUID.randomUUID(), new UserRef(3L, "human"),
				new UserRef(4L, "assistant"), EntityRef.of("Project", 2L),
				AssistantDefinition.POLICY_REVIEW, java.util.Locale.ROOT, Clock.systemUTC(),
				Map.of());
	}

	private static TextEntity target() {
		TextEntity target = mock(TextEntity.class);
		when(target.getId()).thenReturn(10L);
		when(target.getName()).thenReturn("Renew as a patron");
		when(target.getText()).thenReturn("As a patron, I want to renew a loan.");
		doReturn(TextEntity.class).when(target).getProjectOrDomainEntityInterface();
		return target;
	}

	/** Records invocations and returns a configurable response (no real provider). */
	private static final class RecordingAiClient implements AiAnalysisClient {
		private int calls;
		private AiAnalysisRequest lastRequest;
		private AiAnalysisResponse response = new AiAnalysisResponse("noop summary",
				NullNode.getInstance(), List.of(), List.of(), AiUsage.noop("noop", Duration.ZERO),
				Map.of());

		@Override
		public AiAnalysisResponse analyze(AiAnalysisRequest request) throws AiAnalysisException {
			this.calls++;
			this.lastRequest = request;
			return response;
		}
	}
}
