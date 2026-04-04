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

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory session and subscription store for SSE streaming.
 * Tracks which sessions are subscribed to which targets.
 * For Requel (single instance), in-memory is sufficient.
 */
@Service
public class StreamSessionStore {

    private final ConcurrentHashMap<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> targetToSessions = new ConcurrentHashMap<>();

    public void createSession(String sessionId) {
        sessionSubscriptions.putIfAbsent(sessionId, ConcurrentHashMap.newKeySet());
    }

    public void addSubscription(String sessionId, String targetKey) {
        sessionSubscriptions.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(targetKey);
        targetToSessions.computeIfAbsent(targetKey, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }

    public void removeSubscription(String sessionId, String targetKey) {
        Set<String> subs = sessionSubscriptions.get(sessionId);
        if (subs != null) {
            subs.remove(targetKey);
        }
        Set<String> sessions = targetToSessions.get(targetKey);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                targetToSessions.remove(targetKey);
            }
        }
    }

    public void removeSession(String sessionId) {
        Set<String> subs = sessionSubscriptions.remove(sessionId);
        if (subs != null) {
            for (String targetKey : subs) {
                Set<String> sessions = targetToSessions.get(targetKey);
                if (sessions != null) {
                    sessions.remove(sessionId);
                    if (sessions.isEmpty()) {
                        targetToSessions.remove(targetKey);
                    }
                }
            }
        }
    }

    /**
     * Find all session IDs subscribed to a target.
     */
    public Set<String> getSessionsForTarget(String targetKey) {
        Set<String> sessions = targetToSessions.get(targetKey);
        return sessions != null ? Set.copyOf(sessions) : Set.of();
    }

    public Set<String> getSubscriptions(String sessionId) {
        Set<String> subs = sessionSubscriptions.get(sessionId);
        return subs != null ? Set.copyOf(subs) : Set.of();
    }

    public int getActiveSessionCount() {
        return sessionSubscriptions.size();
    }
}
