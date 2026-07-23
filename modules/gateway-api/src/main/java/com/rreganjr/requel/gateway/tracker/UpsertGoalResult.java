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
 * Outcome of a single {@link RequirementGoalUpserter#upsert(UpsertGoalRequest)} call (issue #71),
 * suitable for the client's created-vs-updated report.
 *
 * @param goalId        the created or updated goal's id
 * @param goalName      the goal's final name (may be a disambiguated form on a name collision)
 * @param noteId        the provenance note's id
 * @param created       {@code true} if a new goal was created, {@code false} if an existing goal
 *                      was updated in place
 * @param criterionHash the reconciliation key recorded in the provenance note
 */
public record UpsertGoalResult(
        Long goalId,
        String goalName,
        Long noteId,
        boolean created,
        String criterionHash
) {
}
