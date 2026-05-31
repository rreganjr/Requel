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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rreganjr.nlp.dictionary.GrammaticalStructureLevel;
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
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.TextEntity;
import com.rreganjr.requel.project.exception.NoSuchActorException;
import com.rreganjr.requel.project.exception.NoSuchGlossaryTermException;

/**
 * SPI adapter reproducing the legacy {@code LexicalAssistant} glossary-term
 * discovery as {@link AnnotationAction}s. It extracts candidate noun phrases,
 * filters out clauses / unsuitable phrase types / possessives / sub-phrases, and
 * for each surviving phrase that is neither an existing glossary term nor an
 * existing actor it emits a lexical "potential glossary term" issue with three
 * resolve-positions: ignore, add-to-glossary, and add-as-actor.
 *
 * <p>
 * Deviations from the legacy code (issue #43, Phase 4.5 Step 4d):
 * <ul>
 * <li>The diagnostic NLP notes the legacy emits (constituent/dependency/semantic
 * printers and word-sense notes) are omitted — they are debug output, not
 * findings.</li>
 * <li>When a phrase matches an <em>existing</em> glossary term, the legacy adds
 * the analyzed entity as a referer to that term (a project edit). That path is
 * routed to Step 6 (it needs the project-edit action type), so it is skipped
 * here rather than emitted.</li>
 * </ul>
 */
@Component
public class LexicalGlossaryTermAssistant implements RequelAssistant<TextEntity> {

	private static final Logger log = LoggerFactory.getLogger(LexicalGlossaryTermAssistant.class);

	public static final String ASSISTANT_ID = "legacy-lexical-glossary-term";

	private static final String PROP_NAME = "Name";
	private static final String PROP_TEXT = "Text";

	private static final String TERM_ACTOR_DOMAIN_MSG =
			"The phrase \"{0}\" is a potential glossary term, actor, or domain object/property";
	private static final String IGNORE_PHRASE_MSG = "Ignore this phrase.";
	private static final String ADD_TO_GLOSSARY_MSG = "Add \"{0}\" to the project glossary.";
	private static final String ADD_AS_ACTOR_MSG = "Add \"{0}\" as an actor to the project.";

	private final NLPProcessorFactory nlpProcessorFactory;
	private final ProjectRepository projectRepository;

	@Autowired
	public LexicalGlossaryTermAssistant(NLPProcessorFactory nlpProcessorFactory,
			ProjectRepository projectRepository) {
		this.nlpProcessorFactory = nlpProcessorFactory;
		this.projectRepository = projectRepository;
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
		ProjectOrDomain projectOrDomain = target.getProjectOrDomain();
		AssistantResult.Builder builder = AssistantResult.builder()
				.assistantId(ASSISTANT_ID)
				.runId(context.runId())
				.summary("Glossary-term discovery");
		// A glossary issue is not property-specific; dedupe terms across Name/Text so
		// one run does not emit the same term twice.
		Set<String> emittedTerms = new HashSet<>();
		analyzeProperty(builder, targetRef, projectOrDomain, PROP_NAME, target.getName(),
				emittedTerms);
		analyzeProperty(builder, targetRef, projectOrDomain, PROP_TEXT, target.getText(),
				emittedTerms);
		return builder.build();
	}

	private void analyzeProperty(AssistantResult.Builder builder, EntityRef targetRef,
			ProjectOrDomain projectOrDomain, String propertyName, String text,
			Set<String> emittedTerms) {
		if (text == null || text.isBlank() || projectOrDomain == null) {
			return;
		}
		try {
			NLPText nlpText = nlpProcessorFactory.processText(text);
			for (NLPText term : findPotentialTerms(nlpText)) {
				handleTerm(builder, targetRef, projectOrDomain, term, emittedTerms);
			}
		} catch (RuntimeException e) {
			log.warn("glossary-term analysis of the {} of {} failed; skipping: {}", propertyName,
					targetRef, e.toString());
		}
	}

	private Set<NLPText> findPotentialTerms(NLPText nlpText) {
		Set<NLPText> potentialTerms = new HashSet<>();
		for (NLPText nounPhrase : nlpProcessorFactory.getNounPhraseFinder().process(nlpText)) {
			if (nounPhrase.getLeaves().size() == 1) {
				NLPText singleWord = nounPhrase.getLeaves().get(0);
				if (singleWord.is(ParseTag.NNP) || singleWord.is(ParseTag.NNPS)) {
					potentialTerms.add(singleWord);
				}
			} else {
				if (isAcceptableMultiWordPhrase(nounPhrase)) {
					potentialTerms.add(nounPhrase);
				}
			}
		}
		return filterSubPhrases(potentialTerms);
	}

	private static boolean isAcceptableMultiWordPhrase(NLPText nounPhrase) {
		boolean acceptable = true;
		List<NLPText> todo = new ArrayList<>();
		todo.add(nounPhrase);
		while (!todo.isEmpty()) {
			NLPText current = todo.remove(0);
			if (current.is(GrammaticalStructureLevel.CLAUSE)
					|| current.in(ParseTag.PP, ParseTag.VP, ParseTag.INTJ, ParseTag.LST,
							ParseTag.UCP, ParseTag.WHADJP, ParseTag.WHAVP, ParseTag.WHNP,
							ParseTag.WHPP)
					|| current.in(PartOfSpeech.NUMBER, PartOfSpeech.PUNCTUATION,
							PartOfSpeech.SYMBOL)) {
				return false;
			}
			todo.addAll(current.getChildren());
			for (NLPText word : current.getLeaves()) {
				if (word.is(ParseTag.PRP$)) {
					acceptable = false;
				}
			}
		}
		return acceptable;
	}

	private static Set<NLPText> filterSubPhrases(Set<NLPText> potentialTerms) {
		Set<NLPText> filtered = new HashSet<>(potentialTerms);
		for (NLPText outer : potentialTerms) {
			for (NLPText inner : potentialTerms) {
				if (outer.equals(inner)) {
					continue;
				}
				String outerText = outer.getText().toLowerCase(Locale.ROOT);
				String innerText = inner.getText().toLowerCase(Locale.ROOT);
				if (innerText.contains(outerText)) {
					filtered.remove(outer);
					break;
				}
				if (outerText.contains(innerText)) {
					filtered.remove(inner);
					break;
				}
			}
		}
		return filtered;
	}

	private void handleTerm(AssistantResult.Builder builder, EntityRef targetRef,
			ProjectOrDomain projectOrDomain, NLPText term, Set<String> emittedTerms) {
		String termText = term.getText();
		if (!emittedTerms.add(termText.toLowerCase(Locale.ROOT))) {
			return;
		}
		try {
			projectRepository.findGlossaryTermForProjectOrDomain(projectOrDomain, termText);
			// Term already exists: legacy adds the entity as a referer (project edit) — Step 6.
			return;
		} catch (NoSuchGlossaryTermException e) {
			// fall through to the actor check
		}

		Set<String> namesToMatch = new HashSet<>();
		namesToMatch.add(termText);
		if (!term.isLeaf() && term.getLeaves().get(0).in(PartOfSpeech.DETERMINER)) {
			namesToMatch.add(term.getTextRange(1));
		}
		for (String name : namesToMatch) {
			try {
				projectRepository.findActorByProjectOrDomainAndName(projectOrDomain, name);
				// Phrase already names an actor: do not raise a glossary issue.
				return;
			} catch (NoSuchActorException e) {
				// not an actor under this name; keep checking
			}
		}
		emitGlossaryIssue(builder, targetRef, termText);
	}

	private void emitGlossaryIssue(AssistantResult.Builder builder, EntityRef targetRef,
			String termText) {
		String issueKey = ASSISTANT_ID + ":" + targetRef.entityType() + ":" + targetRef.entityId()
				+ ":glossary-term:" + termText;
		List<EvidenceRef> evidence = List.of(EvidenceRef.ofSnippet(termText));

		Map<String, Object> issueMeta = Map.of(
				"kind", "LEXICAL",
				"word", termText,
				"mustResolve", Boolean.TRUE,
				"findingType", "glossary-term");
		builder.annotationAction(new AnnotationAction(issueKey,
				AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE, targetRef, null,
				MessageFormat.format(TERM_ACTOR_DOMAIN_MSG, termText), null, null, evidence,
				issueMeta));

		builder.annotationAction(new AnnotationAction(issueKey + ":ignore",
				AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, issueKey,
				IGNORE_PHRASE_MSG, null, null, evidence, Map.of()));
		builder.annotationAction(new AnnotationAction(issueKey + ":add-glossary",
				AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, issueKey,
				MessageFormat.format(ADD_TO_GLOSSARY_MSG, termText), null, null, evidence,
				Map.of("kind", "ADD_WORD_TO_GLOSSARY")));
		builder.annotationAction(new AnnotationAction(issueKey + ":add-actor",
				AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, issueKey,
				MessageFormat.format(ADD_AS_ACTOR_MSG, termText), null, null, evidence,
				Map.of("kind", "ADD_ACTOR_TO_PROJECT")));
	}
}
