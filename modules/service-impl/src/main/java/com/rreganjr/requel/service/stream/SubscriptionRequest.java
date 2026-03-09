package com.rreganjr.requel.service.stream;

/**
 * Request body for adding/removing SSE subscriptions.
 */
public record SubscriptionRequest(String targetType, Long targetId) {

    public String toKey() {
        return targetType + ":" + targetId;
    }
}
