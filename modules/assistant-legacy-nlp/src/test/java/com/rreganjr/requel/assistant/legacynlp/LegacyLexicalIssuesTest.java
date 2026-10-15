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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.impl.LexicalIssue;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.core.CommandBackedAssistantResultApplicator;
import com.rreganjr.requel.project.TextEntity;

/** Issue #268: which of the old path's issues an analysis removes. */
class LegacyLexicalIssuesTest {

	private static final EntityRef GOAL = EntityRef.of("Goal", 1L);
	private static final String SPELLING_TEXT = "The word \"groal\" in the Name is not recognized";

	private final Set<Annotation> annotations = new LinkedHashSet<>();
	private final PropertyChecks checks = new PropertyChecks(
			LoggerFactory.getLogger(LegacyLexicalIssuesTest.class), "test", GOAL);

	LegacyLexicalIssuesTest() {
		checks.run("Name", "done", () -> {
		});
		checks.run("Text", "done", () -> {
		});
	}

	@Test
	void anEntityWithNoAnnotationsGetsTheResultBackUnchanged() {
		AssistantResult result = result();
		TextEntity entity = mock(TextEntity.class);
		doReturn(null).when(entity).getAnnotations();

		assertThat(LegacyLexicalIssues.withRemovals(result, entity, GOAL,
				LegacyLexicalIssues.SPELLING, checks)).isSameAs(result);
		assertThat(LegacyLexicalIssues.withRemovals(result, entity(), GOAL,
				LegacyLexicalIssues.SPELLING, checks)).isSameAs(result);
	}

	@Test
	void onlyAnUnresolvedUnownedLexicalIssueWithAnIdAndTheKindsTextIsRemoved() {
		annotations.add(mock(Issue.class));
		annotations.add(issue(null, "Name", "groal", SPELLING_TEXT, null));
		annotations.add(issue(2L, "Name", "groal", null, null));
		annotations.add(issue(3L, "Name", "groal", SPELLING_TEXT, "IMPORTED"));
		annotations.add(issue(4L, "Name", "groal", SPELLING_TEXT, "ASSISTANT:legacy-lexical"));
		annotations.add(issue(5L, null, "groal", SPELLING_TEXT, null));
		annotations.add(issue(6L, "Name", "groal", SPELLING_TEXT, null));

		assertThat(removals(LegacyLexicalIssues.withRemovals(result(), entity(), GOAL,
				LegacyLexicalIssues.SPELLING, checks))).containsExactly(3L, 6L);
	}

	@Test
	void anIssueTheRunReportsAgainIsKeptWhateverTheCase() {
		annotations.add(issue(7L, "Name", "Groal", SPELLING_TEXT, null));
		annotations.add(issue(8L, null, "the Zoom room",
				"The phrase \"the Zoom room\" is a potential glossary term", null));
		AssistantResult reported = result(
				issueAction("groal", "Name"), issueAction("THE ZOOM ROOM", null),
				issueAction(null, "Text"),
				new AnnotationAction("x:position", AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION,
						null, "x", "Ignore", null, null, List.of(), Map.of()));

		assertThat(removals(LegacyLexicalIssues.withRemovals(reported, entity(), GOAL,
				LegacyLexicalIssues.SPELLING, checks))).isEmpty();
		assertThat(removals(LegacyLexicalIssues.withRemovals(reported, entity(), GOAL,
				LegacyLexicalIssues.GLOSSARY_TERM, checks))).isEmpty();
	}

	@Test
	void theKindPredicatesCheckTheProperty() {
		LexicalIssue withProperty = issue(9L, "Text", "x", "is vague and is complex and is not"
				+ " recognized and is a potential glossary term", null);
		LexicalIssue withoutProperty = issue(10L, null, "x", "is vague and is complex and is not"
				+ " recognized and is a potential glossary term", null);

		assertThat(LegacyLexicalIssues.SPELLING.test(withProperty)).isTrue();
		assertThat(LegacyLexicalIssues.SPELLING.test(withoutProperty)).isFalse();
		assertThat(LegacyLexicalIssues.VAGUE_WORD.test(withProperty)).isTrue();
		assertThat(LegacyLexicalIssues.VAGUE_WORD.test(withoutProperty)).isFalse();
		assertThat(LegacyLexicalIssues.COMPLEXITY.test(withProperty)).isTrue();
		assertThat(LegacyLexicalIssues.COMPLEXITY.test(withoutProperty)).isFalse();
		assertThat(LegacyLexicalIssues.GLOSSARY_TERM.test(withProperty)).isFalse();
		assertThat(LegacyLexicalIssues.GLOSSARY_TERM.test(withoutProperty)).isTrue();
		assertThat(LegacyLexicalIssues.SPELLING.test(issue(11L, "Name", "x", "other", null)))
				.isFalse();
	}

	// ---- fixtures -------------------------------------------------------------------------

	private TextEntity entity() {
		TextEntity entity = mock(TextEntity.class);
		doReturn(annotations).when(entity).getAnnotations();
		return entity;
	}

	private static AssistantResult result(AnnotationAction... actions) {
		AssistantResult.Builder builder = AssistantResult.builder().assistantId("legacy-lexical");
		for (AnnotationAction action : actions) {
			builder.annotationAction(action);
		}
		return builder.build();
	}

	private static AnnotationAction issueAction(String word, String property) {
		Map<String, Object> metadata = new java.util.HashMap<>();
		if (word != null) {
			metadata.put("word", word);
		}
		if (property != null) {
			metadata.put("annotatableEntityPropertyName", property);
		}
		return new AnnotationAction("k:" + word + ":" + property,
				AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE, GOAL, null, "issue", null, null,
				List.of(), metadata);
	}

	private static LexicalIssue issue(Long id, String property, String word, String text,
			String source) {
		LexicalIssue issue = mock(LexicalIssue.class);
		when(issue.getId()).thenReturn(id);
		when(issue.getAnnotatableEntityPropertyName()).thenReturn(property);
		when(issue.getWord()).thenReturn(word);
		when(issue.getText()).thenReturn(text);
		when(issue.getSource()).thenReturn(source);
		return issue;
	}

	private static List<Object> removals(AssistantResult result) {
		return result.annotationActions().stream()
				.filter(a -> a.actionType()
						== AnnotationAction.ActionType.REMOVE_ANNOTATION_FROM_ANNOTATABLE)
				.map(a -> a.metadata().get(CommandBackedAssistantResultApplicator.LEGACY_ANNOTATION_ID))
				.toList();
	}
}
