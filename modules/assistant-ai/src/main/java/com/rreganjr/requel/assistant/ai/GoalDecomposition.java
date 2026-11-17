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

import java.util.List;
import java.util.Set;

import com.rreganjr.requel.assistant.core.definition.AssistantDefinition;
import com.rreganjr.requel.assistant.core.definition.DefinitionKind;
import com.rreganjr.requel.assistant.core.definition.DefinitionSource;

/**
 * The goal decomposition: the constants and the prompt of the assistant that proposes sub-goals
 * for one goal. Each proposed sub-goal is returned as a finding of type {@link #FINDING_TYPE} in
 * the {@code RequirementsReviewOutput} v2 schema, so every {@link AiAnalysisClient} can serve it
 * unchanged: {@code suggestedEntityName} is the sub-goal's name, {@code suggestedIssueText} its
 * text and {@code suggestedNoteText} the reason it is a separate outcome.
 *
 * <p>
 * The definition is built in code and is not bundled under {@code ai/definitions}: it has its
 * own task type and must never run as part of a goal review.
 */
public final class GoalDecomposition {

	/** The definition's key, and so its executor's assistant id. */
	public static final String ASSISTANT_ID = "ai-goal-decomposition";

	/** The task type a decomposition run must carry in its {@code AssistantContext}. */
	public static final String TASK_TYPE = "GOAL_DECOMPOSITION";

	/** The finding type of every proposed sub-goal. */
	public static final String FINDING_TYPE = "SUB_GOAL";

	/** Placeholder in {@link #INSTRUCTIONS} replaced by the configured maximum. */
	static final String MAX_PLACEHOLDER = "{{maxSubGoals}}";

	static final String INSTRUCTIONS = """
			You are a Requel requirements-analysis assistant decomposing one Goal.
			Analyze ONLY the target goal described in the context and propose sub-goals that, taken together, achieve it. Return JSON matching the supplied schema.

			A good sub-goal:
			- states an outcome the system under specification must achieve, not a mechanism, task or feature;
			- is one outcome that can be completed and checked on its own, so progress on it can be tracked;
			- is narrower than the target and serves it: if every sub-goal were met, the target would be met;
			- names roles, not people, and is worded the way the target and its stakeholders word things;
			- does not repeat the target, a sibling goal or a goal related to the target. The context lists them: do not propose what is already there.

			Rules:
			- Propose at most {{maxSubGoals}} sub-goals, fewer when fewer are needed. Propose as few as the target needs; a goal that is already a single small outcome needs none, so return an empty "findings" array. Never pad the list.
			- Return each sub-goal as ONE finding:
			  - "findingType" is always "SUB_GOAL";
			  - "suggestedEntityName" is a short name for the sub-goal, at most 80 characters, with no trailing period;
			  - "suggestedIssueText" is the sub-goal's text: one or two sentences naming the outcome, for whom, and how anyone will know it was achieved. Do not invent numbers, limits or thresholds the target does not give; leave them open instead;
			  - "suggestedNoteText" is one sentence on how this sub-goal contributes to the target, or null;
			  - "evidenceReferences" holds the exact words of the target (or of a context section) that the sub-goal comes from;
			  - "severity" is always "LOW"; "confidence" is how sure you are, from 0 to 1;
			  - "suggestedPositions" is always an empty array.
			- Do not split a goal along solution lines (screens, tables, services). Split along outcomes.
			- Do not propose sub-goals about team process or operations rather than the system under specification.
			- Do not repeat a sub-goal under a different name. Order them so the ones the others depend on come first.
			- The "summary" is one sentence on how the target was split, or why it was not.
			- The "context" sections describe related entities so you can avoid duplicating them. Propose sub-goals of the target only.

			Example of one well-formed finding for the goal "Members borrow and return tools without staff help":
			{"findingType": "SUB_GOAL", "severity": "LOW", "confidence": 0.8, "evidenceReferences": ["borrow and return tools"], "suggestedIssueText": "A member can check a tool out on their own and the tool shows as unavailable to other members until it is returned.", "suggestedNoteText": "Covers the borrowing half of the target.", "suggestedPositions": [], "suggestedEntityName": "Self-service checkout"}
			""";

	private GoalDecomposition() {
	}

	/**
	 * The definition that runs one decomposition.
	 *
	 * @param maxSubGoals the most sub-goals the model is asked for
	 */
	static AssistantDefinition definition(int maxSubGoals) {
		return new AssistantDefinition(ASSISTANT_ID, "AI goal decomposition", DefinitionKind.REVIEW,
				TASK_TYPE, Set.of("Goal"),
				List.of("entity", "goal-relations", "goal-siblings", "goal-stakeholders",
						"project-names"),
				INSTRUCTIONS.replace(MAX_PLACEHOLDER, String.valueOf(maxSubGoals)), List.of(),
				RequirementsReview.OUTPUT_SCHEMA_NAME, "2", true, 1, DefinitionSource.BUNDLED, null,
				null, null);
	}
}
