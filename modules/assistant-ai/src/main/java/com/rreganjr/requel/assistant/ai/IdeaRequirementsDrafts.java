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
import java.util.Locale;

import com.rreganjr.requel.assistant.api.AssistantMessage;

/**
 * The validated result of one idea-to-requirements call: the proposed entities as drafts, what
 * was dropped or adjusted on the way ({@link #problems()}), and the model's open questions
 * ({@link #messages()}, passed through as returned). Nothing here is persisted; a later step
 * turns the drafts into the project's own draft types.
 *
 * <p>
 * Built by {@link IdeaRequirementsParser}. When {@link #skipped()}, no call was made and
 * {@code skippedReason} says why.
 *
 * @param summary the model's one-sentence summary of the idea
 * @param drafts the proposals that passed validation, in the model's order
 * @param messages the model's warnings (open questions and assumptions about the idea)
 * @param problems every proposal dropped and every adjustment made during validation
 * @param skippedReason why no call was made, or null
 */
public record IdeaRequirementsDrafts(String summary, List<Draft> drafts,
		List<AssistantMessage> messages, List<Problem> problems, String skippedReason) {

	public IdeaRequirementsDrafts {
		summary = summary == null ? "" : summary;
		drafts = drafts == null ? List.of() : List.copyOf(drafts);
		messages = messages == null ? List.of() : List.copyOf(messages);
		problems = problems == null ? List.of() : List.copyOf(problems);
	}

	/** The result of a call that was not made. */
	static IdeaRequirementsDrafts skipped(String reason) {
		return new IdeaRequirementsDrafts(reason, List.of(), List.of(), List.of(), reason);
	}

	public boolean skipped() {
		return skippedReason != null;
	}

	/** The drafts of one kind, in order. */
	public List<Draft> ofKind(Kind kind) {
		return drafts.stream().filter(draft -> draft.kind() == kind).toList();
	}

	/** The kinds of entity an idea can be turned into; the names are the finding types. */
	public enum Kind {
		GOAL, ACTOR, STORY, USE_CASE, GLOSSARY_TERM;

		/** The kind named by {@code text} (case, spaces and dashes are forgiven), or null. */
		static Kind parse(String text) {
			if (text == null) {
				return null;
			}
			String normalized = text.strip().toUpperCase(Locale.ROOT).replace(' ', '_')
					.replace('-', '_');
			try {
				return valueOf(normalized);
			} catch (IllegalArgumentException e) {
				return null;
			}
		}

		/** For messages: {@code use case}. */
		public String label() {
			return name().toLowerCase(Locale.ROOT).replace('_', ' ');
		}
	}

	/**
	 * A link from a draft to another entity: either another draft of this result
	 * ({@code existing} false) or an entity already in the project.
	 *
	 * @param kind the kind of the entity referred to
	 * @param name its name; for a draft, exactly its {@link Draft#name()}
	 */
	public record Reference(Kind kind, String name, boolean existing) {
	}

	/**
	 * One proposed entity.
	 *
	 * @param kind what it proposes
	 * @param name the proposed name, single-line and bounded
	 * @param text the proposed description, or a glossary term's definition; never blank
	 * @param note what in the idea led to it, or null
	 * @param assumption true when the model says the idea did not state it
	 * @param evidence the idea's own words it cites
	 * @param evidenceUnverified true when some evidence is not in the idea text
	 * @param confidence the model's confidence in 0..1, or null
	 * @param references the entities it refers to. A story or use case has its primary actor
	 *        first, then the goals it serves; a goal refers only to the broader goal it is part
	 *        of; an actor or glossary term refers to none.
	 */
	public record Draft(Kind kind, String name, String text, String note, boolean assumption,
			List<String> evidence, boolean evidenceUnverified, Double confidence,
			List<Reference> references) {

		public Draft {
			evidence = evidence == null ? List.of() : List.copyOf(evidence);
			references = references == null ? List.of() : List.copyOf(references);
		}
	}

	/**
	 * Something validation did. A {@code dropped} problem removed a whole proposal; otherwise
	 * the proposal was kept and only the described part was left out or flagged.
	 *
	 * @param subject the proposal's name, or the finding type when it had no usable name
	 */
	public record Problem(String subject, boolean dropped, String message) {
	}
}
