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
package com.rreganjr.requel.assistant.core.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link AssistantFindingEntity}.
 */
public interface AssistantFindingRepository
		extends JpaRepository<AssistantFindingEntity, String> {

	/**
	 * Lookup by idempotency key. The applicator uses this to decide whether a
	 * new {@code AnnotationAction} produces a fresh finding or updates an
	 * existing one (touch / supersede / auto-resolve / drop).
	 */
	Optional<AssistantFindingEntity> findByIdempotencyKey(String idempotencyKey);

	/**
	 * Findings whose linked annotation matches; used when annotation events
	 * fire (e.g. a user manually resolves an issue) so the applicator can mark
	 * the finding {@code MANUALLY_RESOLVED}.
	 */
	List<AssistantFindingEntity> findByAppliedAnnotationId(Long appliedAnnotationId);

	/**
	 * All findings for an assistant on a target in a given state. Used by the
	 * applicator to reconcile a run's findings against previously recorded ones
	 * (auto-resolving stale {@code ACTIVE} findings the new run no longer reports).
	 */
	List<AssistantFindingEntity> findByAssistantIdAndTargetTypeAndTargetIdAndState(
			String assistantId, String targetType, Long targetId, String state);
}
