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
package com.rreganjr.requel.project.impl.command;

import java.util.regex.Pattern;

import com.rreganjr.validator.EntityValidationException;

/**
 * The rule every word added from a Dictionary page passes (issue #319): trimmed, non-blank, a
 * single token with no internal whitespace, and at most 80 characters, the width of the
 * {@code lemma} columns. A spell checker only ever asks about single tokens, so a phrase could
 * never match anything.
 *
 * @author ron
 */
final class DictionaryWordRules {

	/** The width of {@code project_dictionary_words.lemma} and {@code install_dictionary_words.lemma}. */
	static final int MAX_LEMMA_LENGTH = 80;

	private static final Pattern WHITESPACE = Pattern.compile("\\s");

	private DictionaryWordRules() {
	}

	/**
	 * @param entityType -
	 *            the word entity, for the validation error.
	 * @param lemma -
	 *            the word as entered.
	 * @return the trimmed word.
	 * @throws EntityValidationException
	 *             on property {@code lemma} if the word breaks the rule.
	 */
	static String normalize(Class<?> entityType, String lemma) {
		String trimmed = (lemma == null ? "" : lemma.trim());
		if (trimmed.isEmpty()) {
			throw EntityValidationException.validationFailed(entityType, "lemma",
					"the word cannot be empty.");
		}
		if (WHITESPACE.matcher(trimmed).find()) {
			throw EntityValidationException.validationFailed(entityType, "lemma",
					"the word must be a single word with no spaces.");
		}
		if (trimmed.length() > MAX_LEMMA_LENGTH) {
			throw EntityValidationException.validationFailed(entityType, "lemma",
					"the word cannot be longer than " + MAX_LEMMA_LENGTH + " characters.");
		}
		return trimmed;
	}
}
