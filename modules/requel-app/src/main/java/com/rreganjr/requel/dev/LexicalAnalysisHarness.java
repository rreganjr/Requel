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
package com.rreganjr.requel.dev;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.GrammaticalStructureLevel;
import com.rreganjr.nlp.dictionary.Linkdef;
import com.rreganjr.nlp.dictionary.NLPProcessor;
import com.rreganjr.nlp.dictionary.NLPProcessorFactory;
import com.rreganjr.nlp.dictionary.NLPText;
import com.rreganjr.nlp.dictionary.ParseTag;
import com.rreganjr.nlp.dictionary.PartOfSpeech;
import com.rreganjr.nlp.dictionary.Sense;
import com.rreganjr.nlp.dictionary.Synset;
import com.rreganjr.requel.assistant.legacynlp.LexicalEvidence;
import com.rreganjr.requel.assistant.legacynlp.LexicalGlossaryTermAssistant;
import com.rreganjr.requel.assistant.legacynlp.LexicalVagueWordAssistant;
import com.rreganjr.requel.assistant.legacynlp.ProjectVocabulary;
import com.rreganjr.requel.assistant.legacynlp.SpellingSkips;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.TextEntity;

/**
 * Issue #268, step 1: a read-only, dev-only report of what the lexical assistants see in one
 * project and what each candidate precision rule would do with it. It writes nothing: no
 * annotation, no finding, no run. The output is TSV in two sections, {@code # tokens} (one row
 * per word of every text entity's Name and Text, for the vague-word and spelling rules) and
 * {@code # glossary} (one row per glossary candidate, for the glossary rules). It is the data the
 * vague-word rule in {@code doc/work/2.0/268-lexical-precision-plan.md} is chosen from.
 *
 * <p>
 * Registered only when {@code requel.dev.lexical-harness.enabled=true} (set by the dev profile)
 * and NLP is on. The output quotes project text, so it goes to a gitignored file, never into the
 * repository.
 */
@ConditionalOnProperty(name = "requel.dev.lexical-harness.enabled", havingValue = "true")
@Component
public class LexicalAnalysisHarness {

	private static final Logger log = LoggerFactory.getLogger(LexicalAnalysisHarness.class);

	/** Entity types where a person noun most likely stands in for an actor role (rule C). */
	private static final Set<String> ROLE_CONTEXT_TYPES = Set.of("Story", "UseCase", "Scenario",
			"Step");

	private static final String TOKEN_HEADER = String.join("\t", "entity_type", "entity_id",
			"property", "token", "lemma", "pos", "parse_tag", "named_entity", "sentence_initial",
			"sense", "info_content", "today_vague", "ruleA_vague", "person_hyponym",
			"ruleC_finding", "ruleD_weak", "in_glossary_word", "in_actor_word", "spell_known",
			"spell_new_skip");

	private static final String GLOSSARY_HEADER = String.join("\t", "entity_type", "entity_id",
			"property", "raw_phrase", "raw_in_source", "normalized", "leading_determiner",
			"matches_term_raw", "matches_term_normalized", "matches_term_singular",
			"matches_actor", "det_after_first", "first_sentence_initial", "first_verb_form",
			"proper_noun", "entity_count", "verdict");

	private final ProjectRepository projectRepository;
	private final NLPProcessorFactory nlpProcessorFactory;
	private final DictionaryRepository dictionaryRepository;
	/** Absent when NLP is off ({@code requel.nlp.enabled=false}); the glossary section is then skipped. */
	private final ObjectProvider<LexicalGlossaryTermAssistant> glossaryTermAssistant;

	@Autowired
	public LexicalAnalysisHarness(ProjectRepository projectRepository,
			NLPProcessorFactory nlpProcessorFactory, DictionaryRepository dictionaryRepository,
			ObjectProvider<LexicalGlossaryTermAssistant> glossaryTermAssistant) {
		this.projectRepository = projectRepository;
		this.nlpProcessorFactory = nlpProcessorFactory;
		this.dictionaryRepository = dictionaryRepository;
		this.glossaryTermAssistant = glossaryTermAssistant;
	}

	/**
	 * @param projectName the project to report on
	 * @return the TSV report
	 * @throws com.rreganjr.requel.project.exception.NoSuchProjectException if there is no such
	 *             project
	 */
	@Transactional
	public String run(String projectName) {
		Project project = projectRepository.findProjectByName(projectName);
		Long projectId = project.getId();
		List<TextEntity> entities = textEntities(project);

		Set<String> termNames = new HashSet<>();
		Set<String> termWords = new HashSet<>();
		Set<String> actorNames = new HashSet<>();
		Set<String> actorWords = new HashSet<>();
		for (TextEntity entity : entities) {
			if (entity instanceof GlossaryTerm) {
				addVocabulary(entity.getName(), termNames, termWords);
			} else if (entity instanceof Actor) {
				addVocabulary(entity.getName(), actorNames, actorWords);
			}
		}
		List<String> corpus = entities.stream()
				.map(e -> (nullToEmpty(e.getName()) + "\n" + nullToEmpty(e.getText()))
						.toLowerCase(Locale.ROOT))
				.collect(Collectors.toList());

		Context ctx = new Context(projectId, termNames, termWords, actorNames, actorWords,
				corpus, ProjectVocabulary.of(termNames, actorNames));
		StringBuilder tokens = new StringBuilder("# tokens\n").append(TOKEN_HEADER).append('\n');
		StringBuilder glossary = new StringBuilder("# glossary\n").append(GLOSSARY_HEADER)
				.append('\n');
		for (TextEntity entity : entities) {
			String type = entity.getProjectOrDomainEntityInterface().getSimpleName();
			analyzeProperty(ctx, entity, type, "Name", entity.getName(), tokens, glossary);
			analyzeProperty(ctx, entity, type, "Text", entity.getText(), tokens, glossary);
		}
		return tokens.append('\n').append(glossary).toString();
	}

	private List<TextEntity> textEntities(Project project) {
		Map<String, TextEntity> byKey = new LinkedHashMap<>();
		List<ProjectOrDomainEntity> all = new ArrayList<>(project.getProjectEntities());
		all.addAll(projectRepository.findStepsByProjectOrDomain(project));
		for (ProjectOrDomainEntity entity : all) {
			// Report generators hold HTML/XSL templates, not requirements text; the assistants
			// don't analyze them.
			if (entity instanceof TextEntity text && !"ReportGenerator"
					.equals(entity.getProjectOrDomainEntityInterface().getSimpleName())) {
				byKey.putIfAbsent(entity.getProjectOrDomainEntityInterface().getSimpleName() + ":"
						+ entity.getId(), text);
			}
		}
		List<TextEntity> sorted = new ArrayList<>(byKey.values());
		sorted.sort((a, b) -> {
			int byType = a.getProjectOrDomainEntityInterface().getSimpleName()
					.compareTo(b.getProjectOrDomainEntityInterface().getSimpleName());
			return byType != 0 ? byType : a.getId().compareTo(b.getId());
		});
		return sorted;
	}

	private void analyzeProperty(Context ctx, TextEntity entity, String type, String property,
			String text, StringBuilder tokens, StringBuilder glossary) {
		if (text == null || text.isBlank()) {
			return;
		}
		String prefix = type + "\t" + entity.getId() + "\t" + property;
		NLPText nlpText;
		try {
			nlpText = nlpProcessorFactory.processText(text);
		} catch (RuntimeException e) {
			log.warn("harness: NLP failed on the {} of {}#{}: {}", property, type, entity.getId(),
					e.toString());
			tokens.append(prefix).append("\t#ERROR ").append(clean(e.toString())).append('\n');
			return;
		}
		Set<NLPText> sentenceInitial = sentenceInitialLeaves(nlpText);
		try {
			tokenRows(ctx, type, prefix, nlpText, sentenceInitial, tokens);
		} catch (RuntimeException e) {
			tokens.append(prefix).append("\t#ERROR ").append(clean(e.toString())).append('\n');
		}
		LexicalGlossaryTermAssistant glossaryAssistant = glossaryTermAssistant.getIfAvailable();
		if (glossaryAssistant != null && !(entity instanceof GlossaryTerm)) {
			try {
				glossaryRows(glossaryAssistant, ctx, prefix, text, nlpText, sentenceInitial,
						glossary);
			} catch (RuntimeException e) {
				glossary.append(prefix).append("\t#ERROR ").append(clean(e.toString()))
						.append('\n');
			}
		}
	}

	// ---- tokens: vague-word and spelling rules --------------------------------------------

	private void tokenRows(Context ctx, String type, String prefix, NLPText nlpText,
			Set<NLPText> sentenceInitial, StringBuilder out) {
		NLPProcessor<Boolean> spellChecker = nlpProcessorFactory.getSpellingChecker(ctx.projectId);
		Linkdef linkType = dictionaryRepository.findLinkDef(1L);
		Sense rootNounSense = safeSense("entity", PartOfSpeech.NOUN, 1);
		Sense personSense = safeSense("person", PartOfSpeech.NOUN, 1);
		Synset personSynset = personSense == null ? null : personSense.getSynset();

		for (NLPText word : nlpText.getLeaves()) {
			if (word.in(PartOfSpeech.PUNCTUATION, PartOfSpeech.NUMBER, PartOfSpeech.SYMBOL)
					|| word.in(ParseTag.POS, ParseTag.CD)) {
				continue;
			}
			String token = word.getText();
			String lower = token.toLowerCase(Locale.ROOT);
			Sense sense = word.getDictionaryWordSense();
			Synset synset = sense == null ? null : sense.getSynset();
			Double ic = null;
			if (synset != null && linkType != null) {
				try {
					ic = dictionaryRepository.infoContent(synset, linkType);
				} catch (RuntimeException e) {
					ic = null;
				}
			}
			boolean properRoot = sense != null && sense.equals(rootNounSense)
					&& word.in(ParseTag.NNP, ParseTag.NNPS);
			boolean todayVague = ic != null && !word.isNamedEntity() && !properRoot
					&& ic < LexicalVagueWordAssistant.INFO_CONTENT_THRESHOLD;
			boolean ruleA = todayVague
					&& word.in(PartOfSpeech.NOUN, PartOfSpeech.ADJECTIVE, PartOfSpeech.ADVERB);
			boolean personHyponym = synset != null && personSynset != null
					&& word.is(PartOfSpeech.NOUN) && isHyponymOf(synset, personSynset);
			String ruleC = !ruleA ? ""
					: (personHyponym && ROLE_CONTEXT_TYPES.contains(type) ? "person-not-role"
							: "vague");
			boolean ruleD = LexicalVagueWordAssistant.WEAK_WORDS.contains(lower);
			boolean inGlossaryWord = ctx.termWords.contains(lower);
			boolean inActorWord = ctx.actorWords.contains(lower);
			Boolean known = safeKnown(spellChecker, word);
			String spellSkip = Boolean.TRUE.equals(known) ? "" : spellSkipReason(ctx, token);

			out.append(prefix).append('\t').append(clean(token))
					.append('\t').append(clean(word.getLemma()))
					.append('\t').append(word.getPartOfSpeech())
					.append('\t').append(word.getParseTag())
					.append('\t').append(word.isNamedEntity())
					.append('\t').append(sentenceInitial.contains(word))
					.append('\t').append(sense == null ? "" : clean(sense.getSenseKey()))
					.append('\t').append(ic == null ? "" : String.format(Locale.ROOT, "%.3f", ic))
					.append('\t').append(todayVague)
					.append('\t').append(ruleA)
					.append('\t').append(personHyponym)
					.append('\t').append(ruleC)
					.append('\t').append(ruleD)
					.append('\t').append(inGlossaryWord)
					.append('\t').append(inActorWord)
					.append('\t').append(known == null ? "" : known)
					.append('\t').append(spellSkip)
					.append('\n');
		}
	}

	/** Why the #268 spelling rules skip an unknown token (shared with the assistant), or "". */
	private String spellSkipReason(Context ctx, String token) {
		String reason = SpellingSkips.reason(token, ctx.vocabulary,
				part -> isKnown(ctx.projectId, part));
		return reason == null ? "" : reason;
	}

	private boolean isKnown(Long projectId, String word) {
		try {
			return Boolean.TRUE.equals(dictionaryRepository.isKnownWord(projectId, word));
		} catch (RuntimeException e) {
			return false;
		}
	}

	private static Boolean safeKnown(NLPProcessor<Boolean> spellChecker, NLPText word) {
		try {
			return spellChecker.process(word);
		} catch (RuntimeException e) {
			return null;
		}
	}

	private Sense safeSense(String lemma, PartOfSpeech pos, int index) {
		try {
			return dictionaryRepository.findSense(lemma, pos, index);
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static boolean isHyponymOf(Synset synset, Synset ancestor) {
		Deque<Synset> todo = new ArrayDeque<>();
		Set<Long> seen = new HashSet<>();
		todo.add(synset);
		while (!todo.isEmpty() && seen.size() < 500) {
			Synset current = todo.poll();
			if (current.getId() == null || !seen.add(current.getId())) {
				continue;
			}
			if (current.getId().equals(ancestor.getId())) {
				return true;
			}
			todo.addAll(current.getHypernyms());
		}
		return false;
	}

	// ---- glossary candidates -----------------------------------------------------------------

	private void glossaryRows(LexicalGlossaryTermAssistant glossaryAssistant, Context ctx,
			String prefix, String sourceText, NLPText nlpText, Set<NLPText> sentenceInitial,
			StringBuilder out) {
		for (NLPText candidate : glossaryAssistant.findPotentialTerms(nlpText)) {
			List<NLPText> leaves = candidate.isLeaf() ? List.of(candidate) : candidate.getLeaves();
			if (leaves.isEmpty()) {
				continue;
			}
			String raw = candidate.getText();
			boolean rawInSource = LexicalEvidence.occurs(sourceText, raw);
			boolean leadingDeterminer = leaves.size() > 1
					&& leaves.get(0).in(PartOfSpeech.DETERMINER);
			List<NLPText> body = leadingDeterminer ? leaves.subList(1, leaves.size()) : leaves;
			String rebuilt = LexicalEvidence.locate(sourceText, body);
			String normalized = rebuilt != null ? rebuilt : joinTokens(body);
			String normLower = normalized.toLowerCase(Locale.ROOT);
			String singular = singularizeLastWord(normLower);

			boolean matchesTermRaw = ctx.termNames.contains(raw.toLowerCase(Locale.ROOT));
			boolean matchesTermNormalized = ctx.termNames.contains(normLower);
			boolean matchesTermSingular = ctx.termNames.contains(singular);
			boolean matchesActor = ctx.actorNames.contains(normLower)
					|| ctx.actorNames.contains(singular);
			boolean detAfterFirst = false;
			for (int i = 1; i < body.size(); i++) {
				if (body.get(i).in(PartOfSpeech.DETERMINER)) {
					detAfterFirst = true;
					break;
				}
			}
			NLPText first = leaves.get(0);
			boolean firstSentenceInitial = sentenceInitial.contains(first);
			boolean firstVerbForm = firstSentenceInitial && hasVerbSense(body.get(0));
			boolean properNoun = isProperNoun(body, sentenceInitial);
			int entityCount = countEntitiesContaining(ctx.corpus, normLower);

			String verdict;
			if (matchesTermNormalized || matchesTermSingular || matchesActor) {
				verdict = "existing";
			} else if (detAfterFirst) {
				verdict = "reject-vp-determiner";
			} else if (firstVerbForm && body.size() > 1) {
				verdict = "reject-vp-sentence-verb";
			} else if (entityCount >= 2 || properNoun) {
				verdict = "raise";
			} else {
				verdict = "below-threshold";
			}

			out.append(prefix).append('\t').append(clean(raw))
					.append('\t').append(rawInSource)
					.append('\t').append(clean(normalized))
					.append('\t').append(leadingDeterminer)
					.append('\t').append(matchesTermRaw)
					.append('\t').append(matchesTermNormalized)
					.append('\t').append(matchesTermSingular)
					.append('\t').append(matchesActor)
					.append('\t').append(detAfterFirst)
					.append('\t').append(firstSentenceInitial)
					.append('\t').append(firstVerbForm)
					.append('\t').append(properNoun)
					.append('\t').append(entityCount)
					.append('\t').append(verdict)
					.append('\n');
		}
	}

	private boolean hasVerbSense(NLPText word) {
		String lemma = word.getLemma() != null ? word.getLemma() : word.getText();
		for (String candidate : List.of(lemma.toLowerCase(Locale.ROOT),
				singularizeLastWord(word.getText().toLowerCase(Locale.ROOT)))) {
			try {
				if (dictionaryRepository.findWord(candidate, PartOfSpeech.VERB) != null) {
					return true;
				}
			} catch (RuntimeException e) {
				// not a verb under this spelling
			}
		}
		return false;
	}

	private static boolean isProperNoun(List<NLPText> body, Set<NLPText> sentenceInitial) {
		for (NLPText leaf : body) {
			if (!leaf.in(ParseTag.NNP, ParseTag.NNPS)) {
				return false;
			}
		}
		return body.size() > 1 || !sentenceInitial.contains(body.get(0));
	}

	private static int countEntitiesContaining(List<String> corpus, String phraseLower) {
		int count = 0;
		for (String text : corpus) {
			if (LexicalEvidence.occurs(text, phraseLower)) {
				count++;
			}
		}
		return count;
	}

	private static String joinTokens(List<NLPText> tokens) {
		return tokens.stream().map(NLPText::getText).collect(Collectors.joining(" "));
	}

	private static String singularizeLastWord(String phraseLower) {
		if (phraseLower.length() > 3 && phraseLower.endsWith("s") && !phraseLower.endsWith("ss")) {
			return phraseLower.substring(0, phraseLower.length() - 1);
		}
		return phraseLower;
	}

	// ---- shared --------------------------------------------------------------------------------

	/** The first non-punctuation leaf of each sentence, by identity. */
	private static Set<NLPText> sentenceInitialLeaves(NLPText nlpText) {
		Set<NLPText> initial = Collections.newSetFromMap(new IdentityHashMap<>());
		List<NLPText> sentences = nlpText.is(GrammaticalStructureLevel.PARAGRAPH)
				? nlpText.getChildren()
				: List.of(nlpText);
		for (NLPText sentence : sentences) {
			for (NLPText leaf : sentence.getLeaves()) {
				if (!leaf.in(PartOfSpeech.PUNCTUATION, PartOfSpeech.SYMBOL)) {
					initial.add(leaf);
					break;
				}
			}
		}
		return initial;
	}

	private static void addVocabulary(String name, Set<String> names, Set<String> words) {
		if (name == null || name.isBlank()) {
			return;
		}
		String lower = name.trim().toLowerCase(Locale.ROOT);
		names.add(lower);
		for (String word : lower.split("[\\s/]+")) {
			if (!word.isEmpty()) {
				words.add(word);
			}
		}
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	private static String clean(String s) {
		return s == null ? "" : s.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
	}

	private record Context(Long projectId, Set<String> termNames, Set<String> termWords,
			Set<String> actorNames, Set<String> actorWords, List<String> corpus,
			ProjectVocabulary vocabulary) {
	}
}
