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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.GrammaticalStructureLevel;
import com.rreganjr.nlp.dictionary.NLPProcessorFactory;
import com.rreganjr.nlp.dictionary.NLPText;
import com.rreganjr.nlp.dictionary.ParseTag;
import com.rreganjr.nlp.dictionary.PartOfSpeech;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.CleanupPolicy;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.EvidenceRef;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.TextEntity;

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
 * <li>When a phrase matches an <em>existing</em> glossary term, the legacy adds the
 * analyzed entity as a referer to that term (a project edit). This is emitted as an
 * {@code ADD_GLOSSARY_TERM_REFERER} action, which the applicator applies through
 * {@code EditGlossaryTermCommand.setAddReferers} (Phase 4.5 Step 6).</li>
 * <li>Issue #268: the candidates go through four passes (see {@code handleCandidate}), so an
 * existing term is matched without its determiner or plural, verb phrases, coordinations,
 * possessives and single common nouns are dropped, a common phrase needs three entities, and a
 * phrase raises one issue for the whole project.</li>
 * </ul>
 */
@ConditionalOnProperty(name = "requel.nlp.enabled", havingValue = "true", matchIfMissing = true)
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

	/** #268: a common multi-word phrase is raised once it appears in this many text entities. */
	static final int ENTITY_THRESHOLD = 3;

	/** #268: phrases that open with one of these are indefinite, not names ("anything else"). */
	static final Set<String> INDEFINITE_OPENERS = Set.of("anything", "something", "everything",
			"nothing", "each", "every", "any", "one");

	private final NLPProcessorFactory nlpProcessorFactory;
	private final ProjectRepository projectRepository;
	private final DictionaryRepository dictionaryRepository;

	@Autowired
	public LexicalGlossaryTermAssistant(NLPProcessorFactory nlpProcessorFactory,
			ProjectRepository projectRepository, DictionaryRepository dictionaryRepository) {
		this.nlpProcessorFactory = nlpProcessorFactory;
		this.projectRepository = projectRepository;
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
	 * Glossary-term findings become stale when the candidate phrase is removed or
	 * adopted (added as a glossary term/actor), so this assistant auto-resolves
	 * untouched findings a re-run no longer reports. Resolved issues are preserved.
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
		return "Glossary candidates";
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
		// A glossary term matches itself; the other lexical checks still cover glossary terms.
		if (projectOrDomain == null || target instanceof GlossaryTerm) {
			return builder.build();
		}
		ProjectScope scope = new ProjectScope(projectOrDomain);
		// A glossary issue is not property-specific; dedupe terms across Name/Text so
		// one run does not emit the same term twice.
		Set<String> emittedTerms = new HashSet<>();
		// #268: a failure on one property leaves the other's findings and marks the result
		// incomplete, so nothing is auto-resolved on the strength of it.
		PropertyChecks checks = new PropertyChecks(log, "glossary-term", targetRef);
		checks.run(PROP_NAME, target.getName(), () -> analyzeProperty(builder, targetRef, scope,
				PROP_NAME, target.getName(), emittedTerms));
		checks.run(PROP_TEXT, target.getText(), () -> analyzeProperty(builder, targetRef, scope,
				PROP_TEXT, target.getText(), emittedTerms));
		return LegacyLexicalIssues.withRemovals(checks.finish(builder).build(), target, targetRef,
				LegacyLexicalIssues.GLOSSARY_TERM, checks);
	}

	private void analyzeProperty(AssistantResult.Builder builder, EntityRef targetRef,
			ProjectScope scope, String propertyName, String text, Set<String> emittedTerms) {
		if (text == null || text.isBlank()) {
			return;
		}
		NLPText nlpText = nlpProcessorFactory.processText(text);
		Set<NLPText> sentenceStarts = sentenceStarts(nlpText);
		for (NLPText term : findPotentialTerms(nlpText)) {
			handleCandidate(builder, targetRef, scope, propertyName, text, term, sentenceStarts,
					emittedTerms);
		}
	}
	/**
	 * The candidate glossary phrases in {@code nlpText}, after the clause / phrase-type /
	 * possessive / sub-phrase filters. Public for the #268 dev harness
	 * ({@code LexicalAnalysisHarness}), which reports what each precision rule would do to them.
	 */
	public Set<NLPText> findPotentialTerms(NLPText nlpText) {
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

	/**
	 * Issue #268: the four passes a candidate goes through.
	 * <ol>
	 * <li><b>Already defined?</b> With a leading determiner dropped, and as written or with its
	 * last word singular, a glossary term name (synonyms are terms too) links the entity to the
	 * term; an actor name raises nothing.</li>
	 * <li><b>Not a term?</b> A verb phrase the parser read as a noun phrase, a coordination, a
	 * possessive, an indefinite opener, or a single common noun is dropped.</li>
	 * <li><b>Worth defining?</b> A proper noun or acronym is; a common multi-word phrase is once
	 * {@link #ENTITY_THRESHOLD} text entities in the project use it.</li>
	 * <li><b>One issue per project:</b> the issue action carries {@code scope=PROJECT} and a
	 * {@code shareKey}, so the applicator attaches every entity using the phrase to one issue,
	 * and ignoring it once ignores it project-wide.</li>
	 * </ol>
	 */
	private void handleCandidate(AssistantResult.Builder builder, EntityRef targetRef,
			ProjectScope scope, String propertyName, String source, NLPText term,
			Set<NLPText> sentenceStarts, Set<String> emittedTerms) {
		List<NLPText> leaves = term.isLeaf() ? List.of(term) : term.getLeaves();
		if (leaves.isEmpty()) {
			return;
		}
		// #268/#269: quote the phrase as the author wrote it ("force-stops", not
		// "force - stops"), and drop a phrase that isn't in the text at all.
		if (LexicalEvidence.locate(source, leaves) == null) {
			log.warn("glossary-term: dropping \"{}\" on the {} of {}: not in the source text",
					term.getText(), propertyName, targetRef);
			return;
		}
		List<NLPText> body = leaves.size() > 1 && leaves.get(0).in(PartOfSpeech.DETERMINER)
				? leaves.subList(1, leaves.size())
				: leaves;
		String located = LexicalEvidence.locate(source, body);
		if (located == null) {
			return;
		}
		// As written, but on one line: a phrase can span a line break in the source.
		String phrase = located.replaceAll("\\s+", " ");
		String key = phrase.toLowerCase(Locale.ROOT);
		if (!emittedTerms.add(key)) {
			return;
		}

		// 1. already defined
		GlossaryTerm existing = scope.term(key);
		if (existing == null) {
			existing = scope.term(singular(key));
		}
		if (existing != null) {
			emitGlossaryTermReferer(builder, targetRef, existing, phrase);
			return;
		}
		if (scope.vocabulary.isActorName(key) || scope.vocabulary.isActorName(singular(key))) {
			return;
		}

		// 2. not a term
		if (isNotATerm(body, source, sentenceStarts)) {
			return;
		}

		// 3. worth defining
		boolean properNoun = isProperNoun(body, sentenceStarts);
		if (body.size() == 1) {
			if (!properNoun && !SpellingSkips.ACRONYM.matcher(phrase).matches()) {
				return;
			}
		} else if (!properNoun && scope.entitiesUsing(key) < ENTITY_THRESHOLD) {
			return;
		}

		// 4. one issue per project
		emitGlossaryIssue(builder, targetRef, phrase, key);
	}

	/** Pass 2: shapes that are never glossary terms, whatever their frequency. */
	private boolean isNotATerm(List<NLPText> body, String source, Set<NLPText> sentenceStarts) {
		NLPText first = body.get(0);
		String firstLower = lower(first.getText());
		if (INDEFINITE_OPENERS.contains(firstLower)) {
			return true;
		}
		for (int i = 0; i < body.size(); i++) {
			NLPText leaf = body.get(i);
			String text = lower(leaf.getText());
			// a determiner after the first word: a verb phrase or clause ("archives the room")
			if (i > 0 && leaf.in(PartOfSpeech.DETERMINER)) {
				return true;
			}
			// two terms, not one ("stream key and RTMP endpoint")
			if ("and".equals(text) || "or".equals(text) || leaf.is(ParseTag.CC)) {
				return true;
			}
			// a possessive ("CON-3685's manual")
			if ("'s".equals(text) || "’s".equals(text) || leaf.is(ParseTag.POS)) {
				return true;
			}
		}
		// a sentence-initial third-person verb read as a noun ("Alarms route", "Spans room ...")
		if (body.size() > 1 && sentenceStarts.contains(first) && firstLower.length() > 3
				&& firstLower.endsWith("s") && !firstLower.endsWith("ss")
				&& hasVerbSense(firstLower.substring(0, firstLower.length() - 1))) {
			return true;
		}
		// the letters of a ticket key ("CON" in "CON-3685")
		if (body.size() == 1 && Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(first.getText())
				+ "-\\d").matcher(source).find()) {
			return true;
		}
		return false;
	}

	/**
	 * All proper-noun words; a single one only when it doesn't start its sentence, where
	 * capitalisation says nothing ("Archive the room").
	 */
	private static boolean isProperNoun(List<NLPText> body, Set<NLPText> sentenceStarts) {
		for (NLPText leaf : body) {
			if (!leaf.in(ParseTag.NNP, ParseTag.NNPS)) {
				return false;
			}
		}
		return body.size() > 1 || !sentenceStarts.contains(body.get(0));
	}

	private boolean hasVerbSense(String lemma) {
		if (dictionaryRepository == null) {
			return false;
		}
		try {
			return dictionaryRepository.findWord(lemma, PartOfSpeech.VERB) != null;
		} catch (RuntimeException e) {
			return false;
		}
	}

	/** The phrase with its last word made singular: "rooms" to "room", "entities" to "entity". */
	static String singular(String phraseLower) {
		if (phraseLower.endsWith("ies") && phraseLower.length() > 4) {
			return phraseLower.substring(0, phraseLower.length() - 3) + "y";
		}
		if (phraseLower.endsWith("s") && !phraseLower.endsWith("ss") && !phraseLower.endsWith("us")
				&& phraseLower.length() > 3) {
			return phraseLower.substring(0, phraseLower.length() - 1);
		}
		return phraseLower;
	}

	private static String lower(String s) {
		return s == null ? "" : s.toLowerCase(Locale.ROOT);
	}

	/** The first word of each sentence (punctuation skipped), by identity. */
	static Set<NLPText> sentenceStarts(NLPText nlpText) {
		Set<NLPText> starts = Collections.newSetFromMap(new IdentityHashMap<>());
		List<NLPText> sentences = nlpText.is(GrammaticalStructureLevel.PARAGRAPH)
				? nlpText.getChildren()
				: List.of(nlpText);
		for (NLPText sentence : sentences) {
			for (NLPText leaf : sentence.getLeaves()) {
				if (!leaf.in(PartOfSpeech.PUNCTUATION, PartOfSpeech.SYMBOL)) {
					starts.add(leaf);
					break;
				}
			}
		}
		return starts;
	}

	/**
	 * What one run needs from the project: its vocabulary, its terms by lower-cased name, and,
	 * only when a common phrase needs counting, the lower-cased text of its text entities.
	 */
	private final class ProjectScope {
		private final ProjectOrDomain projectOrDomain;
		private final ProjectVocabulary vocabulary;
		private final Map<String, GlossaryTerm> termsByName = new HashMap<>();
		private List<String> corpus;

		ProjectScope(ProjectOrDomain projectOrDomain) {
			this.projectOrDomain = projectOrDomain;
			this.vocabulary = ProjectVocabulary.of(projectOrDomain);
			if (projectOrDomain.getGlossaryTerms() != null) {
				for (GlossaryTerm glossaryTerm : projectOrDomain.getGlossaryTerms()) {
					if (glossaryTerm.getName() != null) {
						termsByName.putIfAbsent(
								glossaryTerm.getName().trim().replaceAll("\\s+", " ")
										.toLowerCase(Locale.ROOT),
								glossaryTerm);
					}
				}
			}
		}

		GlossaryTerm term(String nameLower) {
			return termsByName.get(nameLower);
		}

		/** How many of the project's text entities (report generators aside) use the phrase. */
		int entitiesUsing(String phraseLower) {
			if (corpus == null) {
				corpus = new ArrayList<>();
				Map<String, TextEntity> byKey = new HashMap<>();
				List<ProjectOrDomainEntity> all = new ArrayList<>();
				if (projectOrDomain.getProjectEntities() != null) {
					all.addAll(projectOrDomain.getProjectEntities());
				}
				all.addAll(projectRepository.findStepsByProjectOrDomain(projectOrDomain));
				for (ProjectOrDomainEntity entity : all) {
					String type = entity.getProjectOrDomainEntityInterface().getSimpleName();
					if (entity instanceof TextEntity text && !"ReportGenerator".equals(type)) {
						byKey.putIfAbsent(type + ":" + entity.getId(), text);
					}
				}
				for (TextEntity text : byKey.values()) {
					corpus.add((text.getName() == null ? "" : text.getName()) + "\n"
							+ (text.getText() == null ? "" : text.getText()));
				}
			}
			int count = 0;
			for (String text : corpus) {
				if (LexicalEvidence.occurs(text, phraseLower)) {
					count++;
				}
			}
			return count;
		}
	}
	/**
	 * Emit a project-edit action linking the analyzed entity ({@code targetRef}) as a referer
	 * to an existing glossary term. Idempotent at the domain level (the referer set is a
	 * {@code Set}), so the applicator runs it without recording a finding.
	 */
	private void emitGlossaryTermReferer(AssistantResult.Builder builder, EntityRef targetRef,
			GlossaryTerm existingTerm, String termText) {
		String actionKey = ASSISTANT_ID + ":" + targetRef.entityType() + ":" + targetRef.entityId()
				+ ":glossary-referer:" + existingTerm.getId();
		builder.annotationAction(new AnnotationAction(actionKey,
				AnnotationAction.ActionType.ADD_GLOSSARY_TERM_REFERER, targetRef, null, null, null,
				null, List.of(EvidenceRef.ofSnippet(termText)),
				Map.of("glossaryTermType", "GlossaryTerm", "glossaryTermId",
						existingTerm.getId())));
	}

	private void emitGlossaryIssue(AssistantResult.Builder builder, EntityRef targetRef,
			String termText, String key) {
		String issueKey = ASSISTANT_ID + ":" + targetRef.entityType() + ":" + targetRef.entityId()
				+ ":glossary-term:" + termText;
		List<EvidenceRef> evidence = List.of(EvidenceRef.ofSnippet(termText));

		// #268: one issue per phrase per project. The applicator attaches this entity to an open
		// issue that already carries the shareKey, and treats an ignore of any entity's finding
		// with the same key as an ignore for the whole project.
		Map<String, Object> issueMeta = Map.of(
				"kind", "LEXICAL",
				"word", termText,
				"mustResolve", Boolean.FALSE,
				"findingType", "glossary-term",
				"scope", "PROJECT",
				"shareKey", "glossary-term:" + key);
		builder.annotationAction(new AnnotationAction(issueKey,
				AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE, targetRef, null,
				MessageFormat.format(TERM_ACTOR_DOMAIN_MSG, termText), null, null, evidence,
				issueMeta));

		builder.annotationAction(new AnnotationAction(issueKey + ":ignore",
				AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, issueKey,
				IGNORE_PHRASE_MSG, null, null, evidence, Map.of("kind", "IGNORE")));
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
