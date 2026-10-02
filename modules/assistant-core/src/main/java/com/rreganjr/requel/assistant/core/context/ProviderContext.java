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

import java.util.List;

import com.rreganjr.platform.identity.User;

/**
 * Issue #261: what a {@link ContextProvider} needs to add text the way the base pack does: the
 * project's redaction policy (#262), the run's author pseudonyms, and the pack's notes. Every name
 * and text a provider adds goes through here.
 */
public final class ProviderContext {

	private final String providerId;
	private final RedactionPolicy policy;
	private final AuthorPseudonyms authors;
	private final List<String> redacted;
	private final List<String> truncated;
	private final int maxTextChars;

	ProviderContext(String providerId, RedactionPolicy policy, AuthorPseudonyms authors,
			List<String> redacted, List<String> truncated, int maxTextChars) {
		this.providerId = providerId;
		this.policy = policy;
		this.authors = authors;
		this.redacted = redacted;
		this.truncated = truncated;
		this.maxTextChars = maxTextChars;
	}

	/** A redacted name; {@code path} names the field in notes, under the provider's id. */
	public String name(String path, String value) {
		return ContextPackTextUtils.prepareName(fieldPath(path), value, policy, redacted);
	}

	/** Redacted text cut to the pack's per-field limit. */
	public String text(String path, String value) {
		return text(path, value, maxTextChars);
	}

	/**
	 * Redacted text cut to {@code maxChars} (a summary). A summary cut is expected, so it is not
	 * noted; only a cut at the pack's per-field limit is.
	 */
	public String text(String path, String value, int maxChars) {
		if (maxChars >= maxTextChars) {
			return ContextPackTextUtils.prepareText(fieldPath(path), value, maxTextChars, policy,
					redacted, truncated);
		}
		String redactedValue = policy.redact(fieldPath(path), value, redacted);
		if (redactedValue == null || redactedValue.length() <= maxChars) {
			return redactedValue;
		}
		return redactedValue.substring(0, maxChars) + "...";
	}

	/** The run's role for a user: {@code assistant} or {@code user-N}, never the username. */
	public String author(User user) {
		return authors.of(user);
	}

	private String fieldPath(String path) {
		return "context." + providerId + "." + path;
	}
}
