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
 * #497: a project's traceability: what references each goal, and the requirements nothing traces
 * to or from. Computed from the model's links; gaps are shown, not raised as issues.
 *
 * @param goals every goal in creation order, with what references it
 * @param gaps one entry per gap, grouped by kind in {@link TraceGapDto#KINDS} order, each kind in
 *             creation order
 */
public record TraceabilityDto(List<GoalTraceDto> goals, List<TraceGapDto> gaps) {
}
