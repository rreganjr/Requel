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

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;

/**
 * SSE streaming endpoints: open stream, add/remove subscriptions, graceful close.
 */
@RestController
@RequestMapping("/api/events/stream")
public class StreamController {

    private final StreamService streamService;

    public StreamController(StreamService streamService) {
        this.streamService = streamService;
    }

    /**
     * GET /api/events/stream?subscribe=Project:1&subscribe=Goal:7
     * Opens SSE connection, creates session, returns SseEmitter.
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter openStream(
            @RequestParam(value = "subscribe", required = false) List<String> subscriptions,
            @RequestParam(value = "sessionId", required = false) String existingSessionId) {
        List<String> subs = subscriptions != null ? subscriptions : new ArrayList<>();
        // JWT expiry is handled by the filter; pass 0 to skip server-side expiry scheduling for now
        // TODO: extract JWT exp claim and pass it here
        return streamService.createStream(existingSessionId, 0L, subs);
    }

    /**
     * POST /api/events/stream/subscriptions — add subscription to existing session.
     */
    @PostMapping("/subscriptions")
    public ResponseEntity<Void> addSubscription(
            @RequestHeader("X-Session-Id") String sessionId,
            @RequestBody SubscriptionRequest request) {
        streamService.addSubscription(sessionId, request.toKey());
        return ResponseEntity.ok().build();
    }

    /**
     * DELETE /api/events/stream/subscriptions — remove subscription from session.
     */
    @DeleteMapping("/subscriptions")
    public ResponseEntity<Void> removeSubscription(
            @RequestHeader("X-Session-Id") String sessionId,
            @RequestBody SubscriptionRequest request) {
        streamService.removeSubscription(sessionId, request.toKey());
        return ResponseEntity.ok().build();
    }

    /**
     * DELETE /api/events/stream/connection — graceful server-side close.
     */
    @DeleteMapping("/connection")
    public ResponseEntity<Void> closeConnection(
            @RequestHeader("X-Session-Id") String sessionId) {
        streamService.closeConnection(sessionId);
        return ResponseEntity.ok().build();
    }
}
