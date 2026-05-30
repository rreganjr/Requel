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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rreganjr.nlp.dictionary.NLPProcessor;
import com.rreganjr.nlp.dictionary.NLPProcessorFactory;
import com.rreganjr.nlp.dictionary.NLPText;
import com.rreganjr.nlp.dictionary.ParseTag;
import com.rreganjr.nlp.dictionary.PartOfSpeech;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.EvidenceRef;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.project.TextEntity;

/**
 * SPI adapter that reproduces the legacy {@code LexicalAssistant} spelling check
 * as {@link AnnotationAction}s rather than creating annotations directly. It
 * analyses the Name and Text properties of any {@link TextEntity} and, for each
 * word the spell checker rejects, emits a lexical "unknown word" issue plus the
 * same set of resolve-positions the legacy assistant created: add-to-dictionary,
 * ignore, add-to-glossary, add-as-actor, and one change-spelling position per
 * suggested correction. The {@code AssistantResultApplicator} turns these into
 * annotations through the existing command stack.
 *
 * <p>
 * This is the first of the legacy lexical checks ported to the SPI (issue #43,
 * Phase 4.5 Step 4b). Vague-word, glossary-term, and complexity checks follow as
 * additional adapters; together they reach parity with the legacy path.
 */
@Component
public class LexicalSpellingAssistant implements RequelAssistant<TextEntity> {

	public static final String ASSISTANT_ID = "legacy-lexical";

	private static final String PROP_NAME = "Name";
	private static final String PROP_TEXT = "Text";

	private static final String UNKNOWN_WORD_MSG =
			"The word \"{0}\" in the {1} is not recognized and may be spelled incorrectly.";
	private static final String IGNORE_WORD_MSG = "Ignore this word.";
	private static final String ADD_TO_DICTIONARY_MSG = "Add \"{0}\" to the dictionary.";
	private static final String SUGGESTED_SPELLING_MSG = "Change the word \"{0}\" to \"{1}\".";
	private static final String ADD_TO_GLOSSARY_MSG = "Add \"{0}\" to the project glossary.";
	private static final String ADD_AS_ACTOR_MSG = "Add \"{0}\" as an actor to the project.";

	private final NLPProcessorFactory nlpProcessorFactory;

	@Autowired
	public LexicalSpellingAssistant(NLPProcessorFactory nlpProcessorFactory) {
		this.nlpProcessorFactory = nlpProcessorFactory;
	}

	@Override
	public String assistantId() {
		return ASSISTANT_ID;
	}

	@Override
	public Class<TextEntity> targetType() {
		return TextEntity.class;
	}

	@Override
	public AssistantResult analyze(AssistantContext context, TextEntity target) {
		String entityType = target.getProjectOrDomainEntityInterface().getSimpleName();
		EntityRef targetRef = EntityRef.of(entityType, target.getId());
		AssistantResult.Builder builder = AssistantResult.builder()
				.assistantId(ASSISTANT_ID)
				.runId(context.runId())
				.summary("Lexical spelling analysis");
		analyzeProperty(builder, targetRef, entityType, target.getId(), PROP_NAME, target.getName());
		analyzeProperty(builder, targetRef, entityType, target.getId(), PROP_TEXT, target.getText());
		return builder.build();
	}

	private void analyzeProperty(AssistantResult.Builder builder, EntityRef targetRef,
			String entityType, Long entityId, String propertyName, String text) {
		if (text == null || text.isBlank()) {
			return;
		}
		NLPText nlpText = nlpProcessorFactory.processText(text);
		NLPProcessor<Boolean> spellChecker = nlpProcessorFactory.getSpellingChecker();
		NLPProcessor<Collection<NLPText>> similarWordFinder = nlpProcessorFactory
				.getSimilarWordFinder();
		for (NLPText word : nlpText.getLeaves()) {
			if (word.in(PartOfSpeech.PUNCTUATION, PartOfSpeech.NUMBER, PartOfSpeech.SYMBOL)
					|| word.in(ParseTag.POS, ParseTag.CD)) {
				continue;
			}
			if (Boolean.TRUE.equals(spellChecker.process(word))) {
				continue;
			}
			emitSpellingIssue(builder, targetRef, entityType, entityId, propertyName, word,
					similarWordFinder);
		}
	}

	private void emitSpellingIssue(AssistantResult.Builder builder, EntityRef targetRef,
			String entityType, Long entityId, String propertyName, NLPText word,
			NLPProcessor<Collection<NLPText>> similarWordFinder) {
		String wordText = word.getText();
		String issueKey = ASSISTANT_ID + ":" + entityType + ":" + entityId + ":unknown-word:"
				+ propertyName + ":" + wordText;
		List<EvidenceRef> evidence = List.of(EvidenceRef.ofLocator("property=" + propertyName),
				EvidenceRef.ofSnippet(wordText));

		Map<String, Object> issueMeta = Map.of(
				"kind", "LEXICAL",
				"word", wordText,
				"annotatableEntityPropertyName", propertyName,
				"mustResolve", Boolean.TRUE,
				"findingType", "unknown-word");
		builder.annotationAction(new AnnotationAction(issueKey,
				AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE, targetRef, null,
				MessageFormat.format(UNKNOWN_WORD_MSG, wordText, propertyName), null, null, evidence,
				issueMeta));

		builder.annotationAction(position(issueKey + ":add-dictionary", issueKey,
				"ADD_WORD_TO_DICTIONARY", MessageFormat.format(ADD_TO_DICTIONARY_MSG, wordText),
				evidence));
		builder.annotationAction(position(issueKey + ":ignore", issueKey, null, IGNORE_WORD_MSG,
				evidence));
		builder.annotationAction(position(issueKey + ":add-glossary", issueKey,
				"ADD_WORD_TO_GLOSSARY", MessageFormat.format(ADD_TO_GLOSSARY_MSG, wordText),
				evidence));
		builder.annotationAction(position(issueKey + ":add-actor", issueKey, "ADD_ACTOR_TO_PROJECT",
				MessageFormat.format(ADD_AS_ACTOR_MSG, wordText), evidence));

		Collection<NLPText> suggestions = similarWordFinder.process(word);
		if (suggestions != null) {
			for (NLPText suggestion : suggestions) {
				String suggestedWord = suggestion.getText();
				builder.annotationAction(new AnnotationAction(
						issueKey + ":change-spelling:" + suggestedWord,
						AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, issueKey,
						MessageFormat.format(SUGGESTED_SPELLING_MSG, wordText, suggestedWord), null,
						null, evidence, Map.of("kind", "CHANGE_SPELLING", "proposedWord",
								suggestedWord)));
			}
		}
	}

	private static AnnotationAction position(String actionKey, String parentIssueKey, String kind,
			String text, List<EvidenceRef> evidence) {
		Map<String, Object> metadata = kind == null ? Map.of() : Map.of("kind", kind);
		return new AnnotationAction(actionKey, AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION,
				null, parentIssueKey, text, null, null, evidence, metadata);
	}
}
