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
 * Small helpers shared by the context-pack builders. Package-private; all
 * external callers go through the builders.
 */
final class ContextPackTextUtils {

	private ContextPackTextUtils() {
	}

	/**
	 * Run {@code value} through the {@link RedactionPolicy}, then clamp it to
	 * {@code maxChars}. Records both redaction notes (via the policy) and
	 * truncation notes (here) so the caller can summarize them in
	 * {@link ContextPackMetadata}.
	 */
	static String prepareText(String fieldPath, String value, int maxChars, RedactionPolicy policy,
			List<String> redactionNotes, List<String> truncationNotes) {
		String redacted = policy.redact(fieldPath, value, redactionNotes);
		if (redacted == null) {
			return null;
		}
		if (maxChars > 0 && redacted.length() > maxChars) {
			truncationNotes.add(fieldPath + " truncated from " + redacted.length() + " to "
					+ maxChars + " chars");
			return redacted.substring(0, maxChars);
		}
		return redacted;
	}

	static String username(User user) {
		return user == null ? null : user.getUsername();
	}
}
