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
package com.rreganjr.requel.mcp;

import java.util.List;
import java.util.Map;

import com.rreganjr.requel.service.api.dto.AnnotationsDto;
import com.rreganjr.requel.service.api.dto.EntityReferenceDto;
import com.rreganjr.requel.service.api.dto.GlossaryTermDto;
import com.rreganjr.requel.service.api.dto.OpenIssueDto;
import com.rreganjr.requel.service.api.dto.ProjectDto;
import com.rreganjr.requel.service.api.dto.ProjectTreeNodeDto;

public interface ProjectQueryGateway {

	List<ProjectDto> listProjects();

	ProjectDto getProject(String projectName);

	List<ProjectTreeNodeDto> getProjectTree(String projectName);

	/** The project's glossary terms (summary form). */
	List<GlossaryTermDto> getGlossaryTerms(String projectName);

	/** Unresolved issues across all of the project's entities. */
	List<OpenIssueDto> getOpenIssues(String projectName);

	/** Notes and issues attached to one annotatable entity in the project. */
	AnnotationsDto getAnnotations(String projectName, String entityType, long entityId);

	/**
	 * Read one entity (Goal / Story / Actor / UseCase / Scenario / GlossaryTerm) by its
	 * domain-interface simple name and id, returning the corresponding detail DTO.
	 */
	Object getEntity(String projectName, String entityType, long entityId);

	/**
	 * Read an entity's related entities, grouped by relationship name (e.g. for a Goal:
	 * {@code relationsFromThisGoal}, {@code relationsToThisGoal}, {@code referencedBy}). Each
	 * neighbour is an {@link EntityReferenceDto} (type + id + name).
	 */
	Map<String, List<EntityReferenceDto>> getEntityNeighbors(String projectName, String entityType,
			long entityId);
}
