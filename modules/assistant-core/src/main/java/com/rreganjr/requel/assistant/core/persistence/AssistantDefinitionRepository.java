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
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Issue #260: Spring Data repository for {@link AssistantDefinitionEntity}. */
public interface AssistantDefinitionRepository
		extends JpaRepository<AssistantDefinitionEntity, Long> {

	/** The bundled definitions and the project's own, in one query (the per-project cache fill). */
	@Query("select d from AssistantDefinitionEntity d"
			+ " where d.projectId is null or d.projectId = :projectId")
	List<AssistantDefinitionEntity> findVisibleTo(@Param("projectId") Long projectId);

	/** The bundled definitions alone. */
	List<AssistantDefinitionEntity> findByProjectIdIsNull();

	Optional<AssistantDefinitionEntity> findByDefinitionKeyAndProjectIdIsNull(String key);

	Optional<AssistantDefinitionEntity> findByDefinitionKeyAndProjectId(String key, Long projectId);

	/** The project's own definitions (never the bundled ones). */
	@Modifying
	@Query("delete from AssistantDefinitionEntity d where d.projectId = :projectId")
	int deleteByProjectId(@Param("projectId") Long projectId);
}
