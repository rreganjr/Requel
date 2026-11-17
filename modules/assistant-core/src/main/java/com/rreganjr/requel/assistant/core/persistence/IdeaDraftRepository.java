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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data repository for {@link IdeaDraftEntity}.
 */
public interface IdeaDraftRepository extends JpaRepository<IdeaDraftEntity, String> {

	List<IdeaDraftEntity> findByIdeaIdOrderBySortOrder(String ideaId);

	/** Remove an idea's drafts, e.g. before a retry stores a new set. Must run in a transaction. */
	@Modifying
	@Query("delete from IdeaDraftEntity d where d.ideaId = :ideaId")
	int deleteByIdeaId(@Param("ideaId") String ideaId);
}
