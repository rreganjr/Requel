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
package com.rreganjr.requel.assistant.core.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import opennlp.tools.stemmer.PorterStemmer;

/**
 * Issue #261: a non-AI similarity score between texts, for ranking which sibling goals a review
 * sees. TF-IDF over lower-cased, Porter-stemmed words with stop words removed; each glossary term
 * and its alternate names count as one token, so "web conference" and "webinar" match when the
 * glossary says they are the same term. Cosine similarity, deterministic.
 *
 * <p>#266's candidate finder extends this with dictionary synonyms and conflict hints.
 */
public final class TextSimilarity {

	private static final Set<String> STOP_WORDS = Set.of("a", "an", "and", "are", "as", "at", "be",
			"by", "for", "from", "has", "have", "in", "into", "is", "it", "its", "of", "on", "or",
			"that", "the", "their", "them", "then", "there", "these", "this", "those", "to", "was",
			"were", "which", "with", "within", "without", "all", "any", "each", "every", "so",
			"than", "can", "could", "may", "might", "must", "shall", "should", "will", "would",
			"not", "no", "do", "does", "when", "while", "if", "who", "what", "how");

	/** Glossary phrases (lower case) to their shared token, longest first. */
	private final Map<String, String> phrases;

	/** @param phraseTokens glossary phrase (any case) to the token it stands for */
	public TextSimilarity(Map<String, String> phraseTokens) {
		List<Map.Entry<String, String>> entries = new ArrayList<>();
		for (Map.Entry<String, String> entry : phraseTokens.entrySet()) {
			if (entry.getKey() != null && !entry.getKey().isBlank()) {
				entries.add(Map.entry(entry.getKey().toLowerCase(Locale.ROOT).strip(),
						entry.getValue()));
			}
		}
		entries.sort(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length())
				.reversed());
		this.phrases = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : entries) {
			this.phrases.putIfAbsent(entry.getKey(), entry.getValue());
		}
	}

	/** No glossary. */
	public TextSimilarity() {
		this(Map.of());
	}

	/**
	 * Cosine similarity of {@code query} to each of {@code documents}, in the same order, in
	 * [0, 1]. Inverse document frequencies come from {@code documents} plus the query.
	 */
	public double[] scores(String query, List<String> documents) {
		List<Map<String, Integer>> docTerms = new ArrayList<>(documents.size());
		Map<String, Integer> documentFrequency = new HashMap<>();
		PorterStemmer stemmer = new PorterStemmer();
		Map<String, Integer> queryTerms = terms(query, stemmer);
		count(queryTerms, documentFrequency);
		for (String document : documents) {
			Map<String, Integer> terms = terms(document, stemmer);
			docTerms.add(terms);
			count(terms, documentFrequency);
		}
		int corpus = documents.size() + 1;
		Map<String, Double> queryVector = weigh(queryTerms, documentFrequency, corpus);
		double[] result = new double[documents.size()];
		for (int i = 0; i < documents.size(); i++) {
			result[i] = cosine(queryVector, weigh(docTerms.get(i), documentFrequency, corpus));
		}
		return result;
	}

	/**
	 * One token of a text: the surface word as written (lower case) and the term it counts as,
	 * either its Porter stem or {@code gt:<token>} for a glossary phrase (#266).
	 */
	public record Token(String surface, String term) {

		/** True for a glossary phrase token. */
		public boolean isGlossary() {
			return term.startsWith("gt:");
		}
	}

	/**
	 * The glossary-folded, stemmed tokens of {@code text} in order, stop words and one-letter
	 * words removed (#266). Glossary phrases come first, then the remaining words.
	 */
	public List<Token> tokens(String text, PorterStemmer stemmer) {
		List<Token> tokens = new ArrayList<>();
		if (text == null || text.isBlank()) {
			return tokens;
		}
		String folded = " " + text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{Nd}]+", " ")
				+ " ";
		for (Map.Entry<String, String> phrase : phrases.entrySet()) {
			String needle = " " + phrase.getKey().replaceAll("[^\\p{L}\\p{Nd}]+", " ").strip() + " ";
			if (needle.isBlank()) {
				continue;
			}
			int at = folded.indexOf(needle);
			while (at >= 0) {
				tokens.add(new Token(needle.strip(), "gt:" + phrase.getValue()));
				folded = folded.substring(0, at) + " " + folded.substring(at + needle.length() - 1);
				at = folded.indexOf(needle);
			}
		}
		for (String word : folded.trim().split("\\s+")) {
			if (isStopWord(word)) {
				continue;
			}
			tokens.add(new Token(word, stemmer.stem(word)));
		}
		return tokens;
	}

	/** True for a word the similarity ignores: empty, a stop word, or one letter (#266). */
	public static boolean isStopWord(String word) {
		return word.isEmpty() || STOP_WORDS.contains(word)
				|| word.length() < 2 && !Character.isDigit(word.charAt(0));
	}

	/** The stemmed, glossary-folded tokens of {@code text}, with counts. */
	Map<String, Integer> terms(String text, PorterStemmer stemmer) {
		Map<String, Integer> counts = new HashMap<>();
		for (Token token : tokens(text, stemmer)) {
			counts.merge(token.term(), 1, Integer::sum);
		}
		return counts;
	}

	private static void count(Map<String, Integer> terms, Map<String, Integer> frequency) {
		for (String term : terms.keySet()) {
			frequency.merge(term, 1, Integer::sum);
		}
	}

	private static Map<String, Double> weigh(Map<String, Integer> terms,
			Map<String, Integer> frequency, int corpus) {
		Map<String, Double> vector = new HashMap<>();
		for (Map.Entry<String, Integer> term : terms.entrySet()) {
			double idf = Math.log((corpus + 1.0) / (frequency.getOrDefault(term.getKey(), 0) + 1.0))
					+ 1.0;
			vector.put(term.getKey(), term.getValue() * idf);
		}
		return vector;
	}

	private static double cosine(Map<String, Double> a, Map<String, Double> b) {
		if (a.isEmpty() || b.isEmpty()) {
			return 0.0;
		}
		Set<String> shared = new HashSet<>(a.keySet());
		shared.retainAll(b.keySet());
		double dot = 0.0;
		for (String term : shared) {
			dot += a.get(term) * b.get(term);
		}
		return dot == 0.0 ? 0.0 : dot / (norm(a.values()) * norm(b.values()));
	}

	private static double norm(Collection<Double> values) {
		double sum = 0.0;
		for (double value : values) {
			sum += value * value;
		}
		return Math.sqrt(sum);
	}
}
