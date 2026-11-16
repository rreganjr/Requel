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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Issue #459: turns a step written as a command into "&lt;actor&gt; does something". The verb goes
 * into its third-person "-s" form by rule, and the rest of the step stays as written.
 */
final class StepVerbs {

	private static final Map<String, String> IRREGULAR = Map.of("be", "is", "have", "has");

	private StepVerbs() {
	}

	/**
	 * The third-person singular of a verb: "download" → "downloads", "push" → "pushes", "carry"
	 * → "carries", "be" → "is". A hyphenated verb changes its last part ("Force-stop" →
	 * "force-stops"). The first letter is lowercased, since the verb no longer starts the
	 * sentence, unless the verb is an acronym ("SSH" stays "SSHs").
	 */
	static String thirdPerson(String verb) {
		if (verb == null || verb.isBlank()) {
			return verb;
		}
		String v = lowerFirst(verb.trim());
		int hyphen = v.lastIndexOf('-');
		if (hyphen >= 0 && hyphen < v.length() - 1) {
			return v.substring(0, hyphen + 1) + thirdPerson(v.substring(hyphen + 1));
		}
		String lower = v.toLowerCase(Locale.ROOT);
		String irregular = IRREGULAR.get(lower);
		if (irregular != null) {
			return irregular;
		}
		if (lower.endsWith("s") || lower.endsWith("x") || lower.endsWith("z")
				|| lower.endsWith("ch") || lower.endsWith("sh") || lower.endsWith("o")) {
			return v + "es";
		}
		if (lower.length() > 1 && lower.endsWith("y")
				&& "aeiou".indexOf(lower.charAt(lower.length() - 2)) < 0) {
			return v.substring(0, v.length() - 1) + "ies";
		}
		return v + "s";
	}

	/**
	 * The step's name with {@code subject} and the verb's "-s" form in place of the verb, the
	 * words before and after it kept as written: ("Then download the file", "download", "Host")
	 * → "Then Host downloads the file". The verb is matched as a whole word, ignoring case, and
	 * widened to the hyphenated word around it ("stop" in "Force-stop the stream"). Null when the
	 * verb isn't in the name. Pass "the system" in lower case: it is capitalised only when it
	 * starts the name.
	 */
	static String rewrite(String name, String verb, String subject) {
		if (name == null || verb == null || verb.isBlank() || subject == null) {
			return null;
		}
		Matcher m = verbIn(name, verb);
		if (m == null) {
			return null;
		}
		String word = m.group();
		String before = name.substring(0, m.start());
		String after = name.substring(m.end());
		String lead = before.isEmpty() ? upperFirst(subject) : subject;
		return before + lead + " " + thirdPerson(word) + after;
	}

	/**
	 * #472: what a step does, without its subject, to follow "and" in a merged step: "Member
	 * submits it" → "submits it"; a command is conjugated, "Submit the form" → "submits the form".
	 * Null when the step can't be read that way: it is unreadable, its subject isn't at the start
	 * of the name, or a command's verb isn't its first word.
	 */
	static String predicate(String name, StepReading reading) {
		if (name == null || reading == null) {
			return null;
		}
		String n = name.trim();
		switch (reading.kind()) {
		case SUBJECT: {
			String subject = reading.subject() == null ? "" : reading.subject().trim();
			if (subject.isEmpty() || n.length() <= subject.length()
					|| !n.regionMatches(true, 0, subject, 0, subject.length())
					|| !Character.isWhitespace(n.charAt(subject.length()))) {
				return null;
			}
			String rest = n.substring(subject.length()).trim();
			return rest.isEmpty() ? null : rest;
		}
		case COMMAND: {
			Matcher m = verbIn(n, reading.verb());
			if (m == null || m.start() != 0) {
				return null;
			}
			return thirdPerson(m.group()) + n.substring(m.end());
		}
		default:
			return null;
		}
	}

	/** The verb as a whole word in {@code name}, widened to a hyphenated word, or null. */
	private static Matcher verbIn(String name, String verb) {
		if (verb == null || verb.isBlank()) {
			return null;
		}
		Matcher m = Pattern.compile("(?<![\\p{L}\\p{N}-])(?:[\\p{L}\\p{N}]+-)*"
				+ Pattern.quote(verb.trim()) + "(?:-[\\p{L}\\p{N}]+)*(?![\\p{L}\\p{N}])",
				Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(name);
		return m.find() ? m : null;
	}

	private static String lowerFirst(String s) {
		if (s.length() > 1 && Character.isUpperCase(s.charAt(0))
				&& Character.isUpperCase(s.charAt(1))) {
			return s; // an acronym
		}
		return s.substring(0, 1).toLowerCase(Locale.ROOT) + s.substring(1);
	}

	private static String upperFirst(String s) {
		return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
	}
}
