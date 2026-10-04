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
package com.rreganjr.requel.project;

/**
 * Issue #264: the entity type of the {@code AssistantDefinition[Edit]} stakeholder permission,
 * which lets a project member read and author the project's assistant definitions. A marker:
 * the definitions themselves are {@code com.rreganjr.requel.assistant.core.definition}
 * records, which project-domain can't see, and a permission needs a {@code Class}.
 *
 * <p>A definition is prompt text a project member controls. The guarantees come from the fixed
 * output envelope, Requel-side validation and the command path, not from trusting the author.
 */
public interface AssistantDefinition {
}
