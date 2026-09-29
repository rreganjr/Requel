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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.Linkdef;
import com.rreganjr.nlp.dictionary.NLPProcessor;
import com.rreganjr.nlp.dictionary.NLPProcessorFactory;
import com.rreganjr.nlp.dictionary.NLPText;
import com.rreganjr.nlp.dictionary.PartOfSpeech;
import com.rreganjr.nlp.dictionary.ParseTag;
import com.rreganjr.nlp.dictionary.Sense;
import com.rreganjr.nlp.dictionary.Synset;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.project.TextEntity;

class LexicalVagueWordAssistantTest {

	private final NLPProcessorFactory nlpProcessorFactory = mock(NLPProcessorFactory.class);
	private final DictionaryRepository dictionaryRepository = mock(DictionaryRepository.class);
	private final LexicalVagueWordAssistant assistant = new LexicalVagueWordAssistant(
			nlpProcessorFactory, dictionaryRepository);

	@Test
	void declaresIdentityAndTargetType() {
		assertThat(assistant.assistantId()).isEqualTo("legacy-lexical-vague-word");
		assertThat(assistant.targetType()).isEqualTo(TextEntity.class);
	}

	@Test
	void blankTextProducesNoActionsAndNoNlpWork() {
		AssistantResult result = assistant.analyze(context(), textEntity("", ""));
		assertThat(result.annotationActions()).isEmpty();
	}

	/**
	 * #268: only nouns, adjectives and adverbs are scored ("be" is a verb with a low score and is
	 * not reported), a weak requirements word is reported whatever its sense ("should"), and "can"
	 * is not a weak word.
	 */
	@Test
	void scoresOnlyNounsAdjectivesAndAdverbsAndAddsTheWeakWords() {
		String text = "The system should be ready for the event and can scale fast.";
		Linkdef linkType = mock(Linkdef.class);
		when(dictionaryRepository.findLinkDef(1L)).thenReturn(linkType);
		List<NLPText> leaves = new ArrayList<>();
		NLPText should = word("should", PartOfSpeech.MODAL, null, 0);
		leaves.add(should);
		leaves.add(word("be", PartOfSpeech.VERB, linkType, 0.42));
		leaves.add(word("ready", PartOfSpeech.ADJECTIVE, linkType, 0.91));
		leaves.add(word("event", PartOfSpeech.NOUN, linkType, 0.199));
		leaves.add(word("can", PartOfSpeech.MODAL, null, 0));
		leaves.add(word("scale", PartOfSpeech.VERB, linkType, 0.3));
		leaves.add(word("fast", PartOfSpeech.ADVERB, linkType, 0.95));
		NLPText nlpText = mock(NLPText.class);
		when(nlpText.getLeaves()).thenReturn(leaves);
		when(nlpProcessorFactory.processText(anyString())).thenReturn(nlpText);
		@SuppressWarnings("unchecked")
		NLPProcessor<java.util.Collection<NLPText>> suggester = mock(NLPProcessor.class);
		when(suggester.process(any())).thenReturn(List.of());
		when(nlpProcessorFactory.getMoreSpecificWordSuggester()).thenReturn(suggester);

		AssistantResult result = assistant.analyze(context(), textEntity("", text));

		assertThat(result.annotationActions()).filteredOn(
				a -> a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE)
				.extracting(a -> a.metadata().get("word"))
				.containsExactly("should", "event", "fast");
		assertThat(result.annotationActions()).filteredOn(
				a -> a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE)
				.allSatisfy(a -> assertThat(a.metadata()).containsEntry("mustResolve", Boolean.FALSE));
		// #268: "should" has no sense, so there is nothing more specific to ask for. Asking threw
		// inside the dictionary's transaction and failed the whole run.
		verify(suggester, never()).process(should);
	}

	/**
	 * #268: a word flagged vague that isn't in the text (a parser fragment) is dropped; a named
	 * entity, a noun with no sense, and a proper noun given the generic "entity" root sense are
	 * never vague.
	 */
	@Test
	void fragmentsNamedEntitiesUnsensedAndRootSensedProperNounsAreNotReported() {
		String text = "Several Acme staff attend the thing.";
		Linkdef linkType = mock(Linkdef.class);
		when(dictionaryRepository.findLinkDef(1L)).thenReturn(linkType);
		NLPText fragment = word("ings", PartOfSpeech.NOUN, linkType, 0.1);
		NLPText named = word("Acme", PartOfSpeech.NOUN, linkType, 0.1);
		when(named.isNamedEntity()).thenReturn(true);
		NLPText unsensed = word("staff", PartOfSpeech.NOUN, null, 0);
		NLPText rootSensed = word("Entity", PartOfSpeech.NOUN, linkType, 0.1);
		Sense rootSense = rootSensed.getDictionaryWordSense();
		when(dictionaryRepository.findSense("entity", PartOfSpeech.NOUN, 1)).thenReturn(rootSense);
		when(rootSensed.in(any(ParseTag[].class))).thenReturn(true);
		NLPText weak = word("Several", PartOfSpeech.ADJECTIVE, null, 0);
		List<NLPText> leaves = List.of(fragment, named, unsensed, rootSensed, weak);
		NLPText nlpText = mock(NLPText.class);
		when(nlpText.getLeaves()).thenReturn(leaves);
		when(nlpProcessorFactory.processText(anyString())).thenReturn(nlpText);
		@SuppressWarnings("unchecked")
		NLPProcessor<java.util.Collection<NLPText>> suggester = mock(NLPProcessor.class);
		when(nlpProcessorFactory.getMoreSpecificWordSuggester()).thenReturn(suggester);

		AssistantResult result = assistant.analyze(context(), textEntity("", text));

		assertThat(result.annotationActions()).filteredOn(
				a -> a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE)
				.extracting(a -> a.metadata().get("word")).containsExactly("Several");
	}

	/** A leaf of the given part of speech whose sense scores {@code infoContent}; no sense if linkType is null. */
	private NLPText word(String text, PartOfSpeech pos, Linkdef linkType, double infoContent) {
		NLPText word = mock(NLPText.class);
		when(word.getText()).thenReturn(text);
		when(word.in(any(PartOfSpeech[].class)))
				.thenAnswer(inv -> Arrays.asList(inv.getArguments()).contains(pos));
		if (linkType != null) {
			Sense sense = mock(Sense.class);
			Synset synset = mock(Synset.class);
			when(sense.getSynset()).thenReturn(synset);
			when(word.getDictionaryWordSense()).thenReturn(sense);
			when(dictionaryRepository.infoContent(synset, linkType)).thenReturn(infoContent);
		}
		return word;
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
