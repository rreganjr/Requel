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
