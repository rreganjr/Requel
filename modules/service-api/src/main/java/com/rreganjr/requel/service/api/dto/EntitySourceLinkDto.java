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

import java.util.Date;

/**
 * One entity's link to a fragment of a source (issue #272), with the change on each side worked
 * out when read.
 *
 * @param id                the link's id
 * @param source            the source
 * @param relation          DERIVED_FROM
 * @param entityType        the entity's type, e.g. Goal
 * @param entityId          the entity's id
 * @param entityName        the entity's name, or null if it can't be read
 * @param fragment          the fragment, or null for the whole source
 * @param ingestedAt        when the link was last written by an ingest
 * @param notInLatestSource the source has a newer version than the one this fragment was last
 *                          seen in: the fragment may have been removed or renamed upstream
 * @param editedSinceIngest the entity changed in Requel since the last ingest
 */
public record EntitySourceLinkDto(
        Long id,
        ExternalSourceDto source,
        String relation,
        String entityType,
        Long entityId,
        String entityName,
        String fragment,
        Date ingestedAt,
        boolean notInLatestSource,
        boolean editedSinceIngest
) {
}
