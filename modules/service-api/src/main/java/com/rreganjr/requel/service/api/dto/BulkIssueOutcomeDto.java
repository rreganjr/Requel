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

/**
 * #507: what a bulk action did to one issue.
 *
 * @param outcome    {@code DONE}, {@code SKIPPED_NOT_APPLICABLE}, {@code SKIPPED_AMBIGUOUS},
 *                   {@code REFUSED} (not permitted) or {@code FAILED}
 * @param reason     why it was skipped, refused or failed; absent when done
 * @param positionId the position the issue was resolved with, for IGNORE and APPLY_FIX
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BulkIssueOutcomeDto(Long issueId, String outcome, String reason, Long positionId) {

    public static final String DONE = "DONE";
    public static final String SKIPPED_NOT_APPLICABLE = "SKIPPED_NOT_APPLICABLE";
    public static final String SKIPPED_AMBIGUOUS = "SKIPPED_AMBIGUOUS";
    public static final String REFUSED = "REFUSED";
    public static final String FAILED = "FAILED";
}
