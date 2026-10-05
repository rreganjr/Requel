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
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Issue #359: the shapes of text that is an address or a secret rather than prose. The email and
 * credential patterns are the ones {@link DefaultRedactionPolicy} masks before text leaves
 * Requel (#262); {@link #URL} is used only by {@link #spans}, since a plain URL is not secret. The
 * lexical checks use {@link #spans} so they never report a fragment of one of these as a word.
 */
public final class RedactablePatterns {

	/** PEM private-key blocks, whole. */
	public static final Pattern PRIVATE_KEY = Pattern.compile(
			"-----BEGIN [A-Z0-9 ]*PRIVATE KEY-----[\\s\\S]*?-----END [A-Z0-9 ]*PRIVATE KEY-----");

	/** {@code scheme://user:password@}: group 1 is up to the password, group 2 the password. */
	public static final Pattern URL_PASSWORD = Pattern.compile(
			"\\b([a-zA-Z][a-zA-Z0-9+.-]*://[^\\s:/@]+:)([^\\s@/]+)@");

	/** {@code password=...}, {@code secret: "..."}: group 1 the name, 2 the separator, 3 the value. */
	public static final Pattern SECRET_PAIR = Pattern.compile(
			"(?i)\\b(password|passwd|pwd|secret|client[_-]?secret"
					+ "|token|access[_-]?token|api[_-]?key|access[_-]?key)(\\s*[:=]\\s*)"
					+ "(\"[^\"]*\"|'[^']*'|[^\\s,;]+)");

	/** {@code Bearer <token>}: group 1 the word. */
	public static final Pattern BEARER = Pattern.compile(
			"(?i)\\b(bearer)\\s+[A-Za-z0-9._~+/=-]{16,}");

	/** JWTs. */
	public static final Pattern JWT = Pattern.compile(
			"\\beyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}");

	/** Well-known API key shapes. */
	public static final Pattern API_KEY = Pattern.compile(
			"\\b(?:sk-ant-[A-Za-z0-9_-]{20,}|sk-(?:proj-)?[A-Za-z0-9_-]{20,}"
					+ "|gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,}"
					+ "|AKIA[0-9A-Z]{16}|xox[abprs]-[A-Za-z0-9-]{10,}|AIza[0-9A-Za-z_-]{35})");

	/** Email addresses. */
	public static final Pattern EMAIL = Pattern.compile(
			"\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");

	/** A URL: a scheme or {@code www.}, up to whitespace, a quote or an angle bracket. */
	public static final Pattern URL = Pattern.compile(
			"\\b(?:[a-zA-Z][a-zA-Z0-9+.-]*://|www\\.)[^\\s<>\"'`]+");

	/** Every credential pattern, in the order {@link DefaultRedactionPolicy} applies them. */
	public static final List<Pattern> CREDENTIALS = List.of(PRIVATE_KEY, URL_PASSWORD, SECRET_PAIR,
			BEARER, JWT, API_KEY);

	/** What a span is. */
	public enum Kind {
		CREDENTIAL, EMAIL, URL
	}

	/** {@code [start, end)} of a match in the text. */
	public record Span(int start, int end, Kind kind) {

		/** Whether {@code [from, to)} lies wholly inside this span. */
		public boolean covers(int from, int to) {
			return start <= from && to <= end;
		}
	}

	private RedactablePatterns() {
	}

	/**
	 * Every credential, email and URL in {@code text}, ordered by start. Spans may overlap (a URL
	 * with a password in it is both).
	 */
	public static List<Span> spans(String text) {
		if (text == null || text.isEmpty()) {
			return List.of();
		}
		List<Span> spans = new ArrayList<>();
		for (Pattern credential : CREDENTIALS) {
			add(spans, credential, text, Kind.CREDENTIAL);
		}
		add(spans, EMAIL, text, Kind.EMAIL);
		add(spans, URL, text, Kind.URL);
		spans.sort(Comparator.comparingInt(Span::start).thenComparingInt(Span::end));
		return List.copyOf(spans);
	}

	/** Whether {@code [from, to)} lies wholly inside one of {@code spans}. */
	public static boolean covered(List<Span> spans, int from, int to) {
		for (Span span : spans) {
			if (span.covers(from, to)) {
				return true;
			}
		}
		return false;
	}

	private static void add(List<Span> spans, Pattern pattern, String text, Kind kind) {
		Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			if (matcher.end() > matcher.start()) {
				spans.add(new Span(matcher.start(), matcher.end(), kind));
			}
		}
	}
}
