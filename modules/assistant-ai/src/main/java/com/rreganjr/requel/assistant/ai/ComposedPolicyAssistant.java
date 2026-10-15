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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantException;
import com.rreganjr.requel.assistant.api.AssistantMessage;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.CleanupPolicy;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.assistant.core.ComposedAssistant;
import com.rreganjr.requel.assistant.core.StagedAssistant;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinition;
import com.rreganjr.requel.assistant.core.definition.DefinitionKind;
import com.rreganjr.requel.assistant.core.definition.DefinitionSource;
import com.rreganjr.requel.assistant.core.definition.VocabularyEntry;
import com.rreganjr.requel.project.TextEntity;

/**
 * Issue #265: every policy that applies to an entity, run in one provider call. Each policy keeps
 * its own rule text and finding types; the model names the policy behind every finding
 * ({@code policyKey}, restricted to the composed keys), and the findings come back as one result
 * per policy, so identity, source, idempotency and cleanup are the policy's own.
 *
 * <ul>
 * <li>A {@code localOnly} policy is left out when the active provider is remote.</li>
 * <li>More policies than {@code requel.ai.policies.max-composed} fail the run: rules are never
 * dropped to fit.</li>
 * <li>A finding naming a policy that was not composed is dropped and counted as a vocabulary
 * miss; one using a type outside its policy's vocabulary is kept and counted, as for reviews.</li>
 * </ul>
 */
public class ComposedPolicyAssistant
		implements RequelAssistant<TextEntity>, ComposedAssistant, StagedAssistant {

	private static final Logger log = LoggerFactory.getLogger(ComposedPolicyAssistant.class);

	/** The composed pass's own id: a run note when no policy could run, and the request's. */
	public static final String ASSISTANT_ID = "ai-policies";

	private final List<DefinitionExecutorAssistant> members;
	private final AiDefinitionExecutorFactory runtime;

	ComposedPolicyAssistant(List<AssistantDefinition> policies, AiDefinitionExecutorFactory runtime) {
		this.runtime = runtime;
		List<DefinitionExecutorAssistant> built = new ArrayList<>();
		for (AssistantDefinition policy : policies) {
			built.add(new DefinitionExecutorAssistant(policy, runtime));
		}
		this.members = List.copyOf(built);
	}

	@Override
	public String assistantId() {
		return ASSISTANT_ID;
	}

	@Override
	public String displayName() {
		return "AI policies";
	}

	@Override
	public Class<TextEntity> targetType() {
		return TextEntity.class;
	}

	@Override
	public boolean handlesTask(String taskType) {
		return AssistantDefinition.POLICY_REVIEW.equals(taskType);
	}

	@Override
	public CleanupPolicy cleanupPolicy() {
		return CleanupPolicy.AUTO_RESOLVE_IF_UNTOUCHED;
	}

	@Override
	public List<RequelAssistant<?>> members() {
		return List.copyOf(members);
	}

	/** The worker calls {@link #analyzeAll}; one result can't carry several policies. */
	@Override
	public AssistantResult analyze(AssistantContext context, TextEntity target)
			throws AssistantException {
		throw new AssistantException("a composed policy pass reports one result per policy;"
				+ " run it through analyzeAll");
	}

	@Override
	public List<AssistantResult> analyzeAll(AssistantContext context, Object target)
			throws AssistantException {
		return prepare(context, target).complete();
	}

	/**
	 * #363: choose the policies, check the ceiling and build the one request inside the analyze
	 * transaction; the stage makes the call and attributes the findings.
	 */
	@Override
	public Stage prepare(AssistantContext context, Object target) throws AssistantException {
		TextEntity entity = (TextEntity) target;
		List<String> notes = new ArrayList<>();
		List<DefinitionExecutorAssistant> running = new ArrayList<>();
		boolean remote = runtime.locality() == AiProviderLocality.REMOTE;
		for (DefinitionExecutorAssistant member : members) {
			if (remote && member.definition().localOnly()) {
				notes.add(member.definition().key() + " is local-only and the provider is remote;"
						+ " it did not run.");
			} else {
				running.add(member);
			}
		}
		int ceiling = runtime.aiProperties.getPolicies().getMaxComposed();
		if (running.size() > ceiling) {
			throw new AssistantException(running.size() + " policies apply to "
					+ entity.getProjectOrDomainEntityInterface().getSimpleName() + " "
					+ entity.getId() + "; the limit is " + ceiling
					+ " (requel.ai.policies.max-composed). No policy ran.");
		}
		if (running.isEmpty()) {
			AssistantResult nothing = AssistantResult.builder().assistantId(ASSISTANT_ID)
					.runId(context.runId()).summary(String.join(" ", notes)).build();
			return () -> List.of(nothing);
		}

		DefinitionExecutorAssistant.Prepared prepared = composedExecutor(running)
				.prepareCall(context, entity);
		Map<String, List<String>> retires = new LinkedHashMap<>();
		for (DefinitionExecutorAssistant member : running) {
			retires.put(member.definition().key(), member.retiredBy(context));
		}
		return () -> results(context, notes, running, prepared.call(), retires);
	}

	/** One result per policy that ran, from the pass's single call. */
	private List<AssistantResult> results(AssistantContext context, List<String> notes,
			List<DefinitionExecutorAssistant> running, DefinitionExecutorAssistant.Call call,
			Map<String, List<String>> retires) {
		if (call.skipped != null) {
			return List.of(call.skipped);
		}

		Map<String, DefinitionExecutorAssistant> byKey = new LinkedHashMap<>();
		Map<String, AssistantResult.Builder> builders = new LinkedHashMap<>();
		Map<String, int[]> counts = new LinkedHashMap<>();
		for (DefinitionExecutorAssistant member : running) {
			String key = member.definition().key();
			byKey.put(key, member);
			builders.put(key, AssistantResult.builder().assistantId(key).runId(context.runId()));
			counts.put(key, new int[2]);
		}
		int unattributed = 0;
		for (AiFindingDraft finding : call.findings()) {
			Object policyKey = finding.metadata() == null ? null
					: finding.metadata().get(ReviewResultMapper.POLICY_KEY);
			DefinitionExecutorAssistant member = policyKey == null ? null : byKey.get(policyKey.toString());
			if (member == null) {
				unattributed++;
				log.debug("dropping a policy finding of type {} for unknown policy {} in run {}",
						finding.findingType(), policyKey, context.runId());
				continue;
			}
			int[] memberCounts = counts.get(member.definition().key());
			if (call.evidenceUnverified(finding)) {
				memberCounts[0]++;
			}
			if (member.vocabularyEntry(finding.findingType()) == null) {
				memberCounts[1]++;
			}
			member.mapFinding(builders.get(member.definition().key()), call.targetRef, finding);
		}

		List<AssistantResult> results = new ArrayList<>();
		boolean first = true;
		for (Map.Entry<String, AssistantResult.Builder> entry : builders.entrySet()) {
			AssistantResult.Builder builder = entry.getValue();
			int[] memberCounts = counts.get(entry.getKey());
			int misses = memberCounts[1];
			if (first) {
				// The call's summary, messages and notes belong to the pass; record them once.
				List<String> summary = new ArrayList<>();
				if (call.response.summary() != null && !call.response.summary().isBlank()) {
					summary.add(call.response.summary().strip());
				}
				summary.addAll(notes);
				builder.summary(String.join(" ", summary));
				if (call.response.messages() != null) {
					call.response.messages().forEach(builder::message);
				}
				if (unattributed > 0) {
					misses += unattributed;
					builder.message(AssistantMessage.warning(unattributed
							+ " finding(s) named no policy in this pass and were dropped"));
				}
				first = false;
			}
			byKey.get(entry.getKey()).finish(builder, retires.get(entry.getKey()), memberCounts[0],
					misses);
			results.add(builder.build());
		}
		return results;
	}

	/** The executor that makes the one call: the policies' rules and context, together. */
	private DefinitionExecutorAssistant composedExecutor(List<DefinitionExecutorAssistant> running) {
		Set<String> providers = new LinkedHashSet<>();
		Map<String, Integer> budgets = new LinkedHashMap<>();
		List<VocabularyEntry> vocabulary = new ArrayList<>();
		Set<String> types = new LinkedHashSet<>();
		List<String> keys = new ArrayList<>();
		for (DefinitionExecutorAssistant member : running) {
			AssistantDefinition policy = member.definition();
			keys.add(policy.key());
			providers.addAll(policy.contextProviders());
			policy.contextBudgets().forEach(budgets::putIfAbsent);
			for (VocabularyEntry entry : policy.vocabulary()) {
				if (types.add(entry.type())) {
					vocabulary.add(entry);
				}
			}
		}
		AssistantDefinition composed = new AssistantDefinition(ASSISTANT_ID, "AI policies",
				DefinitionKind.POLICY, AssistantDefinition.POLICY_REVIEW, Set.of(),
				List.copyOf(providers), instructions(running), vocabulary,
				AssistantDefinition.POLICY_OUTPUT_SCHEMA, "1", true, 1, DefinitionSource.BUNDLED,
				null, null, null, budgets);
		return new DefinitionExecutorAssistant(composed, runtime) {
			@Override
			JsonNode outputSchema() {
				return withPolicyKeys(super.outputSchema(), keys);
			}

			@Override
			Map<String, Object> attributes(AssistantContext context) {
				Map<String, Object> attributes = super.attributes(context);
				attributes.put("policies", List.copyOf(keys));
				return attributes;
			}
		};
	}

	/** The output schema with {@code policyKey} restricted to the composed keys. */
	static JsonNode withPolicyKeys(JsonNode schema, List<String> keys) {
		JsonNode copy = schema.deepCopy();
		JsonNode policyKey = copy.path("properties").path("findings").path("items")
				.path("properties").path("policyKey");
		if (policyKey instanceof ObjectNode node) {
			ArrayNode values = node.putArray("enum");
			keys.forEach(values::add);
		}
		return copy;
	}

	/** One preamble, then each policy's rule with its finding types. */
	static String instructions(List<DefinitionExecutorAssistant> running) {
		StringBuilder text = new StringBuilder();
		text.append("You are a Requel requirements-analysis assistant applying the project's")
				.append(" policies to one entity in a POLICY_REVIEW.\n")
				.append("Analyze ONLY the target described in the context and return JSON matching")
				.append(" the supplied schema.\n\n")
				.append("Each policy below is a separate rule. Check the target against every one.")
				.append(" Set \"policyKey\" on every finding to the key of the policy that raised")
				.append(" it, and use one of that policy's finding types.\n");
		for (DefinitionExecutorAssistant member : running) {
			AssistantDefinition policy = member.definition();
			text.append("\n## Policy ").append(policy.key()).append(" (")
					.append(policy.displayName()).append(")\n\n")
					.append(member.instructions().strip()).append('\n');
		}
		text.append("\nRules:\n")
				.append("- If no policy applies, return an empty \"findings\" array. Never invent")
				.append(" issues to fill space; a clean entity is a good result.\n")
				.append("- Report each problem once, under the policy and type that fit it best.\n")
				.append("- For EVERY finding set \"suggestedIssueText\" to a clear, specific problem")
				.append(" statement about the target, and add \"suggestedPositions\" with concrete")
				.append(" fixes where you can.\n")
				.append("- In \"evidenceReferences\", copy the exact words that show the problem,")
				.append(" from the target or from a context section.\n")
				.append("- The context may include an \"annotations\" list of EXISTING issues and")
				.append(" notes; do NOT restate them.\n")
				.append("- Set \"suggestedEntityName\" to null.\n");
		return text.toString();
	}
}
