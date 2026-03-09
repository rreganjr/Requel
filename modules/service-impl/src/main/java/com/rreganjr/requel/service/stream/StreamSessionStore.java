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
