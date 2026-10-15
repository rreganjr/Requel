/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

/**
 * Endpoint and auth tests for {@link StreamController}.
 *
 * Scope: routing, content-type, and ownership enforcement (403). Actual SSE
 * event delivery and subscription behaviour are covered by Playwright end-to-end
 * tests — MockMvc buffers the full response body so it cannot consume a live
 * SSE stream.
 *
 * Collaborator mocked:
 * - {@code StreamService} — all session management; {@code createStream} returns
 *   a real {@code SseEmitter} so Spring can write the response headers correctly.
 *
 * Scenarios covered:
 * - GET  /api/events/stream              → 200 text/event-stream, createStream called
 * - GET  /api/events/stream?subscribe=…  → subscriptions forwarded to service
 * - GET  /api/events/stream (no subs)    → empty list forwarded to service
 * - POST   /api/events/stream/subscriptions → 200, verifyOwnership + addSubscription called
 * - POST   /api/events/stream/subscriptions → 403 when verifyOwnership rejects
 * - DELETE /api/events/stream/subscriptions → 200, verifyOwnership + removeSubscription called
 * - DELETE /api/events/stream/subscriptions → 403 when verifyOwnership rejects
 * - DELETE /api/events/stream/connection    → 200, verifyOwnership + closeConnection called
 * - DELETE /api/events/stream/connection    → 403 when verifyOwnership rejects
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "alice")
class StreamControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean StreamService streamService;

    // -------------------------------------------------------------------------
    // GET /api/events/stream — open SSE connection
    // -------------------------------------------------------------------------

    @Test
    void openStreamReturnsEventStreamContentType() throws Exception {
        SseEmitter emitter = new SseEmitter();
        when(streamService.createStream(isNull(), eq("alice"), anyLong(), anyList()))
                .thenReturn(emitter);

        // SseEmitter triggers async processing — complete the emitter so MockMvc
        // can dispatch the async result and inspect the response headers.
        var result = mockMvc.perform(get("/api/events/stream"))
                .andExpect(request().asyncStarted())
                .andReturn();
        emitter.complete();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    @Test
    void openStreamForwardsSubscriptionsToService() throws Exception {
        when(streamService.createStream(isNull(), eq("alice"), anyLong(), anyList()))
                .thenReturn(new SseEmitter());

        mockMvc.perform(get("/api/events/stream")
                        .param("subscribe", "Goal:1")
                        .param("subscribe", "Project:2"))
                .andExpect(request().asyncStarted());

        verify(streamService).createStream(isNull(), eq("alice"), anyLong(),
                eq(List.of("Goal:1", "Project:2")));
    }

    @Test
    void openStreamWithNoSubscriptionsPassesEmptyList() throws Exception {
        when(streamService.createStream(isNull(), eq("alice"), anyLong(), anyList()))
                .thenReturn(new SseEmitter());

        mockMvc.perform(get("/api/events/stream"))
                .andExpect(request().asyncStarted());

        verify(streamService).createStream(isNull(), eq("alice"), anyLong(), eq(List.of()));
    }

    // -------------------------------------------------------------------------
    // POST /api/events/stream/subscriptions — add subscription
    // -------------------------------------------------------------------------

    @Test
    void addSubscriptionReturnsOk() throws Exception {
        SubscriptionRequest body = new SubscriptionRequest("Goal", 7L);

        mockMvc.perform(post("/api/events/stream/subscriptions")
                        .header("X-Session-Id", "session-abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(streamService).verifyOwnership("session-abc", "alice");
        verify(streamService).addSubscription("session-abc", "Goal:7");
    }

    @Test
    void addSubscriptionReturnsForbiddenWhenOwnershipFails() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your session"))
                .when(streamService).verifyOwnership("session-xyz", "alice");

        SubscriptionRequest body = new SubscriptionRequest("Goal", 7L);

        mockMvc.perform(post("/api/events/stream/subscriptions")
                        .header("X-Session-Id", "session-xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());

        verify(streamService, never()).addSubscription(any(), any());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/events/stream/subscriptions — remove subscription
    // -------------------------------------------------------------------------

    @Test
    void removeSubscriptionReturnsOk() throws Exception {
        SubscriptionRequest body = new SubscriptionRequest("Project", 3L);

        mockMvc.perform(delete("/api/events/stream/subscriptions")
                        .header("X-Session-Id", "session-abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(streamService).verifyOwnership("session-abc", "alice");
        verify(streamService).removeSubscription("session-abc", "Project:3");
    }

    @Test
    void removeSubscriptionReturnsForbiddenWhenOwnershipFails() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your session"))
                .when(streamService).verifyOwnership("session-xyz", "alice");

        SubscriptionRequest body = new SubscriptionRequest("Project", 3L);

        mockMvc.perform(delete("/api/events/stream/subscriptions")
                        .header("X-Session-Id", "session-xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());

        verify(streamService, never()).removeSubscription(any(), any());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/events/stream/connection — graceful close
    // -------------------------------------------------------------------------

    @Test
    void closeConnectionReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/events/stream/connection")
                        .header("X-Session-Id", "session-abc"))
                .andExpect(status().isOk());

        verify(streamService).verifyOwnership("session-abc", "alice");
        verify(streamService).closeConnection("session-abc");
    }

    @Test
    void closeConnectionReturnsForbiddenWhenOwnershipFails() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your session"))
                .when(streamService).verifyOwnership("session-xyz", "alice");

        mockMvc.perform(delete("/api/events/stream/connection")
                        .header("X-Session-Id", "session-xyz"))
                .andExpect(status().isForbidden());

        verify(streamService, never()).closeConnection(any());
    }
}
