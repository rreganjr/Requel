/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.rreganjr.requel.gateway.AiReviewGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.service.api.dto.AiReviewDto;
import com.rreganjr.requel.service.api.dto.AiReviewRequestDto;

/**
 * Issue #387: the gateway's AI review endpoints map the gateway's answers and kinds to HTTP, and
 * refuse a request when gateway writes are off.
 */
class GatewayAiReviewControllerTest {

	private String lastCall;

	private final AiReviewGateway gateway = new AiReviewGateway() {
		@Override
		public AiReviewRequestDto requestReview(String projectName, String entityType,
				long entityId) throws GatewayException {
			lastCall = "request " + projectName + " " + entityType + " " + entityId;
			return switch ((int) entityId) {
				case 409 -> throw new GatewayException(GatewayException.Kind.CONFLICT,
						"AI review is off on this server.");
				case 404 -> throw new GatewayException(GatewayException.Kind.NOT_FOUND, "no goal");
				case 400 -> throw new GatewayException(GatewayException.Kind.INVALID_INPUT,
						"Goal 400 belongs to another project.");
				default -> new AiReviewRequestDto("r1", null);
			};
		}

		@Override
		public Optional<AiReviewDto> latestReview(String projectName, String entityType,
				long entityId, String taskType) {
			lastCall = "latest " + projectName + " " + entityType + " " + entityId + " " + taskType;
			return entityId == 0 ? Optional.empty()
					: Optional.of(new AiReviewDto("r1", "RUNNING", null, null, null, null, null, null,
							0, List.of(), 0, null, null, 0, null));
		}
	};

	private MockMvc mvc(boolean writes) {
		return MockMvcBuilders.standaloneSetup(new GatewayAiReviewController(gateway, writes))
				.build();
	}

	@Test
	void aRequestIsAcceptedWithTheRuns() throws Exception {
		mvc(true).perform(post("/api/gateway/ai/reviews/Demo").param("entityType", "Goal")
				.param("entityId", "7"))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.runId").value("r1"));
		assertThat(lastCall).isEqualTo("request Demo Goal 7");
	}

	@Test
	void aRequestIsRefusedWhenWritesAreOff() throws Exception {
		mvc(false).perform(post("/api/gateway/ai/reviews/Demo").param("entityType", "Goal")
				.param("entityId", "7"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.kind").value("NOT_ALLOWED"))
				.andExpect(jsonPath("$.message").value(
						GatewayAiReviewController.WRITES_DISABLED_MESSAGE));
		assertThat(lastCall).isNull();
	}

	@Test
	void failuresCarryTheirKind() throws Exception {
		MockMvc mvc = mvc(true);
		mvc.perform(post("/api/gateway/ai/reviews/Demo").param("entityType", "Goal")
				.param("entityId", "409"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.kind").value("CONFLICT"))
				.andExpect(jsonPath("$.message").value("AI review is off on this server."));
		mvc.perform(post("/api/gateway/ai/reviews/Demo").param("entityType", "Goal")
				.param("entityId", "404"))
				.andExpect(status().isNotFound());
		mvc.perform(post("/api/gateway/ai/reviews/Demo").param("entityType", "Goal")
				.param("entityId", "400"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.kind").value("INVALID_INPUT"));
	}

	@Test
	void theLatestRunIsReadEvenWithWritesOffAndNeverReviewedIs204() throws Exception {
		MockMvc mvc = mvc(false);
		mvc.perform(get("/api/gateway/ai/reviews/Demo").param("entityType", "Goal")
				.param("entityId", "7").param("taskType", "POLICY_REVIEW"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("RUNNING"));
		assertThat(lastCall).isEqualTo("latest Demo Goal 7 POLICY_REVIEW");
		mvc.perform(get("/api/gateway/ai/reviews/Demo").param("entityType", "Goal")
				.param("entityId", "0"))
				.andExpect(status().isNoContent());
	}
}
