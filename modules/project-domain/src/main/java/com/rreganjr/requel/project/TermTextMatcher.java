/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.project;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds and replaces a glossary term's name in text (issue #348): whole words or a whole phrase,
 * ignoring case. A boundary is any character that is not a letter or a digit, or the text's
 * edge, so "dry-run event" matches in "the dry-run event's slides" but not in "dry-run events"
 * or "pre-dry-run eventual". A replacement keeps the matched text's leading capital.
 */
public final class TermTextMatcher {

	private TermTextMatcher() {
	}

	/**
	 * @return whether {@code text} uses {@code phrase} as a whole word or phrase, in any case;
	 *         false when either is null or blank
	 */
	public static boolean contains(String text, String phrase) {
		if (isBlank(text) || isBlank(phrase)) {
			return false;
		}
		return pattern(phrase).matcher(text).find();
	}

	/**
	 * Replace every whole-word use of {@code phrase} in {@code text} with {@code replacement},
	 * carrying each match's leading capital.
	 *
	 * @return the new text, or {@code text} itself when nothing matched (or either is blank)
	 */
	public static String replace(String text, String phrase, String replacement) {
		if (isBlank(text) || isBlank(phrase) || replacement == null) {
			return text;
		}
		Matcher m = pattern(phrase).matcher(text);
		StringBuilder out = new StringBuilder();
		boolean found = false;
		while (m.find()) {
			found = true;
			m.appendReplacement(out, Matcher.quoteReplacement(matchCase(m.group(), replacement)));
		}
		if (!found) {
			return text;
		}
		m.appendTail(out);
		return out.toString();
	}

	private static Pattern pattern(String phrase) {
		return Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(phrase.trim())
				+ "(?![\\p{L}\\p{N}])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	}

	/**
	 * The replacement takes the case of the matched text's first letter, so a term named
	 * "Dry-run webinar" reads "the dry-run webinar" mid-sentence and "Dry-run webinar" at its
	 * start. A replacement that starts with an acronym ("API gateway") is left as it is.
	 */
	private static String matchCase(String matched, String replacement) {
		if (replacement.isEmpty() || matched.isEmpty() || startsWithAcronym(replacement)) {
			return replacement;
		}
		String first = replacement.substring(0, 1);
		first = Character.isUpperCase(matched.charAt(0)) ? first.toUpperCase(Locale.ROOT)
				: first.toLowerCase(Locale.ROOT);
		return first + replacement.substring(1);
	}

	private static boolean startsWithAcronym(String s) {
		return s.length() > 1 && Character.isUpperCase(s.charAt(0))
				&& Character.isUpperCase(s.charAt(1));
	}

	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}
}
