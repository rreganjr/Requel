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

import com.rreganjr.nlp.dictionary.GrammaticalStructureLevel;
import com.rreganjr.nlp.dictionary.NLPProcessor;
import com.rreganjr.nlp.dictionary.NLPProcessorFactory;
import com.rreganjr.nlp.dictionary.NLPText;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.project.TextEntity;

class LexicalComplexityAssistantTest {

	private final NLPProcessorFactory nlpProcessorFactory = mock(NLPProcessorFactory.class);
	private final LexicalComplexityAssistant assistant = new LexicalComplexityAssistant(
			nlpProcessorFactory);

	@Test
	void declaresIdentityAndTargetType() {
		assertThat(assistant.assistantId()).isEqualTo("legacy-lexical-complexity");
		assertThat(assistant.targetType()).isEqualTo(TextEntity.class);
	}

	/** #268/#269: a complex sentence is quoted only if the author wrote it. */
	@Test
	void aComplexSentenceIsReportedOnlyIfItIsInTheSourceText() {
		String written = "The operator rotates the key while the stream that the panel feeds is live.";
		AssistantResult found = analyzeOneComplexSentence(written, written);
		assertThat(found.annotationActions()).hasSize(2);
		assertThat(found.annotationActions()).filteredOn(
				a -> a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE)
				.singleElement().satisfies(
						a -> assertThat(a.metadata()).containsEntry("mustResolve", Boolean.FALSE));
		assertThat(analyzeOneComplexSentence(written, "le, not a person that the panel feeds")
				.annotationActions()).isEmpty();
	}

	/** A paragraph is checked sentence by sentence; only the deep one is reported. */
	@Test
	void eachSentenceOfAParagraphIsCheckedOnItsOwn() {
		String text = "The host starts. The operator rotates the key while the stream that the panel feeds is live.";
		@SuppressWarnings("unchecked")
		NLPProcessor<Integer> depthFinder = mock(NLPProcessor.class);
		NLPText paragraph = mock(NLPText.class);
		NLPText shallow = mock(NLPText.class);
		NLPText deep = mock(NLPText.class);
		when(nlpProcessorFactory.processText(text)).thenReturn(paragraph);
		when(nlpProcessorFactory.getConstituentTreeDepthFinder()).thenReturn(depthFinder);
		when(paragraph.is(GrammaticalStructureLevel.PARAGRAPH)).thenReturn(true);
		when(paragraph.getChildren()).thenReturn(java.util.List.of(shallow, deep));
		for (NLPText sentence : java.util.List.of(shallow, deep)) {
			when(sentence.is(GrammaticalStructureLevel.PARAGRAPH)).thenReturn(false);
			when(sentence.is(GrammaticalStructureLevel.SENTENCE)).thenReturn(true);
		}
		when(shallow.getText()).thenReturn("The host starts.");
		when(deep.getText()).thenReturn(
				"The operator rotates the key while the stream that the panel feeds is live.");
		when(depthFinder.process(shallow)).thenReturn(3);
		when(depthFinder.process(deep)).thenReturn(99);

		AssistantResult result = assistant.analyze(context(), textEntity("", text));

		assertThat(result.annotationActions()).filteredOn(
				a -> a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE)
				.singleElement().satisfies(a -> assertThat(a.text()).contains("rotates the key"));
	}

	private AssistantResult analyzeOneComplexSentence(String text, String sentenceText) {
		@SuppressWarnings("unchecked")
		NLPProcessor<Integer> depthFinder = mock(NLPProcessor.class);
		NLPText sentence = mock(NLPText.class);
		when(nlpProcessorFactory.processText(text)).thenReturn(sentence);
		when(nlpProcessorFactory.getConstituentTreeDepthFinder()).thenReturn(depthFinder);
		when(sentence.is(GrammaticalStructureLevel.PARAGRAPH)).thenReturn(false);
		when(sentence.is(GrammaticalStructureLevel.SENTENCE)).thenReturn(true);
		when(sentence.getText()).thenReturn(sentenceText);
		when(depthFinder.process(sentence)).thenReturn(99);
		return assistant.analyze(context(), textEntity("", text));
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

	private static TextEntity textEntity(String name, String text) {
		TextEntity entity = mock(TextEntity.class);
		Class<?> entityInterface = TextEntity.class;
		doReturn(entityInterface).when(entity).getProjectOrDomainEntityInterface();
		when(entity.getId()).thenReturn(1L);
		when(entity.getName()).thenReturn(name);
		when(entity.getText()).thenReturn(text);
		return entity;
	}
}
