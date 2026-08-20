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

import com.rreganjr.command.Command;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.project.command.EditProjectOrDomainEntityCommand;
import com.rreganjr.requel.service.stream.StreamEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Publishes the SSE change events for an executed command. Shared by the HTTP {@link CommandController}
 * and the {@link com.rreganjr.requel.service.gateway.InProcessCommandGateway} so an association made
 * over either path refreshes open browser sessions identically (issue #178), instead of each path
 * carrying its own copy of the reflection-based targeting block.
 */
@Component
public class CommandEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CommandEventPublisher.class);

    /** Sentinel project ID used as a broadcast channel for "any project changed". */
    private static final long PROJECT_BROADCAST_ID = 0L;

    private final StreamEventPublisher streamEventPublisher;

    public CommandEventPublisher(StreamEventPublisher streamEventPublisher) {
        this.streamEventPublisher = streamEventPublisher;
    }

    /**
     * Publish every SSE event for an executed command: the Project:0 broadcast (all sidebar
     * sessions, never excluded), a targeted event for the primary result entity, and a targeted
     * event for the secondary entity (skipped when it resolves to the same type+id as the primary).
     * Targeted events skip {@code excludeSessionId} — the originating HTTP session — so it does not
     * reload the form it just edited; pass {@code null} to exclude nobody (the in-process gateway
     * has no HTTP session).
     */
    public void publish(Command command, Object primaryResult, Object secondaryResult,
                        String excludeSessionId) {
        publishProjectChangedIfScoped(command);
        TargetRef primary = publishTargeted(primaryResult, excludeSessionId, null);
        publishTargeted(secondaryResult, excludeSessionId, primary);
    }

    private record TargetRef(String type, long id) {}

    /**
     * If the DTO has an {@code id()} accessor (all entity record DTOs do), publish a targeted SSE
     * event so any editor subscribed to that entity refreshes. The entity type is the DTO simple
     * name with a trailing "Dto" stripped ({@code GoalDto} -> {@code "Goal"}). Returns the
     * (type, id) that was published, or {@code null} when the argument is not a targetable DTO.
     * Skips publishing when the resolved (type, id) equals {@code skipIfSameAs} (the primary
     * result), so an association whose child extractor happens to resolve to the same entity never
     * fires two identical events.
     */
    private TargetRef publishTargeted(Object result, String excludeSessionId, TargetRef skipIfSameAs) {
        if (result == null) return null;
        try {
            Method idMethod = result.getClass().getMethod("id");
            Object idValue = idMethod.invoke(result);
            if (!(idValue instanceof Long entityId)) return null;
            String simpleName = result.getClass().getSimpleName();
            String entityType = simpleName.endsWith("Dto")
                    ? simpleName.substring(0, simpleName.length() - 3)
                    : simpleName;
            TargetRef self = new TargetRef(entityType, entityId);
            if (self.equals(skipIfSameAs)) return self;
            streamEventPublisher.publishTargetUpdate(entityType, entityId,
                    Map.of("type", "refresh"), excludeSessionId);
            return self;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // Not an entity DTO with an id() accessor — skip silently
            return null;
        } catch (Exception e) {
            log.warn("Failed to publish entity-changed SSE event: {}", e.getMessage());
            return null;
        }
    }

    /**
     * If the command is project-scoped, publish a broadcast Project event so all sidebar sessions
     * subscribed to {@code Project:0} reload their counts. This broadcast is never filtered by
     * originating session — the acting session's own counts change too. Non-project commands
     * (e.g. user management) are silently skipped.
     */
    private void publishProjectChangedIfScoped(Command command) {
        try {
            Object entity = null;
            String discriminant = "neither";
            if (command instanceof ProjectScopedCommand psc) {
                entity = psc.getProject();
                discriminant = "ProjectScopedCommand";
            } else if (command instanceof EditProjectOrDomainEntityCommand podCmd) {
                entity = podCmd.getProjectOrDomain();
                discriminant = "EditProjectOrDomainEntityCommand";
            }
            if (log.isDebugEnabled()) {
                log.debug("publishProjectChangedIfScoped command={} discriminant={} entity={} -> {}",
                        command.getClass().getSimpleName(), discriminant,
                        entity != null ? entity.getClass().getSimpleName() : "null",
                        entity != null ? "BROADCAST" : "skip");
            }
            if (entity != null) {
                streamEventPublisher.publishTargetUpdate("Project", PROJECT_BROADCAST_ID,
                        Map.of("type", "refresh"));
            }
        } catch (Exception e) {
            log.warn("Failed to publish project-changed SSE event: {}", e.getMessage(), e);
        }
    }
}
