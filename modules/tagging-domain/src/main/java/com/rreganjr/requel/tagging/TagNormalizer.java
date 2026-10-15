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
package com.rreganjr.requel.tagging;

import java.util.Locale;

/**
 * Canonicalizes tag category/value strings on write: trims, lowercases with {@code Locale.ROOT},
 * and collapses runs of non-alphanumeric characters into single hyphens (e.g.
 * {@code " Business Rule "} &rarr; {@code "business-rule"}, {@code "CON-3685"} &rarr;
 * {@code "con-3685"}, {@code "v2.0"} &rarr; {@code "v2-0"}).
 *
 * <p><strong>The slug is the stored value and the uniqueness key, not a display transform.</strong>
 * {@code tag_category} is unique on {@code (project_id, name)} and {@code tag} carries a
 * denormalized {@code category} beside {@code value}, so what this method returns is what those
 * constraints compare. The text the caller supplied is not retained anywhere.
 *
 * <p>That is deliberate rather than incidental (issue #255). Requel's tags follow Conduit's, where
 * slugging is what stops one project accumulating {@code Source}, {@code source} and
 * {@code "Source "} as three separate vocabularies. Preserving display case with case-insensitive
 * uniqueness would keep the key controlled while letting the displayed vocabulary drift, which is
 * most of the near-duplicate problem back again. A tag is therefore a controlled vocabulary entry
 * and not a carrier for text that has to round-trip — entity provenance, which needs exactly that,
 * is tracked separately in issue #272 and {@code doc/work/backlog/entity-provenance-notes.md}.
 *
 * <p>Callers reach this through {@code EditTagCategory} and {@code EditTag}, whose input DTOs carry
 * a {@code CommandDescription} stating the behaviour in the MCP tool description and CLI help.
 *
 * @author ron
 */
public final class TagNormalizer {

	private TagNormalizer() {
	}

	/**
	 * Normalize a value/category to a slug, or {@code null} if the input is null or blank
	 * after trimming.
	 */
	public static String slug(String raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = raw.trim().toLowerCase(Locale.ROOT);
		if (trimmed.isEmpty()) {
			return null;
		}
		String slug = trimmed.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
		return slug.isEmpty() ? null : slug;
	}
}
