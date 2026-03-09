package com.rreganjr.requel.service.stream;

import org.springframework.stereotype.Component;

/**
 * Local implementation of StreamEventPublisher that pushes directly
 * to SseEmitter instances via StreamService. For multi-instance deployments,
 * this could be replaced with a Redis pub/sub implementation.
 */
@Component
public class StreamEventPublisherImpl implements StreamEventPublisher {

    private final StreamService streamService;

    public StreamEventPublisherImpl(StreamService streamService) {
        this.streamService = streamService;
    }

    @Override
    public void publishTargetUpdate(String targetType, Long targetId, Object payload) {
        streamService.pushToSubscribedSessions(targetType, targetId, payload);
    }

    @Override
    public void publishTargetDeleted(String targetType, Long targetId) {
        streamService.pushToSubscribedSessions(targetType, targetId, null);
    }
}
