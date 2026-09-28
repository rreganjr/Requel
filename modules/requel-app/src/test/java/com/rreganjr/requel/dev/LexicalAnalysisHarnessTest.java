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
package com.rreganjr.requel.dev;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Issue #268: the dev lexical harness's own logic. The evidence, vocabulary and spelling helpers
 * it shares with the assistants are tested in {@code assistant-legacy-nlp}; the harness itself
 * needs the NLP stack and a real project, so it is run by hand against the dev database.
 */
class LexicalAnalysisHarnessTest {

	@Test
	void onlyLoopbackAddressesAreAllowed() {
		assertTrue(LexicalAnalysisHarnessController.isLoopback("127.0.0.1"));
		assertTrue(LexicalAnalysisHarnessController.isLoopback("0:0:0:0:0:0:0:1"));
		assertFalse(LexicalAnalysisHarnessController.isLoopback("192.168.1.20"));
		assertFalse(LexicalAnalysisHarnessController.isLoopback(null));
	}
}
