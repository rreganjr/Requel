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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.hibernate.Hibernate;

import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.impl.LexicalIssue;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.core.CommandBackedAssistantResultApplicator;
import com.rreganjr.requel.project.TextEntity;

/**
 * Issue #268: clearing the issues the old {@code LexicalAssistant} left on an entity. That path
 * wrote {@link LexicalIssue}s with no {@code ASSISTANT:} source and no finding row, so the SPI's
 * auto-resolve never sees them, and an imported project keeps its old noise forever.
 *
 * <p>
 * When a lexical assistant analyzes an entity, each unresolved, unowned lexical issue of that
 * assistant's kind on a property it finished analyzing, whose word and property the run did not
 * report again, is removed from the entity ({@code REMOVE_ANNOTATION_FROM_ANNOTATABLE} with
 * {@code metadata.legacyAnnotationId}). One the run did report again is left: the lexical issue
 * command reuses it by word and property, so it becomes the assistant's issue with its positions
 * and discussion intact. Resolved issues are never touched. The removals go before the result's
 * other actions, so an issue with no word (a complex sentence) is gone before the run raises its
 * own.
 */
final class LegacyLexicalIssues {

	static final String LEGACY_ANNOTATION_ID =
			CommandBackedAssistantResultApplicator.LEGACY_ANNOTATION_ID;

	private LegacyLexicalIssues() {
	}

	/** Text the old path wrote for each kind of issue. */
	static final Predicate<LexicalIssue> SPELLING = issue -> hasProperty(issue)
			&& contains(issue, "is not recognized");
	static final Predicate<LexicalIssue> VAGUE_WORD = issue -> hasProperty(issue)
			&& contains(issue, "is vague");
	static final Predicate<LexicalIssue> COMPLEXITY = issue -> hasProperty(issue)
			&& contains(issue, "is complex");
	static final Predicate<LexicalIssue> GLOSSARY_TERM = issue -> !hasProperty(issue)
			&& contains(issue, "potential glossary term");

	/**
	 * @return {@code result} with a removal for each of the old path's issues of this kind on
	 *         {@code target} that the run did not report again, placed before its other actions;
	 *         {@code result} itself when there are none. A property-less kind (glossary) is
	 *         considered only when both properties finished.
	 */
	static AssistantResult withRemovals(AssistantResult result, TextEntity target,
			EntityRef targetRef, Predicate<LexicalIssue> kind, PropertyChecks checks) {
		Set<Annotation> annotations = target.getAnnotations();
		if (annotations == null || annotations.isEmpty()) {
			return result;
		}
		Set<String> reported = reportedKeys(result);
		List<AnnotationAction> removals = new ArrayList<>();
		for (Annotation annotation : annotations) {
			Object unproxied = Hibernate.unproxy(annotation);
			if (!(unproxied instanceof LexicalIssue issue) || issue.getId() == null
					|| issue.isResolved() || isAssistantSourced(issue) || !kind.test(issue)) {
				continue;
			}
			String property = issue.getAnnotatableEntityPropertyName();
			boolean finished = property == null ? checks.complete() : checks.completed(property);
			if (!finished || (issue.getWord() != null && reported.contains(key(issue.getWord(),
					property)))) {
				continue;
			}
			removals.add(new AnnotationAction(result.assistantId() + ":" + targetRef.entityType()
					+ ":" + targetRef.entityId() + ":legacy-issue:" + issue.getId(),
					AnnotationAction.ActionType.REMOVE_ANNOTATION_FROM_ANNOTATABLE, targetRef, null,
					null, null, null, List.of(), Map.of(LEGACY_ANNOTATION_ID, issue.getId())));
		}
		if (removals.isEmpty()) {
			return result;
		}
		List<AnnotationAction> actions = new ArrayList<>(removals);
		actions.addAll(result.annotationActions());
		return new AssistantResult(result.assistantId(), result.runId(), result.summary(),
				result.severity(), actions, result.messages(), result.externalActions(),
				result.metadata());
	}

	private static Set<String> reportedKeys(AssistantResult result) {
		Set<String> keys = new HashSet<>();
		for (AnnotationAction action : result.annotationActions()) {
			if (action.actionType() != AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE) {
				continue;
			}
			Object word = action.metadata().get("word");
			if (word != null) {
				Object property = action.metadata().get("annotatableEntityPropertyName");
				keys.add(key(word.toString(), property == null ? null : property.toString()));
			}
		}
		return keys;
	}

	private static String key(String word, String property) {
		return word.trim().toLowerCase(Locale.ROOT) + "|" + (property == null ? "" : property);
	}

	private static boolean hasProperty(LexicalIssue issue) {
		return issue.getAnnotatableEntityPropertyName() != null;
	}

	private static boolean contains(LexicalIssue issue, String marker) {
		return issue.getText() != null && issue.getText().contains(marker);
	}

	private static boolean isAssistantSourced(LexicalIssue issue) {
		return issue.getSource() != null && issue.getSource().startsWith("ASSISTANT:");
	}
}
