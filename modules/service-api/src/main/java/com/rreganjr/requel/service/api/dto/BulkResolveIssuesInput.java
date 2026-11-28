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

import com.rreganjr.requel.service.api.CommandDescription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * #507: one action on many issues of a project.
 *
 * @param issueIds 1 to {@value #MAX_ISSUES} issue ids of the project; duplicates count once
 * @param action   {@code IGNORE} (resolve with the issue's Ignore position), {@code APPLY_FIX}
 *                 (resolve with its one model-changing position), {@code SET_SEVERITY} or
 *                 {@code MARK_ADVISORY} (clear must-resolve)
 * @param severity LOW, MEDIUM or HIGH for {@code SET_SEVERITY}; absent for the other actions
 */
@CommandDescription(value = "Does one action to up to 500 of a project's issues at once (issueIds,"
        + " from getOpenIssues) and reports what happened to each. action is IGNORE (resolve with the"
        + " issue's IgnorePosition, which also keeps the assistant from raising that finding on the"
        + " entity again), APPLY_FIX (resolve with the issue's one position that changes the model,"
        + " such as a ChangeSpellingPosition; an issue with several such positions is skipped as"
        + " ambiguous, so pick one with ResolveIssue), SET_SEVERITY (severity LOW, MEDIUM or HIGH,"
        + " required for this action only) or MARK_ADVISORY (no longer must be resolved). Each issue"
        + " goes through ResolveIssue or EditIssue with their checks, separately, so one refused or"
        + " failed issue doesn't undo the others. Every issue id must be an issue of the project,"
        + " or nothing is done. The result counts done, skipped, refused and failed issues and gives"
        + " each issue's outcome: DONE, SKIPPED_NOT_APPLICABLE, SKIPPED_AMBIGUOUS, REFUSED or"
        + " FAILED, with a reason.",
        authorization = "Annotation[Edit], plus each issue's own checks (Edit on what a fix changes)")
public record BulkResolveIssuesInput(
        @NotBlank String projectName,
        @NotNull @NotEmpty @Size(max = BulkResolveIssuesInput.MAX_ISSUES) List<@NotNull Long> issueIds,
        @NotBlank @Pattern(regexp = "IGNORE|APPLY_FIX|SET_SEVERITY|MARK_ADVISORY",
                message = "must be one of IGNORE, APPLY_FIX, SET_SEVERITY, MARK_ADVISORY")
        String action,
        @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "must be one of LOW, MEDIUM, HIGH")
        String severity
) {
    /** The most issues one request may name. */
    public static final int MAX_ISSUES = 500;

    public static final String IGNORE = "IGNORE";
    public static final String APPLY_FIX = "APPLY_FIX";
    public static final String SET_SEVERITY = "SET_SEVERITY";
    public static final String MARK_ADVISORY = "MARK_ADVISORY";
}
