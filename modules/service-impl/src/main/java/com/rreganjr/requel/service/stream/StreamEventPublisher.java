package com.rreganjr.requel.service.stream;

/**
 * Interface for publishing events to subscribed SSE sessions.
 * Called by domain code (command handlers, NLP assistants) to push
 * target updates and deletes. Decouples publishing from SSE transport.
 */
public interface StreamEventPublisher {

    void publishTargetUpdate(String targetType, Long targetId, Object payload);

    void publishTargetDeleted(String targetType, Long targetId);
}
