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
 * Read DTO for a position option on an issue.
 * <p>
 * positionType is the position's persisted discriminator with its package stripped and a trailing
 * "Impl" removed — "Position" for a plain position, otherwise its subtype ("AddActorPosition",
 * "AddGlossaryTermPosition", "AddWordToDictionaryPosition", "ChangeSpellingPosition") — so the UI
 * can label and dispatch the correct ResolveIssue command variant. It is stable across runs and
 * builds, and never carries a generated proxy class name; see PositionTypes (issue #253).
 */
public record PositionDto(
        Long id,
        int version,
        String text,
        String createdBy,
        String positionType,
        List<ArgumentDto> arguments
) {
}
