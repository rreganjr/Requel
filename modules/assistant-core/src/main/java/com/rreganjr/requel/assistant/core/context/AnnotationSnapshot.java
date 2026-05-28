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
package com.rreganjr.requel.assistant.core.context;

import java.time.Instant;

/**
 * Pack-level view of an annotation. Carries {@code id} and {@code version}
 * so the assistant applicator can reject stale AI output by comparing the
 * persisted annotation's optimistic-lock version against the snapshot the
 * assistant produced findings against.
 */
public record AnnotationSnapshot(Long id, int version, AnnotationKind kind, String text,
		boolean mustBeResolved, boolean resolved, String createdByUsername,
		Instant dateCreated) {
}
