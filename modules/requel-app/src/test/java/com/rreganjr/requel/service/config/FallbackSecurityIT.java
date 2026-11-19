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
package com.rreganjr.requel.service.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.rreganjr.requel.Application;

/**
 * Issue #449: the fallback chain, over real HTTP so the SPA's forward and the error dispatch
 * actually happen (MockMvc does neither). GET and HEAD reach the SPA and static files, every
 * other method on a path no chain claims is refused, and a request that fails on the
 * {@code /api/**} chain still gets its own error, not a 403 from the fallback's error dispatch.
 */
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:db.properties", properties = {
		"db.name=requel_fallback_security_test",
		"db.driverUrl=jdbc:h2:mem:requel_fallback_security_test;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false"
})
@ActiveProfiles("test")
class FallbackSecurityIT {

	@Autowired
	private TestRestTemplate http;

	@Test
	void spaRoutesAreServed() {
		// index.html is on the classpath only when the Angular build ran; without it the forward
		// finds nothing (404). Either way the security chain must not refuse it.
		int expected = new ClassPathResource("static/index.html").exists() ? 200 : 404;
		assertThat(http.getForEntity("/", String.class).getStatusCode().value()).isEqualTo(expected);
		assertThat(http.getForEntity("/projects/some-project", String.class).getStatusCode().value())
				.isEqualTo(expected);
	}

	@Test
	void staticFilesAreServed() {
		ResponseEntity<String> response = http.getForEntity("/test396/plain.txt", String.class);
		assertThat(response.getStatusCode().value()).isEqualTo(200);
	}

	@Test
	void headIsAllowed() {
		ResponseEntity<Void> response = http.exchange("/test396/plain.txt", HttpMethod.HEAD, null,
				Void.class);
		assertThat(response.getStatusCode().value()).isEqualTo(200);
	}

	@Test
	void otherMethodsOnUnclaimedPathsAreRefused() {
		for (HttpMethod method : new HttpMethod[] { HttpMethod.POST, HttpMethod.PUT,
				HttpMethod.DELETE }) {
			ResponseEntity<String> response = http.exchange("/not-an-api-route", method,
					new HttpEntity<>("{}", jsonHeaders()), String.class);
			assertThat(response.getStatusCode().value()).as(method.name()).isEqualTo(403);
		}
	}

	@Test
	void aFailedApiRequestKeepsItsOwnErrorThroughTheErrorDispatch() {
		// Malformed JSON: Spring MVC answers with sendError(400), which dispatches to /error. No
		// chain but the fallback matches /error, so without its ERROR permit this would be a 403.
		ResponseEntity<String> response = http.exchange("/api/auth/login", HttpMethod.POST,
				new HttpEntity<>("{not json", jsonHeaders()), String.class);
		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody()).contains("\"status\":400");
	}

	private static HttpHeaders jsonHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}
}
