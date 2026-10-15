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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.requel.assistant.core.corpus.WordRelations;

/**
 * Issue #266: {@link WordRelations} over the WordNet dictionary. Synonyms are the other words in
 * the synsets of a word's senses ranked {@link #MAX_SENSE_RANK} or better (the common senses, so
 * "bank" doesn't bring "depository" and "slope" into every text). Antonyms are WordNet's lexical
 * antonym links from the same common senses, adjectives and adverbs only: tuning on a real project
 * found verb and noun antonyms (start / stop, pass / fail, one / off) name the steps of a process,
 * not a conflict between requirements, while adjective ones (visible / invisible, public / private)
 * do.
 *
 * <p>An inflected word is looked up by its own spelling first, then by a few regular base forms
 * ("days" -> "day", "renewed" -> "renew"), the first form the dictionary answers for wins. Results
 * are cached, most recently used kept.
 */
@ConditionalOnProperty(name = "requel.nlp.enabled", havingValue = "true", matchIfMissing = true)
@Component
public class WordNetWordRelations implements WordRelations {

	/** Senses ranked this or better contribute synonyms. */
	static final int MAX_SENSE_RANK = 2;

	/** Synset parts of speech whose antonyms count: adjective, adjective satellite, adverb. */
	static final List<String> ANTONYM_POS = List.of("a", "s", "r");

	private static final int CACHE_SIZE = 20_000;

	private final DictionaryRepository dictionary;
	private final Map<String, Set<String>> synonymCache = lru();
	private final Map<String, Set<String>> antonymCache = lru();

	@Autowired
	public WordNetWordRelations(DictionaryRepository dictionary) {
		this.dictionary = Objects.requireNonNull(dictionary, "dictionary");
	}

	@Override
	public Set<String> synonyms(String word) {
		return lookup(synonymCache, word,
				(repository, lemma) -> repository.findSynonymLemmas(lemma, MAX_SENSE_RANK));
	}

	@Override
	public Set<String> antonyms(String word) {
		return lookup(antonymCache, word, (repository, lemma) -> repository
				.findAntonymLemmas(lemma, MAX_SENSE_RANK, ANTONYM_POS));
	}

	private Set<String> lookup(Map<String, Set<String>> cache, String word,
			BiFunction<DictionaryRepository, String, Set<String>> query) {
		if (word == null || word.isBlank()) {
			return Set.of();
		}
		String key = word.toLowerCase(Locale.ROOT).strip();
		synchronized (cache) {
			Set<String> cached = cache.get(key);
			if (cached != null) {
				return cached;
			}
		}
		Set<String> result = Set.of();
		for (String form : baseForms(key)) {
			Set<String> found = query.apply(dictionary, form);
			if (found != null && !found.isEmpty()) {
				found.remove(form);
				found.remove(key);
				result = Set.copyOf(found);
				break;
			}
		}
		synchronized (cache) {
			cache.put(key, result);
		}
		return result;
	}

	/** The word, then regular base forms of it, most likely first. */
	static List<String> baseForms(String word) {
		List<String> forms = new ArrayList<>();
		forms.add(word);
		if (word.endsWith("ies") && word.length() > 4) {
			forms.add(word.substring(0, word.length() - 3) + "y");
		}
		if (word.endsWith("es") && word.length() > 3) {
			forms.add(word.substring(0, word.length() - 2));
		}
		if (word.endsWith("s") && !word.endsWith("ss") && word.length() > 3) {
			forms.add(word.substring(0, word.length() - 1));
		}
		if (word.endsWith("ed") && word.length() > 4) {
			forms.add(word.substring(0, word.length() - 2));
			forms.add(word.substring(0, word.length() - 1));
		}
		if (word.endsWith("ing") && word.length() > 5) {
			forms.add(word.substring(0, word.length() - 3));
			forms.add(word.substring(0, word.length() - 3) + "e");
		}
		// "scanned", "scanning" -> "scan"
		for (String suffix : List.of("ed", "ing")) {
			String stem = word.endsWith(suffix) ? word.substring(0, word.length() - suffix.length())
					: "";
			int n = stem.length();
			if (n > 2 && stem.charAt(n - 1) == stem.charAt(n - 2)
					&& "aeiou".indexOf(stem.charAt(n - 1)) < 0) {
				forms.add(stem.substring(0, n - 1));
			}
		}
		return forms;
	}

	private static Map<String, Set<String>> lru() {
		return Collections.synchronizedMap(new LinkedHashMap<String, Set<String>>(256, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, Set<String>> eldest) {
				return size() > CACHE_SIZE;
			}
		});
	}
}
