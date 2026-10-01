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

	/**
	 * Issue #262: run a name through the {@link RedactionPolicy}. Names are not truncated (they
	 * are short), but an email or key in a name must not reach a model either.
	 */
	static String prepareName(String fieldPath, String value, RedactionPolicy policy,
			List<String> redactionNotes) {
		return value == null ? null : policy.redact(fieldPath, value, redactionNotes);
	}

	/** Issue #262: the pack's project id, for {@link RedactionPolicy#forProject}. */
	static Long projectIdOf(Object target) {
		if (target instanceof com.rreganjr.requel.project.Project project) {
			return project.getId();
		}
		if (target instanceof com.rreganjr.requel.project.ProjectOrDomainEntity entity
				&& entity.getProjectOrDomain() != null) {
			return entity.getProjectOrDomain().getId();
		}
		return null;
	}

	/**
	 * The raw username. Since #262 the builders send {@link AuthorPseudonyms} labels instead;
	 * this stays for callers that are not building a pack.
	 */
	static String username(User user) {
		return user == null ? null : user.getUsername();
	}
}
