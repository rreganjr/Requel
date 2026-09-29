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

import java.util.List;

/**
 * One of a project's sources as the project sees it (issue #273): the source, how many entities
 * were derived from and cite it, and the sources directly above and below it in precedence.
 *
 * @param source       the source
 * @param derivedCount entities with a DERIVED_FROM link to it
 * @param citedByCount entities with a CITES link to it
 * @param defersTo     the sources it defers to directly
 * @param outranks     the sources that defer to it directly
 */
public record ProjectSourceDto(
        ExternalSourceDto source,
        long derivedCount,
        long citedByCount,
        List<SourceRefDto> defersTo,
        List<SourceRefDto> outranks
) {
}
