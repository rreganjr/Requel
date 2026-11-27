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
 * #497: one requirement nothing traces to or from.
 *
 * @param kind one of {@link #KINDS}
 */
public record TraceGapDto(String kind, EntityReferenceDto entity) {

    /** A goal no story or use case references. */
    public static final String GOAL_NOT_REALIZED = "GOAL_NOT_REALIZED";
    /** An actor no story or use case lists, no use case has as primary actor, and no step names. */
    public static final String ACTOR_NOT_USED = "ACTOR_NOT_USED";
    /** A story with no goal. */
    public static final String STORY_WITHOUT_GOAL = "STORY_WITHOUT_GOAL";
    /** A story no use case uses. */
    public static final String STORY_NOT_IN_USE_CASE = "STORY_NOT_IN_USE_CASE";
    /** A use case with no goal. */
    public static final String USE_CASE_WITHOUT_GOAL = "USE_CASE_WITHOUT_GOAL";
    /** A scenario no use case reaches, directly or through the scenarios that use it as a step. */
    public static final String SCENARIO_NOT_IN_USE_CASE = "SCENARIO_NOT_IN_USE_CASE";
    /** A stakeholder, user or named, with no goals. */
    public static final String STAKEHOLDER_WITHOUT_GOAL = "STAKEHOLDER_WITHOUT_GOAL";

    /** Every kind, in the order the gaps are listed. */
    public static final List<String> KINDS = List.of(GOAL_NOT_REALIZED, ACTOR_NOT_USED,
            STORY_WITHOUT_GOAL, STORY_NOT_IN_USE_CASE, USE_CASE_WITHOUT_GOAL,
            SCENARIO_NOT_IN_USE_CASE, STAKEHOLDER_WITHOUT_GOAL);
}
