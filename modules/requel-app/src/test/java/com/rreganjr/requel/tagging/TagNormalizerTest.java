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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the tag slug normalizer (issue #255).
 *
 * <p>The normalization is deliberate — it is what keeps tags a controlled vocabulary rather than
 * free text — and it is now stated in {@code EditTagCategoryInput} and {@code EditTagInput}'s
 * command descriptions and in the admin UI. These tests pin the behaviour those promises describe,
 * so the documentation and the code cannot drift apart silently.
 */
public class TagNormalizerTest {

	@Test
	public void trimsAndLowercases() {
		assertEquals("source", TagNormalizer.slug("Source"));
		assertEquals("source", TagNormalizer.slug("  SOURCE  "));
		assertEquals("business-rule", TagNormalizer.slug(" Business Rule "));
	}

	@Test
	public void collapsesRunsOfNonAlphanumericsIntoOneHyphen() {
		assertEquals("a-b", TagNormalizer.slug("a   b"));
		assertEquals("a-b", TagNormalizer.slug("a...b"));
		assertEquals("a-b", TagNormalizer.slug("a _ - . b"));
		assertEquals("con-3685", TagNormalizer.slug("CON-3685"));
		assertEquals("v2-0", TagNormalizer.slug("v2.0"));
	}

	@Test
	public void stripsLeadingAndTrailingHyphens() {
		assertEquals("billing", TagNormalizer.slug("--billing--"));
		assertEquals("billing", TagNormalizer.slug("...billing..."));
		assertEquals("billing", TagNormalizer.slug("-billing"));
	}

	@Test
	public void returnsNullWhenNothingSurvives() {
		assertNull(TagNormalizer.slug(null));
		assertNull(TagNormalizer.slug(""));
		assertNull(TagNormalizer.slug("   "));
		assertNull(TagNormalizer.slug("---"));
		assertNull(TagNormalizer.slug("!!! ??? ..."));
	}

	/**
	 * {@code slug} lowercases with {@code Locale.ROOT} on purpose. Under a Turkish default locale a
	 * bare {@code toLowerCase()} turns {@code "TITLE"} into {@code "tıtle"} with a dotless i, which
	 * would make the same tag text slug differently depending on where the server runs — and the
	 * slug is the uniqueness key, so that is a data problem rather than a display one.
	 */
	@Test
	public void lowercasingIsIndependentOfTheDefaultLocale() {
		Locale original = Locale.getDefault();
		try {
			Locale.setDefault(Locale.forLanguageTag("tr-TR"));
			assertEquals("title", TagNormalizer.slug("TITLE"));
			assertEquals("in-progress", TagNormalizer.slug("IN PROGRESS"));
		} finally {
			Locale.setDefault(original);
		}
	}
}
