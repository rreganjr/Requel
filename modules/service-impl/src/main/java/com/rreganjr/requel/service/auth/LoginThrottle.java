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
package com.rreganjr.requel.service.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Limits failed sign-in attempts (issue #448). Shared by every place a password is checked: the
 * API login ({@code POST /api/auth/login}), the OAuth sign-in form and the current-password check
 * on {@code POST /api/auth/password}.
 *
 * <p>
 * Failures are counted under three keys, each over a sliding window (15 minutes by default):
 * <ul>
 * <li><b>username + source address</b>: after {@code per-pair} failures (5) each further failure
 * doubles the wait (1 s, 2 s, 4 s ...), up to {@code max-wait} (15 minutes);</li>
 * <li><b>source address</b>, any username: after {@code per-source} failures (50) the source waits
 * {@code max-wait};</li>
 * <li><b>username</b>, any address: after {@code per-username} failures (50) the wait doubles, up to
 * {@code max-wait}.</li>
 * </ul>
 * A count clears once a window passes with no failures, and a successful sign-in clears its
 * username + source count, so nothing locks an account for good. The username is the one supplied,
 * whether or not the account exists, so a refusal says nothing about which usernames exist.
 *
 * <p>
 * Counts are kept in memory: Requel runs as one server, and a restart clears them. A refused
 * attempt is logged (never the password) and counted in {@code requel.login.throttled}.
 */
@Component
public class LoginThrottle {

    private static final Logger log = LoggerFactory.getLogger(LoginThrottle.class);

    /** The counter of refused attempts, tagged {@code path} and {@code limit}. */
    public static final String THROTTLED_METRIC = "requel.login.throttled";

    /** Sweep expired entries at most this often, so the maps stay bounded. */
    private static final Duration SWEEP_INTERVAL = Duration.ofMinutes(1);

    /** Which limit refused an attempt. */
    public enum Limit {
        PAIR("username-and-source"), SOURCE("source"), USERNAME("username");

        private final String tag;

        Limit(String tag) {
            this.tag = tag;
        }

        public String tag() {
            return tag;
        }
    }

    /** A refused attempt: how long to wait, and which limit refused it. */
    public record Refusal(long retryAfterSeconds, Limit limit) {
        /** "Too many sign-in attempts. Try again in 3 minutes." */
        public String message() {
            return "Too many sign-in attempts. Try again in " + waitText(retryAfterSeconds) + ".";
        }

        private static String waitText(long seconds) {
            if (seconds < 60) {
                return seconds <= 1 ? "1 second" : seconds + " seconds";
            }
            long minutes = (seconds + 59) / 60;
            return minutes == 1 ? "1 minute" : minutes + " minutes";
        }
    }

    private final boolean enabled;
    private final Duration window;
    private final Duration maxWait;
    private final int perPair;
    private final int perSource;
    private final int perUsername;
    private final ObjectProvider<Clock> clockProvider;
    private final ObjectProvider<MeterRegistry> meterRegistry;

    private final Map<String, Window> pairs = new ConcurrentHashMap<>();
    private final Map<String, Window> sources = new ConcurrentHashMap<>();
    private final Map<String, Window> usernames = new ConcurrentHashMap<>();
    private volatile Instant lastSweep = Instant.EPOCH;

    public LoginThrottle(
            @Value("${requel.login-throttle.enabled:true}") boolean enabled,
            @Value("${requel.login-throttle.window:PT15M}") Duration window,
            @Value("${requel.login-throttle.max-wait:PT15M}") Duration maxWait,
            @Value("${requel.login-throttle.per-pair:5}") int perPair,
            @Value("${requel.login-throttle.per-source:50}") int perSource,
            @Value("${requel.login-throttle.per-username:50}") int perUsername,
            ObjectProvider<Clock> clockProvider,
            ObjectProvider<MeterRegistry> meterRegistry) {
        this.enabled = enabled;
        this.window = window;
        this.maxWait = maxWait;
        this.perPair = perPair;
        this.perSource = perSource;
        this.perUsername = perUsername;
        this.clockProvider = clockProvider;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Whether an attempt may go ahead. A refused attempt is logged and counted here; the caller
     * answers it without checking the password.
     *
     * @param path
     *            where the attempt came in, for the log and the metric tag: {@code api},
     *            {@code form} or {@code password-change}
     * @return the refusal, or null to go ahead
     */
    public Refusal check(String path, String username, String source) {
        if (!enabled) {
            return null;
        }
        Instant now = now();
        Refusal refusal = longest(
                refusal(pairs.get(pairKey(username, source)), now, Limit.PAIR),
                refusal(sources.get(sourceKey(source)), now, Limit.SOURCE),
                refusal(usernames.get(usernameKey(username)), now, Limit.USERNAME));
        if (refusal != null) {
            log.warn("Sign-in attempt refused ({} limit, {}): username '{}', source {}, retry in {}s",
                    refusal.limit().tag(), path, username, source, refusal.retryAfterSeconds());
            MeterRegistry registry = meterRegistry.getIfAvailable();
            if (registry != null) {
                registry.counter(THROTTLED_METRIC, "path", path, "limit", refusal.limit().tag())
                        .increment();
            }
        }
        return refusal;
    }

    /** A wrong password (or an unknown username) for an attempt {@link #check} let through. */
    public void recordFailure(String username, String source) {
        if (!enabled) {
            return;
        }
        Instant now = now();
        sweepIfDue(now);
        fail(pairs, pairKey(username, source), now, perPair, true);
        fail(sources, sourceKey(source), now, perSource, false);
        fail(usernames, usernameKey(username), now, perUsername, true);
    }

    /** A successful sign-in clears its username + source count. */
    public void recordSuccess(String username, String source) {
        if (enabled) {
            pairs.remove(pairKey(username, source));
        }
    }

    private void fail(Map<String, Window> counts, String key, Instant now, int limit,
            boolean doubling) {
        counts.compute(key, (k, w) -> {
            if (w == null || w.expired(now, window)) {
                w = new Window();
            }
            w.failures++;
            w.lastFailure = now;
            if (w.failures >= limit) {
                w.blockedUntil = now.plus(doubling ? doubledWait(w.failures - limit) : maxWait);
            }
            return w;
        });
    }

    /** 1 s for the failure that reaches the limit, doubling for each one after, capped. */
    private Duration doubledWait(int failuresPastLimit) {
        if (failuresPastLimit >= 30) {
            return maxWait;
        }
        Duration wait = Duration.ofSeconds(1L << failuresPastLimit);
        return wait.compareTo(maxWait) > 0 ? maxWait : wait;
    }

    private static Refusal refusal(Window w, Instant now, Limit limit) {
        if (w == null || w.blockedUntil == null || !w.blockedUntil.isAfter(now)) {
            return null;
        }
        long millis = Duration.between(now, w.blockedUntil).toMillis();
        return new Refusal(Math.max(1, (millis + 999) / 1000), limit);
    }

    private static Refusal longest(Refusal... refusals) {
        Refusal longest = null;
        for (Refusal r : refusals) {
            if (r != null && (longest == null || r.retryAfterSeconds() > longest.retryAfterSeconds())) {
                longest = r;
            }
        }
        return longest;
    }

    private void sweepIfDue(Instant now) {
        if (Duration.between(lastSweep, now).compareTo(SWEEP_INTERVAL) < 0) {
            return;
        }
        lastSweep = now;
        for (Map<String, Window> counts : java.util.List.of(pairs, sources, usernames)) {
            counts.entrySet().removeIf(e -> e.getValue().expired(now, window));
        }
    }

    /** How many keys are being counted, across the three limits (for tests). */
    int trackedKeys() {
        return pairs.size() + sources.size() + usernames.size();
    }

    private Instant now() {
        return clockProvider.getIfAvailable(Clock::systemUTC).instant();
    }

    private static String usernameKey(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static String sourceKey(String source) {
        return source == null ? "" : source;
    }

    private static String pairKey(String username, String source) {
        return usernameKey(username) + "\u0000" + sourceKey(source);
    }

    /** Failures under one key since the window last cleared. */
    private static final class Window {
        int failures;
        Instant lastFailure;
        Instant blockedUntil;

        /** A window passed with no failures, and any wait is over. */
        boolean expired(Instant now, Duration window) {
            boolean quiet = lastFailure == null || !lastFailure.plus(window).isAfter(now);
            boolean waited = blockedUntil == null || !blockedUntil.isAfter(now);
            return quiet && waited;
        }
    }
}
