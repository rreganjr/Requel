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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantException;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.TextEntity;

/**
 * Asks the configured AI provider to decompose one goal into sub-goals, and returns them as
 * proposals; nothing is created. It reuses {@link DefinitionExecutorAssistant} for the context
 * pack, the project's data-handling flags, the skip gating, the provider call and usage
 * recording, with the definition from {@link GoalDecomposition}.
 *
 * <p>
 * Like {@code StagedAssistant}, it is in two steps so the provider call is made with no
 * transaction open: {@link #prepare} reads the goal and its context (call it inside the
 * transaction), and {@link Pending#complete()} makes the call and maps the reply from detached
 * data.
 *
 * <p>
 * The bean exists only when {@code requel.ai.enabled=true}, like {@link AiDefinitionExecutorFactory}.
 */
@Component
@ConditionalOnProperty(name = "requel.ai.enabled", havingValue = "true")
public class GoalDecompositionService {

	private static final Logger log = LoggerFactory.getLogger(GoalDecompositionService.class);

	private static final int MAX_NAME_LENGTH = 255;

	/** One proposed sub-goal. {@code rationale} and {@code evidence} may be empty. */
	public record ProposedSubGoal(String name, String text, String rationale,
			List<String> evidence) {

		public ProposedSubGoal {
			evidence = evidence == null ? List.of() : List.copyOf(evidence);
		}
	}

	/**
	 * The outcome of one decomposition. When {@code skipped}, no call was made and
	 * {@code summary} says why.
	 */
	public record Decomposition(String summary, List<ProposedSubGoal> subGoals, boolean skipped) {

		public Decomposition {
			subGoals = subGoals == null ? List.of() : List.copyOf(subGoals);
		}
	}

	/** The provider call, ready to make. Holds no entity. */
	@FunctionalInterface
	public interface Pending {
		Decomposition complete() throws AssistantException;
	}

	private final AiDefinitionExecutorFactory runtime;

	@Autowired
	public GoalDecompositionService(AiDefinitionExecutorFactory runtime) {
		this.runtime = runtime;
	}

	/**
	 * Decompose {@code goal} in one go. Convenience for callers that don't need the two steps.
	 *
	 * @param context a run context whose {@code taskType()} is {@link GoalDecomposition#TASK_TYPE}
	 * @throws AssistantException if the target is not a goal or the provider call fails
	 */
	public Decomposition decompose(AssistantContext context, TextEntity goal)
			throws AssistantException {
		return prepare(context, goal).complete();
	}

	/**
	 * Build the context and the request for {@code goal}. Call inside the transaction that can
	 * read the goal; the returned {@link Pending} touches no entity.
	 *
	 * @param context a run context whose {@code taskType()} is {@link GoalDecomposition#TASK_TYPE}
	 * @throws AssistantException if the target is not a goal
	 */
	public Pending prepare(AssistantContext context, TextEntity goal) throws AssistantException {
		if (!(goal instanceof Goal)) {
			throw new AssistantException("Only a goal can be decomposed");
		}
		int max = Math.max(1, runtime.aiProperties.getDecomposition().getMaxSubGoals());
		DefinitionExecutorAssistant executor = new DefinitionExecutorAssistant(
				GoalDecomposition.definition(max), runtime);

		String skipReason = executor.skipReason(context);
		if (skipReason != null) {
			Decomposition skipped = skipped(skipReason);
			return () -> skipped;
		}
		DefinitionExecutorAssistant.Prepared prepared = executor.prepareCall(context, goal);
		if (prepared.skipped != null) {
			Decomposition skipped = skipped(
					"Context exceeds the configured AI input cap; decomposition skipped.");
			return () -> skipped;
		}
		String parentName = goal.getName();
		return () -> map(max, parentName, prepared.call());
	}

	private static Decomposition skipped(String reason) {
		return new Decomposition(reason, List.of(), true);
	}

	/**
	 * Each finding becomes a proposal. A finding without a name or text is dropped, as is one
	 * that repeats an earlier proposal or the parent's own name; the list is capped at
	 * {@code max}. The model's output stays untrusted input: names and texts are bounded.
	 */
	private static Decomposition map(int max, String parentName,
			DefinitionExecutorAssistant.Call call) {
		Set<String> seen = new HashSet<String>();
		if (parentName != null) {
			seen.add(parentName.strip().toLowerCase(Locale.ROOT));
		}
		List<ProposedSubGoal> proposals = new ArrayList<ProposedSubGoal>();
		int dropped = 0;
		for (AiFindingDraft finding : call.findings()) {
			Object named = finding.metadata() == null ? null
					: finding.metadata().get(ReviewResultMapper.SUGGESTED_ENTITY_NAME);
			String name = named == null ? null : boundedName(named.toString());
			String text = DefinitionExecutorAssistant.boundedText(finding.suggestedIssueText());
			if (name == null || text == null || !seen.add(name.toLowerCase(Locale.ROOT))
					|| proposals.size() >= max) {
				dropped++;
				continue;
			}
			proposals.add(new ProposedSubGoal(name, text,
					DefinitionExecutorAssistant.boundedText(finding.suggestedNoteText()),
					evidence(finding.evidenceReferences())));
		}
		if (dropped > 0) {
			log.debug("Dropped {} decomposition finding(s): no name or text, a repeat, or over {}",
					dropped, max);
		}
		return new Decomposition(call.response.summary(), proposals, false);
	}

	private static List<String> evidence(List<String> references) {
		List<String> evidence = new ArrayList<String>();
		if (references != null) {
			for (String reference : references) {
				if (reference != null && !reference.isBlank()) {
					evidence.add(reference.strip());
				}
			}
		}
		return evidence;
	}

	/** Single-line, at most {@value #MAX_NAME_LENGTH} characters; null when blank. */
	private static String boundedName(String name) {
		String oneLine = name.replaceAll("\\s+", " ").strip();
		if (oneLine.isEmpty()) {
			return null;
		}
		return oneLine.length() <= MAX_NAME_LENGTH ? oneLine : oneLine.substring(0, MAX_NAME_LENGTH);
	}
}
