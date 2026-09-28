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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.rreganjr.nlp.dictionary.NLPText;

/**
 * Issue #268 (and the rest of #269): what a lexical finding quotes must be text the entity actually
 * contains.
 *
 * <p>
 * The parser's tokens are not the source text. Phrases rebuilt by joining tokens with spaces
 * ({@code NLPText.getText()}) put spaces around hyphens and brackets ({@code force - stops}), and
 * print quotes and brackets as Penn Treebank escapes ({@code ``}, {@code -LRB-}). Before #314 the
 * sentencizer also cut sentences mid-word, so a token could be a fragment ({@code ermissions}) that
 * then failed the spell check. This class finds the source substring a run of tokens came from,
 * and checks that a quoted word or phrase occurs in the source at word boundaries, so an assistant
 * quotes what the author wrote and drops a finding about text that isn't there.
 */
public final class LexicalEvidence {

	/** Clitics the tokenizer splits off a word ({@code does|n't}); they never stand alone. */
	private static final Set<String> CLITICS = Set.of("n't", "'s", "'re", "'ve", "'ll", "'d", "'m",
			"’s", "’re", "’ve", "’ll", "’d", "’m", "n’t");

	private static final String NOT_WORD_BEFORE = "(?<![\\p{L}\\p{N}])";
	private static final String NOT_WORD_AFTER = "(?![\\p{L}\\p{N}])";

	private LexicalEvidence() {
	}

	/**
	 * The substring of {@code source} that {@code tokens} were parsed from: the tokens matched in
	 * order, case-insensitively, with optional whitespace between them and word boundaries at both
	 * ends.
	 *
	 * @return the matched source text, or {@code null} if the tokens don't occur in the source
	 */
	public static String locate(String source, List<NLPText> tokens) {
		if (source == null || tokens == null || tokens.isEmpty()) {
			return null;
		}
		StringBuilder regex = new StringBuilder();
		for (int i = 0; i < tokens.size(); i++) {
			String token = tokens.get(i).getText();
			if (token == null || token.isEmpty()) {
				return null;
			}
			if (i > 0) {
				regex.append("\\s*");
			}
			regex.append(tokenRegex(token));
		}
		String first = tokens.get(0).getText();
		String last = tokens.get(tokens.size() - 1).getText();
		String pattern = (startsWithWordChar(first) ? NOT_WORD_BEFORE : "") + regex
				+ (endsWithWordChar(last) ? NOT_WORD_AFTER : "");
		Matcher m = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
				.matcher(source);
		return m.find() ? m.group() : null;
	}

	/**
	 * Whether {@code quoted} occurs in {@code source} as whole words: case-insensitive, any run of
	 * whitespace in either matching any other, and no letter or digit immediately before or after.
	 * A clitic the tokenizer split off ({@code n't}) always passes, since it never stands alone.
	 */
	public static boolean occurs(String source, String quoted) {
		if (source == null || quoted == null || quoted.isBlank()) {
			return false;
		}
		String trimmed = quoted.trim();
		if (CLITICS.contains(trimmed.toLowerCase(java.util.Locale.ROOT))) {
			return true;
		}
		String body = Arrays.stream(trimmed.split("\\s+")).map(Pattern::quote)
				.collect(Collectors.joining("\\s+"));
		String pattern = (startsWithWordChar(trimmed) ? NOT_WORD_BEFORE : "") + body
				+ (endsWithWordChar(trimmed) ? NOT_WORD_AFTER : "");
		return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
				.matcher(source).find();
	}

	/** A token as the parser prints it, as a regex for what it matches in the source text. */
	static String tokenRegex(String token) {
		switch (token) {
		case "-LRB-":
			return "\\(";
		case "-RRB-":
			return "\\)";
		case "-LSB-":
			return "\\[";
		case "-RSB-":
			return "\\]";
		case "-LCB-":
			return "\\{";
		case "-RCB-":
			return "\\}";
		case "``":
		case "''":
			return "[\"“”'‘’]";
		case "--":
			return "(?:--|—|–)";
		default:
			return Pattern.quote(token);
		}
	}

	private static boolean startsWithWordChar(String s) {
		return !s.isEmpty() && Character.isLetterOrDigit(s.codePointAt(0));
	}

	private static boolean endsWithWordChar(String s) {
		return !s.isEmpty() && Character.isLetterOrDigit(s.codePointBefore(s.length()));
	}
}
