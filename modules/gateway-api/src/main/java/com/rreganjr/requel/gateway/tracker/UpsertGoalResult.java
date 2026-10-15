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

import java.util.List;

/**
 * Outcome of a single {@link RequirementGoalUpserter#upsert(UpsertGoalRequest)} call, suitable for
 * the client's created-vs-updated report. Since #272 there is no provenance note, so #71's
 * {@code noteId} is gone; {@code status} says what happened.
 *
 * @param goalId        the created, updated, unchanged or conflicting goal's id; null when
 *                      AMBIGUOUS
 * @param goalName      the goal's name (may be a disambiguated form on a name collision)
 * @param created       {@code true} if a new goal was created
 * @param criterionHash the hash of the criterion text, which change is detected against
 * @param status        CREATED, UNCHANGED, UPDATED, CONFLICT or AMBIGUOUS (see UpsertFromSource)
 * @param fragment      the fragment the goal is linked by: the criterionRef, or
 *                      {@code hash:<12>} without one
 * @param issueId       the conflict issue raised on the goal, when CONFLICT
 * @param candidates    the goals that came from the criterion, when AMBIGUOUS: pass one as
 *                      goalId
 */
public record UpsertGoalResult(
        Long goalId,
        String goalName,
        boolean created,
        String criterionHash,
        String status,
        String fragment,
        Long issueId,
        List<Long> candidates
) {
}
