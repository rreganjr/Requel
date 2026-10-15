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
        publishTargetUpdate(targetType, targetId, payload, null);
    }

    @Override
    public void publishTargetUpdate(String targetType, Long targetId, Object payload,
                                    String excludeSessionId) {
        streamService.pushToSubscribedSessions(targetType, targetId, payload, excludeSessionId);
    }

    @Override
    public void publishTargetDeleted(String targetType, Long targetId) {
        streamService.pushToSubscribedSessions(targetType, targetId, null);
    }
}
