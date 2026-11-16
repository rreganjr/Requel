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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * The sign-in limits on every path a password is checked (issue #448): the API login, the OAuth
 * sign-in form and the current-password check on a password change. Three failures for one
 * username from one address start the wait here (the test raises the other two limits out of the
 * way; {@link LoginThrottleTest} covers them). The clock is a bean the test moves forward.
 *
 * <p>Each test uses its own user and client address, because the counts outlive a test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "requel.login-throttle.per-pair=3",
        "requel.login-throttle.per-source=100000",
        "requel.login-throttle.per-username=100000" })
@Import(LoginThrottleIT.TestClock.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LoginThrottleIT extends AbstractIntegrationTestCase {

    @TestConfiguration
    static class TestClock {
        @Bean
        LoginThrottleTest.MutableClock loginThrottleClock() {
            return new LoginThrottleTest.MutableClock(Instant.parse("2026-11-01T12:00:00Z"));
        }
    }

    private static final AtomicInteger ADDRESSES = new AtomicInteger(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LoginThrottleTest.MutableClock clock;

    @Autowired
    private Clock injectedClock;

    @Autowired
    private MeterRegistry meterRegistry;

    private String prefix;

    @BeforeAll
    void setUpFixture() throws Exception {
        initializeBaselineData();
        prefix = "throttle-" + System.currentTimeMillis() + "-";
    }

    @Test
    void theTestClockIsTheOneTheThrottleUses() {
        assertThat(injectedClock).isSameAs(clock);
    }

    @Test
    void apiLoginIsRefusedAfterThreeFailuresEvenWithTheRightPasswordThenRecovers() throws Exception {
        String user = newUser("api");
        String address = nextAddress();
        for (int i = 0; i < 3; i++) {
            login(user, "wrong-" + i, address).andExpect(status().isUnauthorized());
        }
        login(user, user, address)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "1"))
                .andExpect(jsonPath("$.error").value("TOO_MANY_ATTEMPTS"))
                .andExpect(jsonPath("$.message")
                        .value("Too many sign-in attempts. Try again in 1 second."));

        clock.advance(Duration.ofSeconds(1));
        login(user, user, address).andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void anotherAddressIsNotSlowedByThisOnesFailures() throws Exception {
        String user = newUser("other-address");
        String address = nextAddress();
        for (int i = 0; i < 3; i++) {
            login(user, "wrong-" + i, address).andExpect(status().isUnauthorized());
        }
        login(user, user, address).andExpect(status().isTooManyRequests());
        login(user, user, nextAddress()).andExpect(status().isOk());
    }

    @Test
    void anUnknownUsernameIsRefusedTheSameWay() throws Exception {
        String ghost = prefix + "no-such-user";
        String address = nextAddress();
        for (int i = 0; i < 3; i++) {
            login(ghost, "guess-" + i, address).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }
        login(ghost, "guess", address).andExpect(status().isTooManyRequests());
    }

    @Test
    void theOAuthSignInFormShowsTheLimitAndCountsIt() throws Exception {
        String user = newUser("form");
        String address = nextAddress();
        double before = throttledCount("form");
        for (int i = 0; i < 3; i++) {
            formLogin(user, "wrong-" + i, address).andExpect(redirectedUrl("/oauth2/login?error"));
        }
        formLogin(user, user, address).andExpect(redirectedUrl("/oauth2/login?error=throttled"));
        assertThat(throttledCount("form")).isEqualTo(before + 1);

        clock.advance(Duration.ofSeconds(1));
        MvcResult ok = formLogin(user, user, address).andReturn();
        assertThat(ok.getResponse().getRedirectedUrl()).doesNotContain("error");
    }

    @Test
    void wrongCurrentPasswordsOnAPasswordChangeAreLimitedToo() throws Exception {
        String user = newUser("password-change");
        String address = nextAddress();
        String token = token(user, address);
        Map<String, String> change = Map.of("currentPassword", "not-it", "newPassword",
                "Another-pass-1", "confirmPassword", "Another-pass-1");
        for (int i = 0; i < 3; i++) {
            changePassword(token, change, address).andExpect(status().isUnprocessableEntity());
        }
        changePassword(token, Map.of("currentPassword", user, "newPassword", "Another-pass-1",
                "confirmPassword", "Another-pass-1"), address)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void theLoginCommandIsNoLongerOnTheCommandEndpoint() throws Exception {
        String user = newUser("command");
        String token = token(user, nextAddress());
        mockMvc.perform(post("/api/commands/Login")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("username", "admin",
                        "password", "admin"))))
                .andExpect(status().isBadRequest());
    }

    // --- helpers ---

    private double throttledCount(String path) {
        var counter = meterRegistry.find(LoginThrottle.THROTTLED_METRIC).tag("path", path).counter();
        return counter == null ? 0 : counter.count();
    }

    private static String nextAddress() {
        int n = ADDRESSES.getAndIncrement();
        return "10.44." + (n / 250) + "." + (n % 250 + 1);
    }

    private static MockHttpServletRequestBuilder from(MockHttpServletRequestBuilder request,
            String address) {
        return request.with(r -> {
            r.setRemoteAddr(address);
            return r;
        });
    }

    private ResultActions login(String username, String password, String address) throws Exception {
        return mockMvc.perform(from(post("/api/auth/login"), address)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("username", username,
                        "password", password))));
    }

    private String token(String username, String address) throws Exception {
        MvcResult result = login(username, username, address).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private ResultActions formLogin(String username, String password, String address)
            throws Exception {
        return mockMvc.perform(from(post("/oauth2/login"), address).with(csrf())
                .param("username", username).param("password", password));
    }

    private ResultActions changePassword(String token, Map<String, String> body, String address)
            throws Exception {
        return mockMvc.perform(from(post("/api/auth/password"), address)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    /** A project user whose password is its username. */
    private String newUser(String name) throws Exception {
        String username = prefix + name;
        User admin = getUserRepository().findUserByUsername("admin");
        EditUserCommand command = getUserCommandFactory().newEditUserCommand();
        command.setEditedBy(admin);
        command.setUsername(username);
        command.setPassword(username);
        command.setRepassword(username);
        command.setName(username);
        command.setEmailAddress(username + "@example.com");
        command.setPhoneNumber("");
        command.setOrganizationName("ThrottleOrg");
        command.addUserRoleName("ProjectUserRole");
        getCommandHandler().execute(command);
        return username;
    }
}
