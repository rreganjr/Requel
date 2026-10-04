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
package com.rreganjr.requel.assistant.corpus;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.nlp.dictionary.Linkdef;
import com.rreganjr.nlp.dictionary.Sense;
import com.rreganjr.nlp.dictionary.Word;
import com.rreganjr.requel.assistant.core.corpus.WordRelations;
import com.rreganjr.requel.assistant.legacynlp.WordNetWordRelations;

/**
 * Issue #266: the dictionary queries behind {@link WordNetWordRelations}, against the test
 * dictionary. The test dump has no lexical links, so the antonym case inserts one, from the most
 * common sense of "visible" (an adjective).
 */
public class WordNetWordRelationsIT extends AbstractIntegrationTestCase {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private WordRelations wordRelations;

	@Test
	void theWordNetBeanIsTheWordRelations() {
		assertThat(wordRelations).isInstanceOf(WordNetWordRelations.class);
	}

	@Test
	void synonymsComeFromTheCommonSensesSynsets() throws Exception {
		ensureDictionaryLoaded();
		Set<String> synonyms = new WordNetWordRelations(getDictionaryRepository())
				.synonyms("purchases");
		assertThat(synonyms).contains("buy").doesNotContain("purchase");
	}

	@Test
	void antonymsComeFromTheLexicalLinks() throws Exception {
		ensureDictionaryLoaded();
		transactionTemplate.executeWithoutResult(status -> {
			if (jdbcTemplate.queryForObject("select count(*) from linkdef where linkid = ?",
					Integer.class, Linkdef.ANTONYM) == 0) {
				jdbcTemplate.update("insert into linkdef (linkid, name, recurses) values (?, ?, ?)",
						Linkdef.ANTONYM, "antonym", "N");
			}
			try {
				Sense visible = firstSense(getDictionaryRepository().findWordExact("visible"));
				Sense invisible = firstSense(getDictionaryRepository().findWordExact("invisible"));
				jdbcTemplate.update("delete from lexlinkref where linkid = ? and word1id = ?",
						Linkdef.ANTONYM, visible.getWord().getId());
				jdbcTemplate.update("insert into lexlinkref (linkid, synset1id, synset2id, word1id,"
						+ " word2id) values (?, ?, ?, ?, ?)", Linkdef.ANTONYM,
						visible.getSynset().getId(), invisible.getSynset().getId(),
						visible.getWord().getId(), invisible.getWord().getId());
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		});
		WordNetWordRelations relations = new WordNetWordRelations(getDictionaryRepository());
		assertThat(relations.antonyms("visible")).containsExactly("invisible");
		assertThat(relations.antonyms("bicycle")).isEmpty();
	}

	private static Sense firstSense(Word word) {
		return word.getSenses().stream()
				.min((a, b) -> Integer.compare(a.getRank(), b.getRank())).orElseThrow();
	}
}
