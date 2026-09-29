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
 * A project's external source (issue #272). Only the provenance reads return it: no general read
 * (getEntity, getProjectContext, getAnnotations) carries a source or a locator.
 *
 * @param id             the source's id
 * @param system         the lower-cased source family, e.g. jira
 * @param externalId     the source's own identifier, exactly as recorded
 * @param locatorType    URL or PATH, or null
 * @param locator        where the source is, or null
 * @param title          a human title, or null
 * @param contentHash    the content hash of the last version recorded, or null
 * @param lastIngestedAt when it was last recorded
 */
public record ExternalSourceDto(
        Long id,
        String system,
        String externalId,
        String locatorType,
        String locator,
        String title,
        String contentHash,
        Date lastIngestedAt,
        String kind,
        String note
) {
}
