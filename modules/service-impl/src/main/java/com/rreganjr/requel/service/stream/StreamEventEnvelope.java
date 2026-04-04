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

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * SSE event envelope. All SSE data is sent as JSON in data: lines.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StreamEventEnvelope(
        String eventType,
        String targetType,
        Long targetId,
        Object payload
) {
    public static StreamEventEnvelope session(String sessionId) {
        return new StreamEventEnvelope("Session", null, null,
                java.util.Map.of("sessionId", sessionId));
    }

    public static StreamEventEnvelope data(String targetType, Long targetId, Object payload) {
        return new StreamEventEnvelope("Data", targetType, targetId, payload);
    }

    public static StreamEventEnvelope targetDeleted(String targetType, Long targetId) {
        return new StreamEventEnvelope("TargetDeleted", targetType, targetId, null);
    }

    public static StreamEventEnvelope sessionExpired() {
        return new StreamEventEnvelope("SESSION_EXPIRED", null, null, null);
    }
}
