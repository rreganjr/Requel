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

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data repository for {@link IdeaEntity}.
 */
public interface IdeaRepository extends JpaRepository<IdeaEntity, String> {

	List<IdeaEntity> findByProjectIdOrderByCreatedAtDesc(Long projectId);

	/**
	 * Take the idea for one generation attempt, atomically: only a pending, failed or skipped
	 * idea, or one stuck generating since before {@code staleBefore}, can be taken, so two
	 * concurrent retries start one attempt. Must run in a transaction.
	 *
	 * @return 1 when the caller now owns the attempt, 0 when it did not get it
	 */
	@Modifying
	@Query("update IdeaEntity i set i.status = 'GENERATING', i.attempts = i.attempts + 1,"
			+ " i.lastRunId = :runId, i.errorSummary = null, i.updatedAt = :now"
			+ " where i.id = :id and (i.status in ('PENDING', 'FAILED', 'SKIPPED')"
			+ " or (i.status = 'GENERATING' and i.updatedAt < :staleBefore))")
	int claim(@Param("id") String id, @Param("runId") String runId, @Param("now") Instant now,
			@Param("staleBefore") Instant staleBefore);
}
