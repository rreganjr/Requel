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

/**
 * Client-side validation bounds, each one mirroring a constraint the server actually
 * enforces (issue #132).
 *
 * The rule for this file: **every entry names its backend source.** A client-side cap
 * with no server-side counterpart rejects input the server would have accepted, which
 * is worse than no cap at all — the user is blocked by a rule that does not exist. So
 * a bound only appears here once something in `modules/` enforces it, and the comment
 * says where, so the next person can diff this against the annotations instead of
 * guessing which numbers are real.
 *
 * What is deliberately ABSENT: a max length for artifact `text`. `AbstractTextEntity.getText()`
 * is `@Lob` (`longtext`), so there is no server-side bound to mirror — and a cap here with no
 * server-side counterpart is the exact failure this file exists to prevent. The `name` bound
 * below arrived with #171, which also made these DTO constraints actually run: before it, nothing
 * in the backend invoked a `Validator` at all.
 */

/**
 * Maximum password length.
 *
 * Source: `UserImpl.MAX_PASSWORD_LENGTH` (user-jpa). `UserImpl.isValidPassword` accepts
 * a password that is non-blank and no longer than this, and that is the *only* password
 * rule the server applies — no complexity requirement, no minimum beyond non-blank. The
 * client mirrors exactly that, so a password the user is allowed to set is never
 * rejected by the form first. A real password policy is a product decision, not
 * something to invent here.
 */
export const PASSWORD_MAX_LENGTH = 128;

/**
 * Minimum number of roles a user must hold.
 *
 * Source: `UserImpl:385`, `@Size(min = 1)` on the roles collection — the one size
 * constraint the backend already had before #171.
 */
export const USER_ROLES_MIN = 1;

/**
 * Maximum length of an artifact `name`.
 *
 * Source: `ValidationLimits.ARTIFACT_NAME_MAX` (platform-core), applied as
 * `@Size(max = 255)` to the `name` of every artifact entity — goals, stories, actors, use cases,
 * scenarios, terms, reports, stakeholders, projects, teams, tag categories, organizations, users —
 * and to the matching `Edit*Input` DTO fields. It is not an invented product limit: every one of
 * those columns is already `varchar(255)`, so this is the database's own bound made visible to the
 * user instead of surfacing as a driver error.
 *
 * Also applies to `username`, which shares the 255-character column on `users`.
 */
export const ARTIFACT_NAME_MAX_LENGTH = 255;
