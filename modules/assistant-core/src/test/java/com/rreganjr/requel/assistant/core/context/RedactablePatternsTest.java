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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.assistant.core.context.RedactablePatterns.Kind;
import com.rreganjr.requel.assistant.core.context.RedactablePatterns.Span;

/** Issue #359: the email, URL and credential spans the lexical checks skip. */
class RedactablePatternsTest {

	private static String spanned(String text, Span span) {
		return span.kind() + ":" + text.substring(span.start(), span.end());
	}

	private static List<String> spans(String text) {
		return RedactablePatterns.spans(text).stream().map(span -> spanned(text, span)).toList();
	}

	@Test
	void findsEmailsUrlsAndEachCredentialShapeInOrder() {
		String key = "sk-proj-Ab3dE5gH7jK9mN1pQ3sT5vX7";
		String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.c2lnbmF0dXJlLXZhbHVl";
		String text = "Mail ops-team@example.com, see https://wiki.example.org/page and"
				+ " www.example.net, key " + key + ", Bearer abcdefghijklmnopqrstuvwx, " + jwt
				+ " and password=hunter2.";

		assertThat(spans(text)).containsExactly(
				"EMAIL:ops-team@example.com",
				"URL:https://wiki.example.org/page",
				"URL:www.example.net,",
				"CREDENTIAL:" + key,
				"CREDENTIAL:Bearer abcdefghijklmnopqrstuvwx",
				"CREDENTIAL:" + jwt,
				"CREDENTIAL:password=hunter2.");
	}

	/** Spans overlap: the password part reads as an email address too. */
	@Test
	void aUrlWithAPasswordIsACredentialAndAUrl() {
		String text = "Clone https://deploy:hunter@ci.example.net/repo now.";

		assertThat(spans(text)).containsExactly("CREDENTIAL:https://deploy:hunter@",
				"URL:https://deploy:hunter@ci.example.net/repo", "EMAIL:hunter@ci.example.net");
		assertThat(RedactablePatterns.spans(text)).extracting(Span::kind)
				.containsExactly(Kind.CREDENTIAL, Kind.URL, Kind.EMAIL);
	}

	@Test
	void plainProseHasNoSpans() {
		assertThat(RedactablePatterns.spans("Members may borrow 5 books at a time.")).isEmpty();
		assertThat(RedactablePatterns.spans("")).isEmpty();
		assertThat(RedactablePatterns.spans(null)).isEmpty();
	}

	@Test
	void coveredMeansWhollyInsideOneSpan() {
		List<Span> spans = List.of(new Span(5, 10, Kind.EMAIL), new Span(20, 30, Kind.URL));

		assertThat(RedactablePatterns.covered(spans, 5, 10)).isTrue();
		assertThat(RedactablePatterns.covered(spans, 21, 25)).isTrue();
		assertThat(RedactablePatterns.covered(spans, 8, 12)).isFalse();
		assertThat(RedactablePatterns.covered(spans, 0, 3)).isFalse();
		assertThat(RedactablePatterns.covered(List.of(), 5, 10)).isFalse();
	}
}
