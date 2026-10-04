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
package com.rreganjr.requel.assistant.core.corpus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opennlp.tools.stemmer.PorterStemmer;

import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.core.context.TextSimilarity;
import com.rreganjr.requel.assistant.core.context.TextSimilarity.Token;

/**
 * Issue #266, stage 1: finds the pairs in a set of entities worth asking about, without a model.
 *
 * <ul>
 * <li><b>Score.</b> TF-IDF cosine over the set ({@link TextSimilarity} tokens: stemmed, stop words
 * removed, glossary phrases as one token). A word also counts, at {@code synonymWeight}, as each of
 * its dictionary synonyms that occurs somewhere else in the set, so "buy" and "purchase" meet
 * without the noise of every synonym of every word.</li>
 * <li><b>Hints</b> on pairs scoring at least the conflict threshold: an antonym about the same
 * thing ({@code data stays private} / {@code data is public}: the two words share a word within
 * {@link #ANTONYM_WINDOW} words), opposite polarity on a shared verb ({@code must send} /
 * {@code must not send}), and a different number with the same unit ({@code 14 days} /
 * {@code 21 days}).</li>
 * <li><b>Kind.</b> {@code CONFLICT} when the score reaches the conflict threshold and there is a
 * hint, otherwise {@code OVERLAP} when it reaches the overlap threshold and the two can repeat each
 * other: see {@link #comparable}. Linked members (see {@link CorpusMembers}) are never an
 * overlap.</li>
 * </ul>
 *
 * Deterministic: the same members give the same pairs in the same order.
 */
public class CorpusCandidateFinder {

	/** One entity in the set: its reference, name and text. */
	public record Member(EntityRef ref, String name, String text) {

		public Member {
			Objects.requireNonNull(ref, "ref");
		}

		String combined() {
			String n = name == null ? "" : name.strip();
			String t = text == null ? "" : text.strip();
			return n.isEmpty() ? t : t.isEmpty() ? n : n + ". " + t;
		}
	}

	/** What a hint found. */
	public enum HintType {
		ANTONYM, NEGATION, NUMBER
	}

	/** A conflict hint: what one member says and what the other says. */
	public record Hint(HintType type, String inA, String inB) {

		public String describe() {
			return "'" + inA + "' / '" + inB + "'";
		}
	}

	/** Why a pair is a candidate. */
	public enum Kind {
		OVERLAP, CONFLICT
	}

	/**
	 * A scored pair. {@code kind} is null when the pair is below both thresholds (the harness
	 * reports those too).
	 */
	public record ScoredPair(EntityRef a, EntityRef b, double score, double plainScore,
			List<Hint> hints, Kind kind, boolean linked) {

		public boolean isCandidate() {
			return kind != null;
		}
	}

	/**
	 * The thresholds and the synonym weight. The defaults are the #266 checkpoint's, from the eval
	 * fixture and the roundtable project: see doc/work/2.0/266-corpus-analyses-plan.md.
	 */
	public record Settings(double overlapThreshold, double conflictThreshold,
			double synonymWeight) {

		public static final Settings DEFAULTS = new Settings(0.35, 0.20, 0.5);

		public Settings {
			if (overlapThreshold <= 0 || conflictThreshold <= 0 || synonymWeight < 0) {
				throw new IllegalArgumentException("thresholds must be positive and the synonym"
						+ " weight non-negative");
			}
		}
	}

	private static final Map<String, String> NUMBER_WORDS = Map.ofEntries(Map.entry("one", "1"),
			Map.entry("two", "2"), Map.entry("three", "3"), Map.entry("four", "4"),
			Map.entry("five", "5"), Map.entry("six", "6"), Map.entry("seven", "7"),
			Map.entry("eight", "8"), Map.entry("nine", "9"), Map.entry("ten", "10"),
			Map.entry("eleven", "11"), Map.entry("twelve", "12"), Map.entry("twenty", "20"),
			Map.entry("thirty", "30"), Map.entry("once", "1"), Map.entry("twice", "2"));

	private static final Pattern NUMBER = Pattern.compile("(?<![\\p{L}\\p{Nd}-])(\\d+(?:[.,]\\d+)?"
			+ "|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|twenty|thirty)"
			+ "\\s*(%|[\\p{L}]+(?:\\s+[\\p{L}]+)?)");

	private static final Pattern MODAL = Pattern.compile("\\b(must|shall|should|will|can|may|might"
			+ "|could|would|do|does|did|is|are|was|were)\\s+(not\\s+|never\\s+)?(?:be\\s+)?"
			+ "(\\p{L}+)");

	private static final Pattern CONTRACTION = Pattern.compile("\\b(cannot|can't|won't|don't"
			+ "|doesn't|didn't|isn't|aren't|wasn't|weren't|shouldn't|mustn't|never)\\s+(?:be\\s+)?"
			+ "(\\p{L}+)");

	private static final Pattern ALWAYS = Pattern.compile("\\balways\\s+(\\p{L}+)");

	private static final Set<String> NOT_VERBS = Set.of("be", "not", "never", "also", "only",
			"always", "able", "the", "a", "an", "to", "in", "on", "at", "of", "for", "by");

	/**
	 * Type pairs that can repeat each other, so their overlap is worth raising. Tuning on a real
	 * project found the rest are mentions: an actor or a glossary term named in a step, a step that
	 * performs a use case. Steps are compared as part of their scenario, never alone.
	 */
	private static final Set<String> COMPARABLE = Set.of("Goal|Goal", "Story|Story",
			"UseCase|UseCase", "Scenario|Scenario", "Actor|Actor", "GlossaryTerm|GlossaryTerm",
			"Actor|GlossaryTerm", "Goal|Story", "Goal|UseCase", "Story|UseCase");

	/** Whether entities of these two types can repeat each other. */
	public static boolean comparable(String typeA, String typeB) {
		return COMPARABLE.contains(typeA + "|" + typeB) || COMPARABLE.contains(typeB + "|" + typeA);
	}

	private final WordRelations relations;
	private final Settings settings;
	private final Map<String, Set<String>> synonymCache = new HashMap<>();
	private final Map<String, Set<String>> antonymCache = new HashMap<>();

	public CorpusCandidateFinder(WordRelations relations, Settings settings) {
		this.relations = relations == null ? WordRelations.NONE : relations;
		this.settings = Objects.requireNonNull(settings, "settings");
	}

	public Settings settings() {
		return settings;
	}

	/** The candidate pairs of {@code members}, highest score first. */
	public List<ScoredPair> find(List<Member> members, Map<String, String> glossaryPhrases) {
		return find(members, glossaryPhrases, (a, b) -> false);
	}

	/** Whether two members are structurally linked (see {@link CorpusMembers}). */
	@FunctionalInterface
	public interface Links {
		boolean linked(EntityRef a, EntityRef b);
	}

	/** The candidate pairs of a set, highest score first. */
	public List<ScoredPair> find(CorpusMembers.CorpusSet set) {
		return find(set.members(), set.glossaryTokens(), set::linked);
	}

	/**
	 * The candidate pairs of {@code members}, highest score first. A linked pair is only a
	 * candidate as a conflict.
	 */
	public List<ScoredPair> find(List<Member> members, Map<String, String> glossaryPhrases,
			Links links) {
		List<ScoredPair> candidates = new ArrayList<>();
		for (ScoredPair pair : scoreAll(members, glossaryPhrases, links)) {
			if (pair.isCandidate()) {
				candidates.add(pair);
			}
		}
		return candidates;
	}

	/**
	 * Every pair with a score above zero, highest score first, each with its hints and kind
	 * (null below both thresholds). Hints are only worked out at or above the conflict threshold.
	 */
	public List<ScoredPair> scoreAll(List<Member> members, Map<String, String> glossaryPhrases) {
		return scoreAll(members, glossaryPhrases, (a, b) -> false);
	}

	/** {@link #scoreAll(List, Map)} over a set, with its links. */
	public List<ScoredPair> scoreAll(CorpusMembers.CorpusSet set) {
		return scoreAll(set.members(), set.glossaryTokens(), set::linked);
	}

	/** {@link #scoreAll(List, Map)}, where a linked pair is never an overlap. */
	public List<ScoredPair> scoreAll(List<Member> members, Map<String, String> glossaryPhrases,
			Links links) {
		TextSimilarity similarity = new TextSimilarity(
				glossaryPhrases == null ? Map.of() : glossaryPhrases);
		PorterStemmer stemmer = new PorterStemmer();
		int n = members.size();
		List<List<Token>> tokens = new ArrayList<>(n);
		Set<String> setTerms = new HashSet<>();
		for (Member member : members) {
			List<Token> memberTokens = similarity.tokens(member.combined(), stemmer);
			tokens.add(memberTokens);
			for (Token token : memberTokens) {
				setTerms.add(token.term());
			}
		}
		List<Map<String, Double>> plain = new ArrayList<>(n);
		List<Map<String, Double>> expanded = new ArrayList<>(n);
		for (List<Token> memberTokens : tokens) {
			Map<String, Double> counts = new HashMap<>();
			for (Token token : memberTokens) {
				counts.merge(token.term(), 1.0, Double::sum);
			}
			plain.add(counts);
			expanded.add(expand(counts, memberTokens, setTerms, stemmer));
		}
		List<Map<String, Double>> plainVectors = weigh(plain);
		List<Map<String, Double>> expandedVectors = weigh(expanded);

		// inverted index over the expanded vectors: only pairs sharing a term can score
		Map<String, List<Integer>> postings = new HashMap<>();
		for (int i = 0; i < n; i++) {
			for (String term : expandedVectors.get(i).keySet()) {
				postings.computeIfAbsent(term, k -> new ArrayList<>()).add(i);
			}
		}
		Set<Long> seen = new HashSet<>();
		List<ScoredPair> pairs = new ArrayList<>();
		List<Facts> facts = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			facts.add(null);
		}
		for (List<Integer> posting : postings.values()) {
			for (int x = 0; x < posting.size(); x++) {
				for (int y = x + 1; y < posting.size(); y++) {
					// a is the member with the lower key, so a pair reads the same whatever the input order
					int i = posting.get(x);
					int j = posting.get(y);
					if (key(members.get(i).ref()).compareTo(key(members.get(j).ref())) > 0) {
						int swap = i;
						i = j;
						j = swap;
					}
					if (!seen.add((long) i * n + j)) {
						continue;
					}
					double score = cosine(expandedVectors.get(i), expandedVectors.get(j));
					if (score <= 0.0) {
						continue;
					}
					double plainScore = cosine(plainVectors.get(i), plainVectors.get(j));
					List<Hint> hints = List.of();
					if (score >= settings.conflictThreshold()) {
						hints = hints(facts(facts, i, members, tokens, stemmer),
								facts(facts, j, members, tokens, stemmer), stemmer);
					}
					boolean linked = links.linked(members.get(i).ref(), members.get(j).ref());
					Kind kind = !hints.isEmpty() ? Kind.CONFLICT
							: score >= settings.overlapThreshold() && !linked
									&& comparable(members.get(i).ref().entityType(),
											members.get(j).ref().entityType()) ? Kind.OVERLAP
													: null;
					pairs.add(new ScoredPair(members.get(i).ref(), members.get(j).ref(),
							round(score), round(plainScore), hints, kind, linked));
				}
			}
		}
		pairs.sort(Comparator.comparingDouble(ScoredPair::score).reversed()
				.thenComparing(p -> key(p.a())).thenComparing(p -> key(p.b())));
		return pairs;
	}

	private Map<String, Double> expand(Map<String, Double> counts, List<Token> memberTokens,
			Set<String> setTerms, PorterStemmer stemmer) {
		Map<String, Double> result = new HashMap<>(counts);
		if (settings.synonymWeight() == 0.0) {
			return result;
		}
		Set<String> surfaces = new LinkedHashSet<>();
		for (Token token : memberTokens) {
			if (!token.isGlossary() && isWord(token.surface())) {
				surfaces.add(token.surface());
			}
		}
		for (String surface : surfaces) {
			String own = stemmer.stem(surface);
			Set<String> added = new HashSet<>();
			for (String synonym : synonyms(surface)) {
				String term = stemmer.stem(synonym);
				if (!term.equals(own) && setTerms.contains(term) && !counts.containsKey(term)
						&& added.add(term)) {
					result.merge(term, settings.synonymWeight(), Double::sum);
				}
			}
		}
		return result;
	}

	/** What one member says that a hint can compare. */
	private record Facts(Set<String> words, Set<String> stems, List<String> sequence,
			Map<String, Boolean> polarity, Map<String, String> polarityText,
			Map<String, Set<String>> numbersByUnit, Map<String, String> numberText) {
	}

	/** Words either side of an antonym that must include a shared one: the same subject. */
	static final int ANTONYM_WINDOW = 3;

	private Facts facts(List<Facts> cache, int index, List<Member> members,
			List<List<Token>> tokens, PorterStemmer stemmer) {
		Facts existing = cache.get(index);
		if (existing != null) {
			return existing;
		}
		String text = members.get(index).combined().toLowerCase(Locale.ROOT);
		Set<String> words = new LinkedHashSet<>();
		Set<String> stems = new HashSet<>();
		List<String> sequence = new ArrayList<>();
		for (Token token : tokens.get(index)) {
			if (!token.isGlossary() && isWord(token.surface())) {
				words.add(token.surface());
				stems.add(token.term());
				sequence.add(token.term());
			}
		}
		Map<String, Boolean> polarity = new LinkedHashMap<>();
		Map<String, String> polarityText = new HashMap<>();
		Matcher modal = MODAL.matcher(text);
		while (modal.find()) {
			polar(polarity, polarityText, modal.group(3), modal.group(2) != null, modal.group(),
					stemmer);
		}
		Matcher contraction = CONTRACTION.matcher(text);
		while (contraction.find()) {
			polar(polarity, polarityText, contraction.group(2), true, contraction.group(),
					stemmer);
		}
		Matcher always = ALWAYS.matcher(text);
		while (always.find()) {
			polar(polarity, polarityText, always.group(1), false, always.group(), stemmer);
		}
		Map<String, Set<String>> numbers = new LinkedHashMap<>();
		Map<String, String> numberText = new HashMap<>();
		Matcher number = NUMBER.matcher(text);
		while (number.find()) {
			String value = NUMBER_WORDS.getOrDefault(number.group(1), number.group(1).replace(',', '.'));
			for (Map.Entry<String, String> unit : units(number.group(2), stemmer).entrySet()) {
				numbers.computeIfAbsent(unit.getKey(), k -> new LinkedHashSet<>()).add(value);
				numberText.putIfAbsent(unit.getKey() + "#" + value,
						number.group(1) + ("%".equals(unit.getValue()) ? "" : " ") + unit.getValue());
			}
		}
		Facts facts = new Facts(words, stems, sequence, polarity, polarityText, numbers,
				numberText);
		cache.set(index, facts);
		return facts;
	}

	private static void polar(Map<String, Boolean> polarity, Map<String, String> text, String verb,
			boolean negated, String phrase, PorterStemmer stemmer) {
		if (NOT_VERBS.contains(verb) || TextSimilarity.isStopWord(verb)) {
			return;
		}
		String stem = stemmer.stem(verb);
		if (!polarity.containsKey(stem)) {
			polarity.put(stem, negated);
		} else if (polarity.get(stem) != null && polarity.get(stem) != negated) {
			// says both: no single polarity to compare
			polarity.put(stem, null);
		}
		text.putIfAbsent(stem + "#" + negated, phrase.strip());
	}

	/** The unit stems after a number, each to the word as written. */
	private static Map<String, String> units(String following, PorterStemmer stemmer) {
		Map<String, String> units = new LinkedHashMap<>();
		if ("%".equals(following)) {
			units.put("percent", "%");
			return units;
		}
		for (String word : following.split("\\s+")) {
			if (word.equals("percent")) {
				units.putIfAbsent("percent", word);
			} else if (!TextSimilarity.isStopWord(word)) {
				units.putIfAbsent(stemmer.stem(word), word);
			}
		}
		return units;
	}

	private List<Hint> hints(Facts a, Facts b, PorterStemmer stemmer) {
		List<Hint> hints = new ArrayList<>();
		// antonyms, either way round, once per word pair
		Set<String> reported = new HashSet<>();
		antonymHints(a, b, stemmer, hints, reported, false);
		antonymHints(b, a, stemmer, hints, reported, true);
		// polarity on a shared verb
		for (Map.Entry<String, Boolean> entry : a.polarity().entrySet()) {
			Boolean other = b.polarity().get(entry.getKey());
			if (entry.getValue() != null && other != null && !other.equals(entry.getValue())) {
				hints.add(new Hint(HintType.NEGATION,
						a.polarityText().get(entry.getKey() + "#" + entry.getValue()),
						b.polarityText().get(entry.getKey() + "#" + other)));
			}
		}
		// a different number with the same unit, once per pair of numbers (the first unit wins)
		Set<String> numberPairs = new HashSet<>();
		for (Map.Entry<String, Set<String>> entry : a.numbersByUnit().entrySet()) {
			Set<String> other = b.numbersByUnit().get(entry.getKey());
			if (other == null || other.equals(entry.getValue())) {
				continue;
			}
			for (String valueA : entry.getValue()) {
				if (other.contains(valueA)) {
					continue;
				}
				for (String valueB : other) {
					if (!entry.getValue().contains(valueB) && numberPairs.add(valueA + "|" + valueB)) {
						hints.add(new Hint(HintType.NUMBER,
								a.numberText().get(entry.getKey() + "#" + valueA),
								b.numberText().get(entry.getKey() + "#" + valueB)));
						break;
					}
				}
				break;
			}
		}
		return List.copyOf(new LinkedHashSet<>(hints));
	}

	private void antonymHints(Facts from, Facts to, PorterStemmer stemmer, List<Hint> hints,
			Set<String> reported, boolean swapped) {
		for (String word : from.words()) {
			for (String antonym : antonyms(word)) {
				String term = stemmer.stem(antonym);
				if (!to.stems().contains(term)) {
					continue;
				}
				// the two words must be about the same thing: a word they share nearby
				String own = stemmer.stem(word);
				Set<String> near = neighbours(from.sequence(), own);
				near.retainAll(neighbours(to.sequence(), term));
				near.remove(own);
				near.remove(term);
				if (near.isEmpty()) {
					continue;
				}
				String found = surfaceFor(to, term, stemmer);
				String a = swapped ? found : word;
				String b = swapped ? word : found;
				if (reported.add(stemmer.stem(a) + "|" + stemmer.stem(b))) {
					hints.add(new Hint(HintType.ANTONYM, a, b));
				}
			}
		}
	}

	/** The stems within {@link #ANTONYM_WINDOW} words of any occurrence of {@code stem}. */
	private static Set<String> neighbours(List<String> sequence, String stem) {
		Set<String> near = new HashSet<>();
		for (int i = 0; i < sequence.size(); i++) {
			if (!sequence.get(i).equals(stem)) {
				continue;
			}
			for (int j = Math.max(0, i - ANTONYM_WINDOW);
					j <= Math.min(sequence.size() - 1, i + ANTONYM_WINDOW); j++) {
				if (j != i) {
					near.add(sequence.get(j));
				}
			}
		}
		return near;
	}

	private static String surfaceFor(Facts facts, String term, PorterStemmer stemmer) {
		for (String word : facts.words()) {
			if (stemmer.stem(word).equals(term)) {
				return word;
			}
		}
		return term;
	}

	private Set<String> synonyms(String word) {
		return synonymCache.computeIfAbsent(word, w -> singleWords(relations.synonyms(w)));
	}

	private Set<String> antonyms(String word) {
		return antonymCache.computeIfAbsent(word, w -> singleWords(relations.antonyms(w)));
	}

	private static Set<String> singleWords(Collection<String> lemmas) {
		Set<String> result = new LinkedHashSet<>();
		if (lemmas == null) {
			return result;
		}
		for (String lemma : lemmas) {
			if (lemma != null && isWord(lemma.toLowerCase(Locale.ROOT))) {
				result.add(lemma.toLowerCase(Locale.ROOT));
			}
		}
		return result;
	}

	private static boolean isWord(String word) {
		if (word.length() < 2) {
			return false;
		}
		for (int i = 0; i < word.length(); i++) {
			if (!Character.isLetter(word.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private static List<Map<String, Double>> weigh(List<Map<String, Double>> counts) {
		Map<String, Integer> frequency = new HashMap<>();
		for (Map<String, Double> doc : counts) {
			for (String term : doc.keySet()) {
				frequency.merge(term, 1, Integer::sum);
			}
		}
		int corpus = counts.size();
		List<Map<String, Double>> vectors = new ArrayList<>(counts.size());
		for (Map<String, Double> doc : counts) {
			Map<String, Double> vector = new HashMap<>();
			for (Map.Entry<String, Double> term : doc.entrySet()) {
				double idf = Math.log((corpus + 1.0) / (frequency.get(term.getKey()) + 1.0)) + 1.0;
				vector.put(term.getKey(), term.getValue() * idf);
			}
			vectors.add(vector);
		}
		return vectors;
	}

	private static double cosine(Map<String, Double> a, Map<String, Double> b) {
		if (a.isEmpty() || b.isEmpty()) {
			return 0.0;
		}
		Map<String, Double> small = a.size() <= b.size() ? a : b;
		Map<String, Double> large = small == a ? b : a;
		double dot = 0.0;
		for (Map.Entry<String, Double> term : small.entrySet()) {
			Double other = large.get(term.getKey());
			if (other != null) {
				dot += term.getValue() * other;
			}
		}
		return dot == 0.0 ? 0.0 : dot / (norm(a) * norm(b));
	}

	private static double norm(Map<String, Double> vector) {
		double sum = 0.0;
		for (double value : vector.values()) {
			sum += value * value;
		}
		return Math.sqrt(sum);
	}

	private static double round(double value) {
		return Math.round(value * 1000.0) / 1000.0;
	}

	private static String key(EntityRef ref) {
		return ref.entityType() + ":" + String.format("%012d", ref.entityId());
	}
}
