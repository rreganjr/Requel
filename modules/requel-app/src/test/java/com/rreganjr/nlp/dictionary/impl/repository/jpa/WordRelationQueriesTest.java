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
package com.rreganjr.nlp.dictionary.impl.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.rreganjr.nlp.dictionary.Linkdef;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * Issue #266: the synonym and antonym lookups behind the corpus finder's {@code WordRelations},
 * without a database (the WordNet-backed behaviour is covered by WordNetWordRelationsIT). Here
 * rather than in dictionary-jpa, whose tests lack the spelling dictionaries the class loads.
 */
class WordRelationQueriesTest {

	private final EntityManager entityManager = mock(EntityManager.class);
	private final Query query = mock(Query.class);
	private JpaDictionaryRepository repository;

	@BeforeEach
	void setUp() {
		repository = mock(JpaDictionaryRepository.class, CALLS_REAL_METHODS);
		doReturn(entityManager).when(repository).getEntityManager();
		when(entityManager.createQuery(anyString())).thenReturn(query);
	}

	@Test
	void synonymsShareASynsetWithAWellRankedSense() {
		when(query.getResultList()).thenReturn(Arrays.asList("Purchase", "get", null, "purchase"));

		Set<String> synonyms = repository.findSynonymLemmas("buy", 2);

		assertThat(synonyms).containsExactly("get", "purchase");
		ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
		verify(entityManager).createQuery(jpql.capture());
		assertThat(jpql.getValue()).contains("other.synset = sense.synset",
				"sense.rank <= :maxRank", "other.word <> sense.word");
		verify(query).setParameter("lemma", "buy");
		verify(query).setParameter("maxRank", 2);
	}

	@Test
	void antonymsFollowTheLexicalLinkFromWellRankedSensesOfTheGivenKinds() {
		when(query.getResultList()).thenReturn(List.of("Decrease"));

		Set<String> antonyms = repository.findAntonymLemmas("increase", 2, List.of("a", "s"));

		assertThat(antonyms).containsExactly("decrease");
		ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
		verify(entityManager).createQuery(jpql.capture());
		assertThat(jpql.getValue()).contains("from Lexlinkref", "lexlink.linkType.id = :linkId",
				"lexlink.fromSynset.pos in :pos");
		verify(query).setParameter("lemma", "increase");
		verify(query).setParameter("linkId", (long) Linkdef.ANTONYM);
		verify(query).setParameter("maxRank", 2);
		verify(query).setParameter("pos", List.of("a", "s"));
	}
}
