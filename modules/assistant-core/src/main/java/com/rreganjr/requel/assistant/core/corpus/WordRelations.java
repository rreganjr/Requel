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

import java.util.Set;

/**
 * Issue #266: dictionary relations between words, for the corpus candidate finder. Words and
 * results are lower-case lemmas. Implemented over WordNet in the legacy NLP module; assistant-core
 * can't see the dictionary.
 */
public interface WordRelations {

	/** No dictionary: no synonyms, no antonyms. */
	WordRelations NONE = new WordRelations() {
		@Override
		public Set<String> synonyms(String word) {
			return Set.of();
		}

		@Override
		public Set<String> antonyms(String word) {
			return Set.of();
		}
	};

	/**
	 * Single-word synonyms of {@code word} from its most common senses, any part of speech,
	 * excluding the word itself. Empty for an unknown word.
	 */
	Set<String> synonyms(String word);

	/** Single-word antonyms of {@code word} in any sense. Empty for an unknown word. */
	Set<String> antonyms(String word);
}
