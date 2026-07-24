/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.nlp.dictionary;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.nlp.dictionary.impl.repository.AssignedOrGeneratedWordIdGenerator;

/**
 * Regression test for issue #80: a runtime-added word must not occupy an id that the dictionary
 * import will assign, so the import stays order-independent even against a {@code word} table that
 * already contains runtime words.
 *
 * <p>The dictionary import inserts words with their dictionary-assigned source ids (1..~150k). If a
 * runtime word — created without an id by the spell dictionary's add-word path — were allocated a
 * low id (e.g. id 1 on a fresh, empty table), a later import of dictionary word 1 would fail with a
 * primary-key collision. {@link AssignedOrGeneratedWordIdGenerator} therefore allocates runtime
 * words in a reserved high range (>= {@link AssignedOrGeneratedWordIdGenerator#RUNTIME_ID_FLOOR}).
 *
 * <p>This test runs in its own Spring context (distinct {@link TestPropertySource}) so it can start
 * from an empty {@code word} table without perturbing the shared integration-test context. It is
 * intentionally cheap: it does not import the full dictionary (that path is exercised by every
 * integration test that calls {@code ensureDictionaryLoaded}); it verifies the id-allocation
 * invariant directly and then proves a dictionary-style low id can still be inserted alongside the
 * runtime word.
 */
@TestPropertySource(properties = { "requel.test.context=word-id-reserved-range" })
public class DictionaryImportNonPristineIT extends AbstractIntegrationTestCase {

	@Autowired
	private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

	@Test
	public void runtimeWordIsAllocatedAboveTheDictionaryIdRange() throws Exception {
		// Start from an empty dictionary. This context is isolated, so clearing here is safe.
		jdbcTemplate.execute("DELETE FROM sense");
		jdbcTemplate.execute("DELETE FROM word");

		// A runtime word created without an id (the spell-dictionary add-word path uses
		// DictionaryRepository.persist) must be allocated in the reserved high range, even though
		// the table is empty and a naive max(id)+1 would hand out id 1.
		Word runtimeWord = getDictionaryRepository().persist(new Word("zznonpristineprobe", null));
		assertNotNull(runtimeWord.getId(), "runtime word should have been assigned an id");
		assertTrue(runtimeWord.getId() >= AssignedOrGeneratedWordIdGenerator.RUNTIME_ID_FLOOR,
				"runtime word id (" + runtimeWord.getId() + ") must be in the reserved range >= "
						+ AssignedOrGeneratedWordIdGenerator.RUNTIME_ID_FLOOR
						+ " so it cannot collide with a dictionary-assigned id");

		// The exact CI failure was a dictionary import inserting word id 1 onto a table where a
		// runtime word had already taken id 1. With the reserved range, id 1 is free, so a
		// dictionary-style low id inserts without a primary-key collision.
		jdbcTemplate.update("INSERT INTO word (wordid, lemma, phonetic_code) VALUES (?, ?, ?)",
				1L, "dictionarywordone", null);

		Integer wordsAtIdOne = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM word WHERE wordid = 1", Integer.class);
		assertTrue(wordsAtIdOne == 1, "a dictionary-style word must occupy id 1 alongside the runtime word");
	}
}
