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
package com.rreganjr.validator;

/**
 * Shared bounds for bean validation constraints (issue #171).
 *
 * <p>The point of putting these in one place is that the same number has to appear on both sides of
 * every constraint — on the JPA entity, where it is enforced at flush time, and on the input DTO,
 * where {@code CommandInputValidator} enforces it before the command runs — and a third time in the
 * Angular client, whose {@code shared/validation-limits.ts} mirrors it so the form reports an
 * over-long value inline instead of waiting for a round trip. Three copies of a literal 255 across
 * three languages is a number that drifts. One constant is a number the client file can name.
 *
 * <p>These are constraint bounds, not schema. The database column widths they match are owned by
 * Flyway (`modules/requel-app/src/main/resources/db/migration/`), and nothing here changes DDL.
 */
public final class ValidationLimits {

    /**
     * Maximum length of an artifact {@code name}.
     *
     * <p>255 is not chosen, it is <em>observed</em>: every artifact name column in
     * {@code V1__init.sql} is already {@code varchar(255)} — goals, stories, actors, use cases,
     * scenarios, terms, reports, stakeholders, projects, teams, organizations, users — as is
     * {@code tag_category.name} in {@code V13__tagging.sql}. Matching the DDL is what makes this
     * constraint free of a migration: it converts what is currently a driver-level truncation error
     * into a field-level validation message, and changes nothing about what the database accepts.
     *
     * <p>Do not raise this past ~767 without checking indexes.
     * {@code goals}, {@code stories}, {@code actors}, {@code usecases}, {@code scenarios},
     * {@code terms} and {@code stakeholders} all carry a {@code UNIQUE KEY (projectordomain_id,
     * name)}; at {@code varchar(255)} utf8mb4 that is 1020 bytes, comfortably inside InnoDB's
     * 3072-byte limit, but a wider column would not be.
     *
     * <p>Deliberately absent: any bound on artifact {@code text}.
     * {@code AbstractTextEntity.getText()} is {@code @Lob} ({@code longtext}), so a cap here would
     * be invented rather than mirrored — and a client-side cap with no server-side counterpart
     * rejects input the server would have accepted.
     */
    public static final int ARTIFACT_NAME_MAX = 255;

    /**
     * Message template for a length violation. {@code {max}} is interpolated by the validator, so
     * this stays correct if {@link #ARTIFACT_NAME_MAX} changes.
     *
     * <p>Worded to match the hand-written entity messages already in the codebase ("a unique name is
     * required.", "one or more roles must be selected.") rather than the bean-validation default
     * ("size must be between 0 and 255"), because these strings are rendered under the field in the
     * UI, not read in a log.
     */
    public static final String LENGTH_MESSAGE = "must be {max} characters or fewer.";

    private ValidationLimits() {
        // constants only
    }
}
