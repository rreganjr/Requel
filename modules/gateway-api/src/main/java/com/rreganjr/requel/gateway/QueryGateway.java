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
package com.rreganjr.requel.gateway;

import java.util.List;
import java.util.Map;

import com.rreganjr.requel.service.api.dto.AnnotationsDto;
import com.rreganjr.requel.service.api.dto.EntityReferenceDto;
import com.rreganjr.requel.service.api.dto.GlossaryTermDto;
import com.rreganjr.requel.service.api.dto.OpenIssueDto;
import com.rreganjr.requel.service.api.dto.ProjectDto;
import com.rreganjr.requel.service.api.dto.ProjectTreeNodeDto;

/**
 * Read side of the gateway: authorized, DTO-shaped project queries for external clients. This is
 * the canonical home for the read contract that {@code mcp-server}'s {@code ProjectQueryGateway}
 * is folded into (Slice 4); the in-process and REST-backed implementations satisfy the same
 * interface, mirroring the {@link CommandGateway} split on the write side.
 */
public interface QueryGateway {

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
     * An entity's related entities grouped by relationship name. Each neighbour is an
     * {@link EntityReferenceDto} (type + id + name).
     */
    Map<String, List<EntityReferenceDto>> getEntityNeighbors(String projectName, String entityType,
            long entityId);

    /**
     * Find a project's goals / stories / actors / use cases / scenarios / glossary terms whose
     * name contains {@code query} (case-insensitive), as {@link EntityReferenceDto}s.
     */
    List<EntityReferenceDto> searchProjectEntities(String projectName, String query);

    /**
     * A composite context bundle for a project: its summary, content tree, glossary, and open
     * issues — keyed {@code project} / {@code tree} / {@code glossary} / {@code openIssues}.
     */
    Map<String, Object> getProjectContext(String projectName);
}
