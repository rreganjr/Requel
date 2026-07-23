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
package com.rreganjr.requel.gateway.tracker;

/**
 * Input to {@link RequirementGoalUpserter#upsert(UpsertGoalRequest)} (issue #71): one discrete
 * requirement statement plus the source descriptor needed for provenance and reconciliation.
 *
 * <p>The workflow is source-agnostic — {@code sourceSystem}/{@code sourceRef}/{@code sourceUrl}
 * describe any tracker (Jira, GitHub, Linear, …); Requel never contacts the tracker.</p>
 *
 * <p>Only {@code projectName}, {@code criterionText}, {@code sourceSystem} and {@code sourceRef}
 * are required. When {@code name}, {@code text} or {@code criterionHash} are omitted they are
 * derived deterministically from {@code criterionText} by the upserter, so callers cannot drift
 * from the reconciliation key.</p>
 *
 * @param projectName   target project
 * @param criterionText the requirement / acceptance-criterion statement (source of the derived
 *                      name and hash)
 * @param name          optional explicit goal name (derived from {@code criterionText} if null)
 * @param text          optional goal body (defaults to {@code criterionText} if null)
 * @param sourceSystem  tracker family (e.g. {@code jira}, {@code github})
 * @param sourceRef     source-specific reference to the item/criterion
 * @param sourceUrl     optional human-openable URL to the source item
 * @param criterionRef  optional human-readable criterion reference (e.g. {@code AC-2})
 * @param client        optional external-client id for audit attribution (e.g.
 *                      {@code claude-desktop})
 * @param criterionHash optional precomputed hash (computed from {@code criterionText} if null)
 */
public record UpsertGoalRequest(
        String projectName,
        String criterionText,
        String name,
        String text,
        String sourceSystem,
        String sourceRef,
        String sourceUrl,
        String criterionRef,
        String client,
        String criterionHash
) {

    public UpsertGoalRequest {
        requireText("projectName", projectName);
        requireText("criterionText", criterionText);
        requireText("sourceSystem", sourceSystem);
        requireText("sourceRef", sourceRef);
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    /** Convenience factory for the common case where name/text/hash are derived. */
    public static UpsertGoalRequest of(String projectName, String criterionText,
            String sourceSystem, String sourceRef, String sourceUrl, String criterionRef,
            String client) {
        return new UpsertGoalRequest(projectName, criterionText, null, null, sourceSystem,
                sourceRef, sourceUrl, criterionRef, client, null);
    }
}
