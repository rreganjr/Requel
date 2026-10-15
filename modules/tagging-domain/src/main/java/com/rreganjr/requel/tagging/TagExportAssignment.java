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
package com.rreganjr.requel.tagging;

/**
 * A single tag assignment to export: the tagged entity (opaque {@code Object}), its registry
 * discriminator, and the by-name token. Neutral carrier returned by {@link TagExportProvider} so the
 * project export can build its XML view without referencing a tag JPA type.
 *
 * @param entity the tagged entity instance (marshalled elsewhere in the project graph)
 * @param entityType the registry discriminator, e.g. {@code "Goal"}
 * @param token the by-name tag token ({@code category:value[color]})
 * @author ron
 */
public record TagExportAssignment(Object entity, String entityType, String token) {
}
