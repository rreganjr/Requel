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
package com.rreganjr.requel.assistant.ai;

import java.util.List;

/**
 * Issue #260: does a finding's cited evidence actually occur in the entity? Free and
 * deterministic: each reference, whitespace-normalised and with one pair of surrounding quotes
 * removed, must be a substring of the entity's name or text (also whitespace-normalised). Case
 * counts. A finding with no references is not checked.
 */
final class EvidenceCheck {

	private EvidenceCheck() {
	}

	/** True when at least one non-blank reference does not occur in {@code entityText}. */
	static boolean unverified(List<String> references, String entityText) {
		if (references == null || references.isEmpty()) {
			return false;
		}
		String haystack = normalise(entityText);
		for (String reference : references) {
			String needle = unquote(normalise(reference));
			if (!needle.isEmpty() && !haystack.contains(needle)) {
				return true;
			}
		}
		return false;
	}

	static String normalise(String text) {
		return text == null ? "" : text.replaceAll("\\s+", " ").strip();
	}

	private static String unquote(String text) {
		if (text.length() >= 2) {
			char first = text.charAt(0);
			char last = text.charAt(text.length() - 1);
			if ((first == '"' && last == '"') || (first == '\'' && last == '\'')
					|| (first == '“' && last == '”')
					|| (first == '‘' && last == '’')) {
				return text.substring(1, text.length() - 1).strip();
			}
		}
		return text;
	}
}
