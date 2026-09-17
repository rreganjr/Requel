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
package com.rreganjr.requel.service.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.rreganjr.command.Command;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.service.stream.StreamEventPublisher;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Issue #276: a permission grant has to reach the session whose permissions changed, and that
 * session usually belongs to someone other than the person who made the grant.
 *
 * <p>The event is deliberately its own target rather than a reuse of the {@code Project:0}
 * broadcast. That broadcast fires on every project-scoped command, so a client invalidating its
 * permissions on it would re-fetch them on every goal edit and scenario save by anyone. These tests
 * pin both halves: a stakeholder write publishes it, and an ordinary project-scoped command does
 * not.
 */
class CommandEventPublisherPermissionsTest {

    @Test
    void aStakeholderPermissionWritePublishesThePermissionsBroadcast() {
        StreamEventPublisher stream = mock(StreamEventPublisher.class);
        CommandEventPublisher publisher = new CommandEventPublisher(stream);

        publisher.publish(mock(EditUserStakeholderCommand.class), null, null, null);

        verify(stream, times(1)).publishTargetUpdate(eq("Permissions"), eq(0L),
                eq(Map.of("type", "refresh")));
    }

    /**
     * The payload says only "refresh". It names no user and no permission, so a client learns
     * nothing from the event itself — it asks the server what it may do and is told only what it is
     * already entitled to know.
     */
    @Test
    void thePermissionsEventCarriesNoPermissionData() {
        StreamEventPublisher stream = mock(StreamEventPublisher.class);
        CommandEventPublisher publisher = new CommandEventPublisher(stream);

        publisher.publish(mock(EditUserStakeholderCommand.class), null, null, null);

        verify(stream).publishTargetUpdate(eq("Permissions"), anyLong(),
                eq(Map.of("type", "refresh")));
    }

    @Test
    void anUnrelatedCommandDoesNotPublishIt() {
        StreamEventPublisher stream = mock(StreamEventPublisher.class);
        CommandEventPublisher publisher = new CommandEventPublisher(stream);

        publisher.publish(mock(EditGoalCommand.class), null, null, null);

        verify(stream, never()).publishTargetUpdate(eq("Permissions"), anyLong(), any());
    }

    /**
     * Never filtered by originating session: excluding the acting session is right for "do not
     * reload the form you just edited", and wrong here, where the whole point is reaching sessions
     * other than the actor's. The three-argument overload is the unfiltered one.
     */
    @Test
    void thePermissionsEventIsNeverFilteredByOriginatingSession() {
        StreamEventPublisher stream = mock(StreamEventPublisher.class);
        CommandEventPublisher publisher = new CommandEventPublisher(stream);

        publisher.publish(mock(EditUserStakeholderCommand.class), null, null, "session-of-the-actor");

        verify(stream).publishTargetUpdate(eq("Permissions"), anyLong(), any());
        verify(stream, never()).publishTargetUpdate(eq("Permissions"), anyLong(), any(), anyString());
    }
}
