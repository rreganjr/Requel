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
package com.rreganjr.requel.service.stream;

/**
 * Issue #450: whether a user may subscribe to an event-stream target ({@code "Type:id"}).
 * Events carry only the target and a refresh hint, but which entities change, and when, is still
 * the project's business.
 */
@FunctionalInterface
public interface StreamTargetAccess {

    /** The broadcast every signed-in session listens to: some project's content changed. */
    String PROJECT_BROADCAST = "Project:0";

    /** The broadcast for "someone's permissions changed; re-fetch yours". */
    String PERMISSIONS_BROADCAST = "Permissions:0";

    /** Whether {@code username} may subscribe to {@code targetKey}. Never throws. */
    boolean canSubscribe(String username, String targetKey);
}
