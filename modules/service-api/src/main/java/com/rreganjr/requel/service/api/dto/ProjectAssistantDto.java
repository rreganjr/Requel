/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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

/**
 * One assistant a project can switch on and off (issue #268).
 *
 * @param assistantId the id EditProjectAssistantSetting takes
 * @param displayName the name to show
 * @param enabled whether it runs in the project
 * @param group the heading its switch shows under, e.g. "Lexical checks" or "AI review" (#263)
 */
public record ProjectAssistantDto(
        String assistantId,
        String displayName,
        boolean enabled,
        String group
) {
}
