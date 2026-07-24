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
package com.rreganjr.nlp.dictionary.impl.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.EnumSet;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;

import com.rreganjr.nlp.dictionary.Word;

/**
 * An assigned-or-generate id generator for {@link Word} (issue #80).
 *
 * <p>The WordNet dictionary is reference data with a self-referential id graph: a {@code Sense} is
 * keyed by the composite {@code (synsetId, wordId)} and its foreign-key columns are those
 * <em>source</em> ids. {@code Synset} preserves its source id, so if {@code Word} regenerates its
 * id the two halves of every sense key are populated on different bases and the {@code sense -> word}
 * FKs only resolve when the id counter happens to reproduce the source ids (a pristine, in-order
 * {@code word} table). That made the XML import order-dependent.
 *
 * <p>This is a <strong>before-execution</strong> generator: it produces the id <em>before</em> the
 * INSERT, so Hibernate performs a plain insert with the value returned here — the caller-supplied id
 * when the entity already has one (XML import and the {@code DictionarySQLInitializer} SQL dump both
 * carry the dictionary-assigned id), otherwise a freshly generated id for a runtime word created
 * without an id by {@code DatabaseSpellDictionary.addWord}. A post-insert {@code IDENTITY} generator
 * cannot do this: the importer inserts via the legacy {@code Session.save()} path, which reassigns
 * IDENTITY ids and ignores a pre-assigned value, which is what broke the import before this fix.
 *
 * <p>{@link #allowAssignedIdentifiers()} returns {@code true} so that Hibernate treats an
 * id-bearing {@code Word} as a new (transient) row to be inserted with its assigned id, rather than
 * as a detached row to be merged/updated.
 *
 * <p>Generated (no-id) runtime words are allocated in a reserved high id range (at or above
 * {@link #RUNTIME_ID_FLOOR}) that sits above the dictionary's assigned source ids (WordNet is
 * ~150k words). This is what makes the import order-independent in <em>both</em> directions: not
 * only do assigned ids survive the import, but a runtime word added <em>before</em> the dictionary
 * is loaded (e.g. a spell-dictionary add on a fresh database) can no longer occupy a low id that a
 * later import needs, which would otherwise be a primary-key collision. It uses {@code max(wordid)}
 * against the current transaction's connection; the runtime add-word use case (a user resolving a
 * spelling issue) is effectively serialized and very low volume, not high-concurrency allocation.
 */
public class AssignedOrGeneratedWordIdGenerator implements BeforeExecutionGenerator {

	private static final long serialVersionUID = 1L;

	/**
	 * Lowest id handed out to a runtime-generated (no-id) word. Chosen to sit well above the
	 * dictionary's assigned source-id range (WordNet is ~150k words) so a runtime word can never
	 * collide with a word the dictionary import will insert with its own assigned id. See #80.
	 */
	public static final long RUNTIME_ID_FLOOR = 1_000_000_000L;

	@Override
	public EnumSet<EventType> getEventTypes() {
		return EventTypeSets.INSERT_ONLY;
	}

	/**
	 * Allow a caller-supplied identifier so Hibernate treats an id-bearing Word as a new row
	 * (insert with the assigned id) instead of a detached row (merge/update).
	 */
	@Override
	public boolean allowAssignedIdentifiers() {
		return true;
	}

	@Override
	public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue,
			EventType eventType) {
		Long assignedId = ((Word) owner).getId();
		if (assignedId != null) {
			// Import / SQL dump: preserve the dictionary-assigned id so the composite sense -> word
			// FKs resolve regardless of insert order.
			return assignedId;
		}
		// Runtime add-word: no id supplied, allocate the next one.
		return nextWordId(session);
	}

	private Long nextWordId(SharedSessionContractImplementor session) {
		long currentMax = session.doReturningWork(connection -> {
			try (PreparedStatement ps =
					connection.prepareStatement("select coalesce(max(wordid), 0) from word");
					ResultSet rs = ps.executeQuery()) {
				rs.next();
				return rs.getLong(1);
			}
		});
		// Allocate in the reserved high range so a runtime word never lands on an id the dictionary
		// import will assign (which would be a PK collision if the word was added before the import).
		return Math.max(currentMax, RUNTIME_ID_FLOOR - 1) + 1L;
	}
}
