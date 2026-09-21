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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.rreganjr.nlp.dictionary.NLPProcessor;
import com.rreganjr.nlp.dictionary.NLPProcessorFactory;
import com.rreganjr.nlp.dictionary.NLPText;
import com.rreganjr.nlp.dictionary.ParseTag;
import com.rreganjr.nlp.dictionary.PartOfSpeech;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.project.TextEntity;

class LexicalSpellingAssistantTest {

	private final NLPProcessorFactory nlpProcessorFactory = mock(NLPProcessorFactory.class);
	private final LexicalSpellingAssistant assistant = new LexicalSpellingAssistant(
			nlpProcessorFactory);

	@Test
	void declaresIdentityAndTargetType() {
		assertThat(assistant.assistantId()).isEqualTo("legacy-lexical");
		assertThat(assistant.targetType()).isEqualTo(TextEntity.class);
	}

	@Test
	void emitsLexicalIssueAndPositionsForMisspelledWord() {
		@SuppressWarnings("unchecked")
		NLPProcessor<Boolean> spellChecker = mock(NLPProcessor.class);
		@SuppressWarnings("unchecked")
		NLPProcessor<java.util.Collection<NLPText>> similarWordFinder = mock(NLPProcessor.class);
		NLPText nlpText = mock(NLPText.class);
		NLPText word = mock(NLPText.class);
		NLPText suggestion = mock(NLPText.class);

		when(nlpProcessorFactory.processText(anyString())).thenReturn(nlpText);
		// Stubbed on the exact project id, not any(): the assistant must pass Project 7 from the
		// context through to the factory (issue #313). If it passes null or the wrong id the stub
		// does not match, the processors come back null and this fails.
		when(nlpProcessorFactory.getSpellingChecker(7L)).thenReturn(spellChecker);
		when(nlpProcessorFactory.getSimilarWordFinder(7L)).thenReturn(similarWordFinder);
		when(nlpText.getLeaves()).thenReturn(List.of(word));
		when(word.in(any(PartOfSpeech[].class))).thenReturn(false);
		when(word.in(any(ParseTag[].class))).thenReturn(false);
		when(word.getText()).thenReturn("datalaek");
		when(spellChecker.process(word)).thenReturn(false);
		when(similarWordFinder.process(word)).thenReturn(List.of(suggestion));
		when(suggestion.getText()).thenReturn("data lake");

		AssistantResult result = assistant.analyze(context(), textEntity("", "datalaek"));

		// 1 issue + add-dictionary + ignore + add-glossary + add-actor + 1 change-spelling
		assertThat(result.annotationActions()).hasSize(6);
		AnnotationAction issue = result.annotationActions().get(0);
		assertThat(issue.actionType()).isEqualTo(AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE);
		assertThat(issue.metadata()).containsEntry("kind", "LEXICAL")
				.containsEntry("word", "datalaek").containsEntry("findingType", "unknown-word");
		assertThat(issue.targetRef()).isEqualTo(EntityRef.of("TextEntity", 1L));
		assertThat(result.annotationActions()).filteredOn(
				a -> a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION)
				.allMatch(a -> issue.actionKey().equals(a.parentActionKey()));
		assertThat(result.annotationActions()).anyMatch(
				a -> "CHANGE_SPELLING".equals(a.metadata().get("kind")));
	}

	/**
	 * {@code AssistantContext.projectRef()} is nullable, so a run with no project must still
	 * analyze — against the two installation-wide dictionary layers alone (issue #313).
	 */
	@Test
	void aRunWithNoProjectStillAnalyzesAgainstTheInstallationWideDictionary() {
		@SuppressWarnings("unchecked")
		NLPProcessor<Boolean> spellChecker = mock(NLPProcessor.class);
		@SuppressWarnings("unchecked")
		NLPProcessor<java.util.Collection<NLPText>> similarWordFinder = mock(NLPProcessor.class);
		NLPText nlpText = mock(NLPText.class);
		NLPText word = mock(NLPText.class);

		when(nlpProcessorFactory.processText(anyString())).thenReturn(nlpText);
		when(nlpProcessorFactory.getSpellingChecker(null)).thenReturn(spellChecker);
		when(nlpProcessorFactory.getSimilarWordFinder(null)).thenReturn(similarWordFinder);
		when(nlpText.getLeaves()).thenReturn(List.of(word));
		when(word.in(any(PartOfSpeech[].class))).thenReturn(false);
		when(word.in(any(ParseTag[].class))).thenReturn(false);
		when(word.getText()).thenReturn("datalaek");
		when(spellChecker.process(word)).thenReturn(false);
		when(similarWordFinder.process(word)).thenReturn(List.of());

		AssistantResult result = assistant.analyze(contextWithNoProject(),
				textEntity("", "datalaek"));

		// 1 issue + add-dictionary + ignore + add-glossary + add-actor, no change-spelling
		assertThat(result.annotationActions()).hasSize(5);
	}

	@Test
	void blankTextProducesNoActions() {
		AssistantResult result = assistant.analyze(context(), textEntity("", ""));
		assertThat(result.annotationActions()).isEmpty();
	}

	private static AssistantContext context() {
		return new AssistantContext(UUID.randomUUID(), new UserRef(3L, "ron"),
				new UserRef(11L, "assistant"), EntityRef.of("Project", 7L), Locale.US,
				Clock.systemUTC(), Map.of());
	}

	private static AssistantContext contextWithNoProject() {
		return new AssistantContext(UUID.randomUUID(), new UserRef(3L, "ron"),
				new UserRef(11L, "assistant"), null, Locale.US, Clock.systemUTC(), Map.of());
	}

	private static TextEntity textEntity(String name, String text) {
		TextEntity entity = mock(TextEntity.class);
		// doReturn avoids the Class<?> wildcard-capture mismatch that when/thenReturn hits.
		doReturn(TextEntity.class).when(entity).getProjectOrDomainEntityInterface();
		when(entity.getId()).thenReturn(1L);
		when(entity.getName()).thenReturn(name);
		when(entity.getText()).thenReturn(text);
		return entity;
	}
}
