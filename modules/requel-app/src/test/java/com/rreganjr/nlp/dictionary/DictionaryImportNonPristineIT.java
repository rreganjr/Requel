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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.nlp.dictionary.command.ImportDictionaryCommand;

/**
 * Regression test for issue #80: the dictionary XML import must be order-independent — it must
 * produce valid {@code sense -> word} foreign keys even when the {@code word} table is not a
 * pristine, id-aligned (IDENTITY counter at 1) table.
 *
 * <p>Before the fix, {@code Word.id} was a plain {@code @GeneratedValue(IDENTITY)} column: the DB
 * assigned fresh ids on import and discarded the dictionary-assigned source ids, while every
 * {@code Sense} still carried the source word id in its composite key. The two halves of the sense
 * key therefore only lined up when the import ran against an empty {@code word} table whose
 * IDENTITY counter reproduced the source ids. A single stray word offset the counter and broke the
 * FKs. With {@code Word.id} now using {@link com.rreganjr.nlp.dictionary.impl.repository.AssignedIdentityGenerator}
 * (assigned-or-generate), the import preserves the dictionary-assigned ids and is order-independent.
 *
 * <p>This test seeds a stray word (with an id well above the dictionary's id range so it cannot
 * collide with an assigned id) to make the table non-pristine, then imports the full dictionary and
 * asserts the sense FKs resolve.
 *
 * <p>It runs in its own Spring context (distinct {@link TestPropertySource}) so that seeding the
 * stray word and importing here does not perturb the shared integration-test context/DB.
 */
@TestPropertySource(properties = { "requel.test.context=dictionary-import-non-pristine" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DictionaryImportNonPristineIT extends AbstractIntegrationTestCase {

	/**
	 * An id far above the dictionary's source-id range (max source word id is ~147306) so seeding
	 * the stray word cannot collide with a dictionary-assigned id, while still leaving the table
	 * non-pristine before the import.
	 */
	private static final long STRAY_WORD_ID = 9_000_000L;
	private static final String STRAY_WORD_LEMMA = "zzzstraywordnonpristine";

	@Autowired
	private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

	@Test
	public void importIntoNonPristineTableResolvesSenseForeignKeys() throws Exception {
		// Start from a clean, but explicitly non-pristine, dictionary. This context is isolated, so
		// clearing here does not affect other tests.
		jdbcTemplate.execute("DELETE FROM sense");
		jdbcTemplate.execute("DELETE FROM word");

		// Seed a stray word so the table is not pristine (a pre-existing row, and an id well above
		// the dictionary's range) before importing — the condition under which the old
		// IDENTITY-based import reassigned word ids and broke the sense -> word FKs.
		jdbcTemplate.update("INSERT INTO word (wordid, lemma, phonetic_code) VALUES (?, ?, ?)",
				STRAY_WORD_ID, STRAY_WORD_LEMMA, null);

		// Import the full dictionary into the non-pristine table.
		ImportDictionaryCommand importDictionary =
				(ImportDictionaryCommand) applicationContext.getBean("importDictionaryCommand");
		InputStream in = getClass().getClassLoader()
				.getResourceAsStream("nlp/dictionary/dictionary.xml.gz");
		assertNotNull(in, "nlp/dictionary/dictionary.xml.gz not found on classpath");
		importDictionary.setInputStream(new GZIPInputStream(in));
		getCommandHandler().execute(importDictionary);

		// The word the NLP pipeline requires must be present and its senses must resolve back to it
		// — i.e. the sense -> word composite FK is valid.
		Word person = getDictionaryRepository().findWord("person", PartOfSpeech.NOUN);
		assertNotNull(person, "'person' should be found after importing into a non-pristine table");
		assertFalse(person.getSenses().isEmpty(), "'person' should have at least one sense");
		for (Sense sense : person.getSenses()) {
			assertEquals("person", sense.getWord().getLemma(),
					"each sense of 'person' must resolve to the 'person' word");
			assertNotNull(sense.getSynset(), "each sense must resolve to its synset");
		}

		// Direct, ORM-independent integrity check: no sense row may reference a missing word row.
		Integer orphanedSenses = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM sense s LEFT JOIN word w ON s.wordid = w.wordid "
						+ "WHERE w.wordid IS NULL",
				Integer.class);
		assertEquals(0, orphanedSenses,
				"every sense must reference an existing word after import (issue #80)");
	}
}
