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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.rreganjr.nlp.dictionary.NLPProcessor;
import com.rreganjr.nlp.dictionary.NLPProcessorFactory;
import com.rreganjr.nlp.dictionary.NLPText;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.TextEntity;
import com.rreganjr.requel.project.exception.NoSuchActorException;
import com.rreganjr.requel.project.exception.NoSuchGlossaryTermException;

class LexicalGlossaryTermAssistantTest {

	private final NLPProcessorFactory nlpProcessorFactory = mock(NLPProcessorFactory.class);
	private final ProjectRepository projectRepository = mock(ProjectRepository.class);
	private final LexicalGlossaryTermAssistant assistant = new LexicalGlossaryTermAssistant(
			nlpProcessorFactory, projectRepository);

	@Test
	void declaresIdentityAndTargetType() {
		assertThat(assistant.assistantId()).isEqualTo("legacy-lexical-glossary-term");
		assertThat(assistant.targetType()).isEqualTo(TextEntity.class);
	}

	@Test
	void blankTextProducesNoActions() {
		AssistantResult result = assistant.analyze(context(), textEntity("", ""));
		assertThat(result.annotationActions()).isEmpty();
	}

	/**
	 * #268/#269: the issue quotes the phrase as the author wrote it ("force-stops", which the parser
	 * splits into "force - stops"), and a phrase that isn't in the text (the pre-#314 fragment
	 * "ermissions matrix") raises nothing.
	 */
	@Test
	void quotesThePhraseAsWrittenAndDropsOneThatIsNotInTheText() {
		String text = "The Operator force-stops the stream and checks the permissions matrix.";
		ProjectOrDomain projectOrDomain = mock(ProjectOrDomain.class);
		NLPText nlpText = mock(NLPText.class);
		@SuppressWarnings("unchecked")
		NLPProcessor<Collection<NLPText>> nounPhraseFinder = mock(NLPProcessor.class);
		when(nlpProcessorFactory.processText(text)).thenReturn(nlpText);
		when(nlpProcessorFactory.getNounPhraseFinder()).thenReturn(nounPhraseFinder);
		// Built before the when(): creating and stubbing mocks inside a thenReturn() argument is an
		// unfinished stubbing.
		NLPText written = phrase("force", "-", "stops", "the", "stream");
		NLPText fragment = phrase("ermissions", "matrix");
		when(nounPhraseFinder.process(nlpText)).thenReturn(List.of(written, fragment));
		when(projectRepository.findGlossaryTermForProjectOrDomain(any(), anyString()))
				.thenThrow(NoSuchGlossaryTermException.class);
		when(projectRepository.findActorByProjectOrDomainAndName(any(), anyString()))
				.thenThrow(NoSuchActorException.class);

		AssistantResult result = assistant.analyze(context(),
				textEntity(projectOrDomain, "", text));

		List<AnnotationAction> issues = result.annotationActions().stream()
				.filter(a -> a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE)
				.toList();
		assertThat(issues).hasSize(1);
		assertThat(issues.get(0).metadata()).containsEntry("word", "force-stops the stream");
		assertThat(issues.get(0).text()).contains("\"force-stops the stream\"");
	}

	/** A multi-word phrase whose leaves print as {@code words}; everything else is a mock default. */
	private static NLPText phrase(String... words) {
		List<NLPText> leaves = new ArrayList<>();
		for (String word : words) {
			NLPText leaf = mock(NLPText.class);
			when(leaf.getText()).thenReturn(word);
			leaves.add(leaf);
		}
		NLPText phrase = mock(NLPText.class);
		when(phrase.getLeaves()).thenReturn(leaves);
		when(phrase.getText()).thenReturn(String.join(" ", words));
		return phrase;
	}

	private static TextEntity textEntity(ProjectOrDomain projectOrDomain, String name,
			String text) {
		TextEntity entity = textEntity(name, text);
		doReturn(projectOrDomain).when(entity).getProjectOrDomain();
		return entity;
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
		// getProjectOrDomain() returns null in this case -> analyzeProperty short-circuits,
		// but blank text already prevents any NLP/repository interaction.
		return entity;
	}
}
