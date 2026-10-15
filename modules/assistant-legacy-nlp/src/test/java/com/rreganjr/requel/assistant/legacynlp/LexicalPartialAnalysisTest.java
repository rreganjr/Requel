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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.NLPProcessor;
import com.rreganjr.nlp.dictionary.NLPProcessorFactory;
import com.rreganjr.nlp.dictionary.NLPText;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.impl.LexicalIssue;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.assistant.core.CommandBackedAssistantResultApplicator;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.TextEntity;

/**
 * Issue #268: what the four lexical assistants do when one property can't be analyzed, and how
 * they clear the issues the old lexical path left on an entity.
 */
class LexicalPartialAnalysisTest {

	private static final String NAME = "Route the alarm";
	private static final String TEXT = "The operator routes it.";

	private final NLPProcessorFactory nlpProcessorFactory = mock(NLPProcessorFactory.class);
	private final DictionaryRepository dictionaryRepository = mock(DictionaryRepository.class);
	private final ProjectRepository projectRepository = mock(ProjectRepository.class);
	private final ProjectOrDomain project = mock(ProjectOrDomain.class);
	private final Set<Annotation> annotations = new LinkedHashSet<>();

	LexicalPartialAnalysisTest() {
		doReturn(new TreeSet<>()).when(project).getGlossaryTerms();
		doReturn(Set.of()).when(project).getActors();
		doReturn(Set.of()).when(project).getProjectEntities();
		NLPText nothing = mock(NLPText.class);
		when(nlpProcessorFactory.processText(NAME)).thenReturn(nothing);
		@SuppressWarnings("unchecked")
		NLPProcessor<Collection<NLPText>> noPhrases = mock(NLPProcessor.class);
		when(noPhrases.process(any())).thenReturn(List.of());
		when(nlpProcessorFactory.getNounPhraseFinder()).thenReturn(noPhrases);
		when(nlpProcessorFactory.processText(TEXT))
				.thenThrow(new IllegalStateException("parser failed"));
	}

	@Test
	void eachAssistantThatFailsOnTheTextSaysItsResultIsIncomplete() {
		for (AssistantResult result : List.of(spelling(), vagueWord(), complexity(), glossary())) {
			assertThat(result.metadata()).as(result.assistantId())
					.containsEntry("incomplete", Boolean.TRUE)
					.containsEntry("failedProperties", List.of("Text"));
		}
	}

	@Test
	void aCompleteAnalysisHasNoIncompleteFlag() {
		doReturn(mock(NLPText.class)).when(nlpProcessorFactory).processText(TEXT);

		for (AssistantResult result : List.of(spelling(), vagueWord(), complexity(), glossary())) {
			assertThat(result.metadata()).as(result.assistantId()).doesNotContainKey("incomplete");
		}
	}

	@Test
	void anOldIssueOfTheAssistantsKindOnAFinishedPropertyIsRemovedFirst() {
		annotations.add(oldIssue(41L, "Name", "grout", "The word \"grout\" in the Name is not"
				+ " recognized and may be spelled incorrectly.", false, null));

		AssistantResult result = spelling();

		assertThat(removals(result)).containsExactly(41L);
		assertThat(result.annotationActions().get(0).actionType())
				.isEqualTo(AnnotationAction.ActionType.REMOVE_ANNOTATION_FROM_ANNOTATABLE);
	}

	@Test
	void anOldIssueOnAPropertyThatFailedIsLeft() {
		annotations.add(oldIssue(42L, "Text", "routs", "The word \"routs\" in the Text is not"
				+ " recognized and may be spelled incorrectly.", false, null));

		assertThat(removals(spelling())).isEmpty();
	}

	@Test
	void resolvedOwnedAndOtherKindsOfIssueAreLeft() {
		annotations.add(oldIssue(43L, "Name", "grout", "The word \"grout\" in the Name is not"
				+ " recognized and may be spelled incorrectly.", true, null));
		annotations.add(oldIssue(44L, "Name", "grout", "The word \"grout\" in the Name is not"
				+ " recognized and may be spelled incorrectly.", false, "ASSISTANT:legacy-lexical"));
		annotations.add(oldIssue(45L, "Name", "alarm", "The word \"alarm\" in the Name is vague and"
				+ " may lead to ambiguity.", false, null));

		assertThat(removals(spelling())).isEmpty();
		assertThat(removals(vagueWord())).containsExactly(45L);
	}

	@Test
	void anOldGlossaryIssueIsRemovedOnlyWhenBothPropertiesFinished() {
		annotations.add(oldIssue(46L, null, "the alarm", "The phrase \"the alarm\" is a potential"
				+ " glossary term, actor, or domain object/property", false, null));

		assertThat(removals(glossary())).isEmpty();

		doReturn(mock(NLPText.class)).when(nlpProcessorFactory).processText(TEXT);
		assertThat(removals(glossary())).containsExactly(46L);
	}

	@Test
	void anOldComplexSentenceIssueIsRemovedFromAFinishedProperty() {
		annotations.add(oldIssue(47L, "Name", null, "The text \"Route the alarm\" in the Name is"
				+ " complex and may be hard to understand.", false, null));

		assertThat(removals(complexity())).containsExactly(47L);
	}

	// ---- fixtures -------------------------------------------------------------------------

	private AssistantResult spelling() {
		return new LexicalSpellingAssistant(nlpProcessorFactory, dictionaryRepository)
				.analyze(context(), entity());
	}

	private AssistantResult vagueWord() {
		return new LexicalVagueWordAssistant(nlpProcessorFactory, dictionaryRepository)
				.analyze(context(), entity());
	}

	private AssistantResult complexity() {
		return new LexicalComplexityAssistant(nlpProcessorFactory).analyze(context(), entity());
	}

	private AssistantResult glossary() {
		return new LexicalGlossaryTermAssistant(nlpProcessorFactory, projectRepository,
				dictionaryRepository).analyze(context(), entity());
	}

	private static List<Object> removals(AssistantResult result) {
		return result.annotationActions().stream()
				.filter(a -> a.actionType()
						== AnnotationAction.ActionType.REMOVE_ANNOTATION_FROM_ANNOTATABLE)
				.map(a -> a.metadata().get(CommandBackedAssistantResultApplicator.LEGACY_ANNOTATION_ID))
				.toList();
	}

	private static LexicalIssue oldIssue(Long id, String property, String word, String text,
			boolean resolved, String source) {
		LexicalIssue issue = mock(LexicalIssue.class);
		when(issue.getId()).thenReturn(id);
		when(issue.getAnnotatableEntityPropertyName()).thenReturn(property);
		when(issue.getWord()).thenReturn(word);
		when(issue.getText()).thenReturn(text);
		when(issue.isResolved()).thenReturn(resolved);
		when(issue.getSource()).thenReturn(source);
		return issue;
	}

	private TextEntity entity() {
		TextEntity entity = mock(TextEntity.class);
		doReturn(TextEntity.class).when(entity).getProjectOrDomainEntityInterface();
		when(entity.getId()).thenReturn(1L);
		when(entity.getName()).thenReturn(NAME);
		when(entity.getText()).thenReturn(TEXT);
		doReturn(project).when(entity).getProjectOrDomain();
		doReturn(annotations).when(entity).getAnnotations();
		return entity;
	}

	private static AssistantContext context() {
		return new AssistantContext(UUID.randomUUID(), new UserRef(3L, "ron"),
				new UserRef(11L, "assistant"), EntityRef.of("Project", 7L), Locale.US,
				Clock.systemUTC(), Map.of());
	}
}
