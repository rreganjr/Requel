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
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.rreganjr.nlp.dictionary.GrammaticalStructureLevel;
import com.rreganjr.nlp.dictionary.NLPProcessor;
import com.rreganjr.nlp.dictionary.NLPProcessorFactory;
import com.rreganjr.nlp.dictionary.NLPText;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.CleanupPolicy;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.EvidenceRef;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.project.TextEntity;

/**
 * SPI adapter reproducing the legacy {@code LexicalAssistant} sentence-complexity
 * check as {@link AnnotationAction}s. Each sentence whose constituent-parse tree
 * is deeper than the threshold is flagged with a property-level (word-less)
 * lexical "complex text" issue plus an ignore position.
 *
 * <p>
 * Deviation from the legacy code: the legacy {@code addComplexityIssue} also adds
 * an "add word to dictionary" position whose text is built from
 * {@code issue.getWord()} — which is {@code null} for a complexity issue, yielding
 * a nonsensical {@code Add "null" to the dictionary.} position. That position is
 * intentionally omitted here. Part of incremental legacy parity (issue #43,
 * Phase 4.5 Step 4d).
 */
@ConditionalOnProperty(name = "requel.nlp.enabled", havingValue = "true", matchIfMissing = true)
@Component
public class LexicalComplexityAssistant implements RequelAssistant<TextEntity> {

	private static final Logger log = LoggerFactory.getLogger(LexicalComplexityAssistant.class);

	public static final String ASSISTANT_ID = "legacy-lexical-complexity";

	private static final String PROP_NAME = "Name";
	private static final String PROP_TEXT = "Text";

	private static final String COMPLEX_TEXT_MSG =
			"The text \"{0}\" in the {1} is complex and may be hard to understand.";
	private static final String IGNORE_WORD_MSG = "Ignore this word.";

	private static final int COMPLEXITY_DEPTH_THRESHOLD = 12;

	private final NLPProcessorFactory nlpProcessorFactory;

	@Autowired
	public LexicalComplexityAssistant(NLPProcessorFactory nlpProcessorFactory) {
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

	/**
	 * Complexity findings become stale when the flagged sentence is simplified or
	 * removed, so this assistant auto-resolves untouched findings a re-run no longer
	 * reports. Resolved issues are preserved.
	 */
	@Override
	public CleanupPolicy cleanupPolicy() {
		return CleanupPolicy.AUTO_RESOLVE_IF_UNTOUCHED;
	}

	@Override
	public AssistantResult analyze(AssistantContext context, TextEntity target) {
		String entityType = target.getProjectOrDomainEntityInterface().getSimpleName();
		EntityRef targetRef = EntityRef.of(entityType, target.getId());
		AssistantResult.Builder builder = AssistantResult.builder()
				.assistantId(ASSISTANT_ID)
				.runId(context.runId())
				.summary("Sentence-complexity analysis");
		analyzeProperty(builder, targetRef, PROP_NAME, target.getName());
		analyzeProperty(builder, targetRef, PROP_TEXT, target.getText());
		return builder.build();
	}

	private void analyzeProperty(AssistantResult.Builder builder, EntityRef targetRef,
			String propertyName, String text) {
		if (text == null || text.isBlank()) {
			return;
		}
		try {
			NLPText nlpText = nlpProcessorFactory.processText(text);
			NLPProcessor<Integer> depthFinder = nlpProcessorFactory.getConstituentTreeDepthFinder();
			collectComplexSentences(builder, targetRef, propertyName, nlpText, depthFinder);
		} catch (RuntimeException e) {
			log.warn("complexity analysis of the {} of {} failed; skipping: {}", propertyName,
					targetRef, e.toString());
		}
	}

	private void collectComplexSentences(AssistantResult.Builder builder, EntityRef targetRef,
			String propertyName, NLPText node, NLPProcessor<Integer> depthFinder) {
		if (node.is(GrammaticalStructureLevel.PARAGRAPH)) {
			for (NLPText sentence : node.getChildren()) {
				collectComplexSentences(builder, targetRef, propertyName, sentence, depthFinder);
			}
		} else if (node.is(GrammaticalStructureLevel.SENTENCE)) {
			Integer depth = depthFinder.process(node);
			if (depth != null && depth > COMPLEXITY_DEPTH_THRESHOLD) {
				emitComplexityIssue(builder, targetRef, propertyName, node);
			}
		}
	}

	private void emitComplexityIssue(AssistantResult.Builder builder, EntityRef targetRef,
			String propertyName, NLPText sentence) {
		String sentenceText = sentence.getText();
		// Sentences can be long; key the finding on a bounded hash of the sentence.
		String issueKey = ASSISTANT_ID + ":" + targetRef.entityType() + ":" + targetRef.entityId()
				+ ":complex-text:" + propertyName + ":" + Integer.toHexString(sentenceText.hashCode());
		List<EvidenceRef> evidence = List.of(EvidenceRef.ofLocator("property=" + propertyName),
				EvidenceRef.ofSnippet(sentenceText));

		Map<String, Object> issueMeta = Map.of(
				"kind", "LEXICAL",
				"annotatableEntityPropertyName", propertyName,
				"mustResolve", Boolean.TRUE,
				"findingType", "complex-text");
		builder.annotationAction(new AnnotationAction(issueKey,
				AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE, targetRef, null,
				MessageFormat.format(COMPLEX_TEXT_MSG, sentenceText, propertyName), null, null,
				evidence, issueMeta));

		builder.annotationAction(new AnnotationAction(issueKey + ":ignore",
				AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, issueKey,
				IGNORE_WORD_MSG, null, null, evidence, Map.of()));
	}
}
