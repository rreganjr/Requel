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

import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Issue #268: tokens the spelling check leaves alone even though the dictionary doesn't know them,
 * because they are the project's own vocabulary or aren't ordinary words at all. Every rule came
 * from the roundtable project's unknown tokens (the #268 harness): acronyms ({@code MP4},
 * {@code RTMP}), CamelCase product names ({@code CloudWatch}), identifiers with a digit or an
 * underscore ({@code v1}, {@code ENTRA_SETUP}) and hyphenated compounds of known words
 * ({@code co-hosts}, {@code non-production}).
 */
public final class SpellingSkips {

	/** Two or more upper-case letters or digits, at least one letter, optionally a plural "s". */
	static final Pattern ACRONYM = Pattern.compile("^(?=.*\\p{Lu})[\\p{Lu}0-9]{2,}s?$");

	/** A lower-case letter followed by an upper-case one, anywhere in the token. */
	static final Pattern CAMEL_CASE = Pattern.compile(".*\\p{Ll}\\p{Lu}.*");

	/** Prefixes that may start a hyphenated compound without being dictionary words themselves. */
	static final Set<String> HYPHEN_PREFIXES = Set.of("co", "re", "pre", "non", "multi", "sub",
			"self", "anti", "semi", "post", "inter", "cross", "mid");

	private SpellingSkips() {
	}

	/**
	 * Why {@code token} is not reported as a misspelling, or {@code null} if nothing exempts it.
	 * Checked in order: {@code vocabulary}, {@code acronym}, {@code camel-case}, {@code digit},
	 * {@code underscore}, {@code hyphen}.
	 *
	 * @param vocabulary the project's vocabulary; may be null
	 * @param knownWord whether a single word is in the dictionary; used for the parts of a
	 *            hyphenated token only, and may be null (hyphenated tokens are then not exempt)
	 */
	public static String reason(String token, ProjectVocabulary vocabulary,
			Predicate<String> knownWord) {
		if (token == null || token.isEmpty()) {
			return null;
		}
		if (vocabulary != null && vocabulary.contains(token)) {
			return "vocabulary";
		}
		if (ACRONYM.matcher(token).matches()) {
			return "acronym";
		}
		if (CAMEL_CASE.matcher(token).matches()) {
			return "camel-case";
		}
		if (token.chars().anyMatch(Character::isDigit)) {
			return "digit";
		}
		if (token.indexOf('_') >= 0) {
			return "underscore";
		}
		if (knownWord != null && isKnownCompound(token.toLowerCase(Locale.ROOT), knownWord)) {
			return "hyphen";
		}
		return null;
	}

	/** Every part of a hyphenated token is a known word (or its plural) or an allowed prefix. */
	static boolean isKnownCompound(String lower, Predicate<String> knownWord) {
		if (lower.indexOf('-') <= 0 || lower.endsWith("-")) {
			return false;
		}
		for (String part : lower.split("-")) {
			if (part.isEmpty()) {
				return false;
			}
			if (HYPHEN_PREFIXES.contains(part) || knownWord.test(part)) {
				continue;
			}
			if (part.length() > 3 && part.endsWith("s")
					&& knownWord.test(part.substring(0, part.length() - 1))) {
				continue;
			}
			return false;
		}
		return true;
	}
}
