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
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.Linkdef;
import com.rreganjr.nlp.dictionary.NLPProcessor;
import com.rreganjr.nlp.dictionary.NLPProcessorFactory;
import com.rreganjr.nlp.dictionary.NLPText;
import com.rreganjr.nlp.dictionary.ParseTag;
import com.rreganjr.nlp.dictionary.PartOfSpeech;
import com.rreganjr.nlp.dictionary.Sense;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.CleanupPolicy;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.EvidenceRef;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.assistant.core.context.RedactablePatterns;
import com.rreganjr.requel.project.TextEntity;

/**
 * SPI adapter reproducing the legacy {@code LexicalAssistant} vague-word check as
 * {@link AnnotationAction}s. For each word whose WordNet sense has information
 * content below the threshold (i.e. it is semantically vague), it emits a lexical
 * "vague word" issue plus an ignore position and one change-spelling position per
 * more-specific-word suggestion. Named entities and the generic root-noun sense on
 * proper nouns are excluded, matching the legacy logic.
 *
 * <p>
 * Part of the incremental legacy lexical parity work (issue #43, Phase 4.5
 * Step 4d). Runs in the same dispatch as the other lexical adapters; each
 * processes the Name and Text properties of the {@link TextEntity}.
 */
@ConditionalOnProperty(name = "requel.nlp.enabled", havingValue = "true", matchIfMissing = true)
@Component
public class LexicalVagueWordAssistant implements RequelAssistant<TextEntity> {

	private static final Logger log = LoggerFactory.getLogger(LexicalVagueWordAssistant.class);

	public static final String ASSISTANT_ID = "legacy-lexical-vague-word";

	private static final String PROP_NAME = "Name";
	private static final String PROP_TEXT = "Text";

	private static final String VAGUE_WORD_MSG =
			"The word \"{0}\" in the {1} is vague and may lead to ambiguity.";
	private static final String IGNORE_WORD_MSG = "Ignore this word.";
	private static final String SUGGESTED_MORE_SPECIFIC_WORD_MSG =
			"Change the word \"{0}\" to \"{1}\".";

	/** Below this WordNet information content a word is reported as vague. */
	static final double INFO_CONTENT_THRESHOLD = 0.50;

	/**
	 * Issue #268: weak requirements words WordNet can't score as vague (most are modals or have
	 * specific-sounding senses), reported whatever their sense. "can" is deliberately absent: in a
	 * requirement it usually states a capability, not a hedge.
	 */
	static final Set<String> WEAK_WORDS = Set.of("should", "may", "might", "could", "easy",
			"easily", "fast", "quickly", "robust", "flexible", "sufficient", "adequate",
			"appropriate", "appropriately", "reasonable", "user-friendly", "efficient", "several",
			"various", "etc", "etc.");

	private final NLPProcessorFactory nlpProcessorFactory;
	private final DictionaryRepository dictionaryRepository;

	@Autowired
	public LexicalVagueWordAssistant(NLPProcessorFactory nlpProcessorFactory,
			DictionaryRepository dictionaryRepository) {
		this.nlpProcessorFactory = nlpProcessorFactory;
		this.dictionaryRepository = dictionaryRepository;
	}

	@Override
	public String assistantId() {
		return ASSISTANT_ID;
	}

	@Override
	public Class<TextEntity> targetType() {
		return TextEntity.class;
	}

	/**
	 * Vague-word findings become stale when the flagged word is replaced, so this
	 * assistant auto-resolves untouched findings a re-run no longer reports.
	 * Resolved issues are preserved.
	 */
	@Override
	public CleanupPolicy cleanupPolicy() {
		return CleanupPolicy.AUTO_RESOLVE_IF_UNTOUCHED;
	}

	/** Issue #268: a project can switch this check off. */
	@Override
	public boolean projectSwitchable() {
		return true;
	}

	@Override
	public String displayName() {
		return "Vague words";
	}

	@Override
	public AssistantResult analyze(AssistantContext context, TextEntity target) {
		String entityType = target.getProjectOrDomainEntityInterface().getSimpleName();
		EntityRef targetRef = EntityRef.of(entityType, target.getId());
		AssistantResult.Builder builder = AssistantResult.builder()
				.assistantId(ASSISTANT_ID)
				.runId(context.runId())
				.summary("Lexical vague-word analysis");
		// #268: a failure on one property leaves the other's findings and marks the result
		// incomplete, so nothing is auto-resolved on the strength of it.
		PropertyChecks checks = new PropertyChecks(log, "vague-word", targetRef);
		checks.run(PROP_NAME, target.getName(),
				() -> analyzeProperty(builder, targetRef, PROP_NAME, target.getName()));
		checks.run(PROP_TEXT, target.getText(),
				() -> analyzeProperty(builder, targetRef, PROP_TEXT, target.getText()));
		return LegacyLexicalIssues.withRemovals(checks.finish(builder).build(), target, targetRef,
				LegacyLexicalIssues.VAGUE_WORD, checks);
	}

	private void analyzeProperty(AssistantResult.Builder builder, EntityRef targetRef,
			String propertyName, String text) {
		if (text == null || text.isBlank()) {
			return;
		}
		NLPText nlpText = nlpProcessorFactory.processText(text);
		NLPProcessor<Collection<NLPText>> moreSpecificWordSuggester = nlpProcessorFactory
				.getMoreSpecificWordSuggester();
		Linkdef linkType = dictionaryRepository.findLinkDef(1L);
		Sense rootNounSense = dictionaryRepository.findSense("entity", PartOfSpeech.NOUN, 1);

		// #359: a word inside an email address, URL or credential isn't the author's prose.
		List<RedactablePatterns.Span> spans = RedactablePatterns.spans(text);
		for (NLPText word : nlpText.getLeaves()) {
			if (!isVague(word, linkType, rootNounSense)) {
				continue;
			}
			if (LexicalEvidence.onlyInside(text, word.getText(), spans)) {
				continue;
			}
			// #268/#269: never report a word the author didn't write (a parser fragment).
			if (!LexicalEvidence.occurs(text, word.getText())) {
				log.warn("vague-word: dropping \"{}\" on the {} of {}: not in the source text",
						word.getText(), propertyName, targetRef);
				continue;
			}
			emitVagueWordIssue(builder, targetRef, propertyName, word,
					moreSpecificWordSuggester);
		}
	}

	/**
	 * Issue #268: a weak requirements word always is; otherwise only a noun, adjective or adverb
	 * whose WordNet sense carries little information is. Verbs are never scored: on the roundtable
	 * project every verb the information-content test flagged ("be", "says", "creates",
	 * "covering") was noise.
	 */
	private boolean isVague(NLPText word, Linkdef linkType, Sense rootNounSense) {
		String text = word.getText();
		if (text != null && WEAK_WORDS.contains(text.toLowerCase(Locale.ROOT))) {
			return true;
		}
		if (!word.in(PartOfSpeech.NOUN, PartOfSpeech.ADJECTIVE, PartOfSpeech.ADVERB)) {
			return false;
		}
		Sense sense = word.getDictionaryWordSense();
		if (word.isNamedEntity() || sense == null || sense.getSynset() == null) {
			return false;
		}
		// Proper nouns may carry the generic "entity#n#1" root sense; skip those.
		if (sense.equals(rootNounSense) && word.in(ParseTag.NNP, ParseTag.NNPS)) {
			return false;
		}
		return dictionaryRepository.infoContent(sense.getSynset(), linkType) < INFO_CONTENT_THRESHOLD;
	}

	private void emitVagueWordIssue(AssistantResult.Builder builder, EntityRef targetRef,
			String propertyName, NLPText word,
			NLPProcessor<Collection<NLPText>> moreSpecificWordSuggester) {
		String wordText = word.getText();
		String issueKey = ASSISTANT_ID + ":" + targetRef.entityType() + ":" + targetRef.entityId()
				+ ":vague-word:" + propertyName + ":" + wordText;
		List<EvidenceRef> evidence = List.of(EvidenceRef.ofLocator("property=" + propertyName),
				EvidenceRef.ofSnippet(wordText));

		Map<String, Object> issueMeta = Map.of(
				"kind", "LEXICAL",
				"word", wordText,
				"annotatableEntityPropertyName", propertyName,
				"mustResolve", Boolean.FALSE,
				"findingType", "vague-word");
		builder.annotationAction(new AnnotationAction(issueKey,
				AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE, targetRef, null,
				MessageFormat.format(VAGUE_WORD_MSG, wordText, propertyName), null, null, evidence,
				issueMeta));

		builder.annotationAction(new AnnotationAction(issueKey + ":ignore",
				AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, issueKey,
				IGNORE_WORD_MSG, null, null, evidence, Map.of("kind", "IGNORE")));

		// #268: a weak word may have no sense ("should"); it has nothing more specific.
		Collection<NLPText> suggestions = word.getDictionaryWordSense() == null ? null
				: moreSpecificWordSuggester.process(word);
		if (suggestions != null) {
			for (NLPText suggestion : suggestions) {
				String suggestedWord = suggestion.getText();
				builder.annotationAction(new AnnotationAction(
						issueKey + ":more-specific:" + suggestedWord,
						AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, issueKey,
						MessageFormat.format(SUGGESTED_MORE_SPECIFIC_WORD_MSG, wordText,
								suggestedWord),
						null, null, evidence, Map.of("kind", "CHANGE_SPELLING", "proposedWord",
								suggestedWord)));
			}
		}
	}
}
