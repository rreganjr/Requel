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
package com.rreganjr.requel.service.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * A node in the project content tree for sidebar navigation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectTreeNodeDto(
        Long id,
        String type,
        String name,
        List<ProjectTreeNodeDto> children
) {
    /** Leaf node constructor — entity with id, no children. */
    public ProjectTreeNodeDto(Long id, String type, String name) {
        this(id, type, name, null);
    }

    /** Group node constructor — no id, has children. */
    public ProjectTreeNodeDto(String type, String name, List<ProjectTreeNodeDto> children) {
        this(null, type, name, children);
    }
}
