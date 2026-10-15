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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.rreganjr.nlp.dictionary.DictionaryRepository;

/** Issue #266: WordNet relations, base forms and caching. */
class WordNetWordRelationsTest {

	private static Set<String> set(String... words) {
		return new TreeSet<>(Set.of(words));
	}

	@Test
	void anInflectedWordFallsBackToItsBaseForm() {
		DictionaryRepository dictionary = mock(DictionaryRepository.class);
		when(dictionary.findSynonymLemmas(anyString(), anyInt())).thenReturn(set());
		when(dictionary.findSynonymLemmas("day", WordNetWordRelations.MAX_SENSE_RANK))
				.thenReturn(set("twenty-four hours", "daytime", "day"));
		assertThat(new WordNetWordRelations(dictionary).synonyms("Days"))
				.containsExactlyInAnyOrder("twenty-four hours", "daytime");
	}

	@Test
	void antonymsComeFromTheLexicalLinks() {
		DictionaryRepository dictionary = mock(DictionaryRepository.class);
		when(dictionary.findAntonymLemmas(anyString(), anyInt(), anyCollection())).thenReturn(set());
		when(dictionary.findAntonymLemmas("visible", WordNetWordRelations.MAX_SENSE_RANK,
				WordNetWordRelations.ANTONYM_POS)).thenReturn(set("invisible", "unseeable"));
		WordNetWordRelations relations = new WordNetWordRelations(dictionary);
		assertThat(relations.antonyms("visible")).containsExactlyInAnyOrder("invisible",
				"unseeable");
		assertThat(relations.antonyms("unknownword")).isEmpty();
	}

	@Test
	void resultsAreCached() {
		DictionaryRepository dictionary = mock(DictionaryRepository.class);
		when(dictionary.findAntonymLemmas(anyString(), anyInt(), anyCollection()))
				.thenReturn(set("private"));
		WordNetWordRelations relations = new WordNetWordRelations(dictionary);
		relations.antonyms("public");
		relations.antonyms("PUBLIC");
		verify(dictionary, times(1)).findAntonymLemmas(anyString(), anyInt(), anyCollection());
	}

	@Test
	void baseFormsOfRegularInflections() {
		assertThat(WordNetWordRelations.baseForms("stories")).contains("story");
		assertThat(WordNetWordRelations.baseForms("renewed")).contains("renew");
		assertThat(WordNetWordRelations.baseForms("scanning")).contains("scan");
		assertThat(WordNetWordRelations.baseForms("class")).containsExactly("class");
	}
}
