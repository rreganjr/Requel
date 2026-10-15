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

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the by-name tag token format used in the project XML round-trip.
 */
public class TagTokenTest {

	@Test
	public void formatsFlatNamespacedAndColored() {
		assertEquals("billing", new TagToken(null, "billing", null).toToken());
		assertEquals("type:business-rule", new TagToken("type", "business-rule", null).toToken());
		assertEquals("projectKind:product[#1d4ed8]",
				new TagToken("projectKind", "product", "#1d4ed8").toToken());
		assertEquals("billing[blue]", new TagToken(null, "billing", "blue").toToken());
	}

	@Test
	public void parsesFlatNamespacedAndColored() {
		TagToken flat = TagToken.parse("billing");
		assertNull(flat.category());
		assertEquals("billing", flat.value());
		assertNull(flat.color());

		TagToken ns = TagToken.parse("type:business-rule");
		assertEquals("type", ns.category());
		assertEquals("business-rule", ns.value());
		assertNull(ns.color());

		TagToken colored = TagToken.parse("projectKind:product[#1d4ed8]");
		assertEquals("projectKind", colored.category());
		assertEquals("product", colored.value());
		assertEquals("#1d4ed8", colored.color());

		TagToken flatColored = TagToken.parse("billing[blue]");
		assertNull(flatColored.category());
		assertEquals("billing", flatColored.value());
		assertEquals("blue", flatColored.color());
	}

	@Test
	public void roundTripsThroughFormatAndParse() {
		for (String token : new String[] { "billing", "type:business-rule",
				"projectKind:product[#1d4ed8]", "billing[blue]" }) {
			assertEquals(token, TagToken.parse(token).toToken(),
					"token should survive parse -> format");
		}
	}

	@Test
	public void returnsNullForBlankOrEmpty() {
		assertNull(TagToken.parse(null));
		assertNull(TagToken.parse("   "));
		assertNull(TagToken.parse(""));
	}
}
