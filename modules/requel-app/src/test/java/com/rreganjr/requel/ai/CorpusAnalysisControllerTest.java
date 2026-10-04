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
package com.rreganjr.requel.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunReadService;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.service.api.dto.ErrorResponse;

/** Issue #266: the corpus endpoints' responses. */
class CorpusAnalysisControllerTest {

	private final CorpusAnalysisService service = mock(CorpusAnalysisService.class);
	private final CorpusAnalysisController controller = new CorpusAnalysisController(service);

	@Test
	void aRequestIsAcceptedOrNotFound() {
		assertThat(controller.request(7L, "PROJECT", null, "CANDIDATES").getStatusCode())
				.isEqualTo(HttpStatus.ACCEPTED);
		verify(service).request(7L, "PROJECT", null, "CANDIDATES");

		doThrow(NoSuchEntityException.byQuery(Project.class, "id", 8L)).when(service)
				.request(8L, "PROJECT", null, "CANDIDATES");
		assertThat(controller.request(8L, "PROJECT", null, "CANDIDATES").getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void theLatestRunIsReturnedOrNoContentOrNotFound() {
		AssistantRunReadService.RunView run = new AssistantRunReadService.RunView("run-1",
				"corpus-finder", "COMPLETED", null, null, null, null, null, "2 candidates", 0,
				List.of(), 0, null, null, 0);
		when(service.latest(7L, "PROJECT", null, "CANDIDATES")).thenReturn(Optional.of(run));
		when(service.latest(7L, "GOAL", 3L, "CANDIDATES")).thenReturn(Optional.empty());
		when(service.latest(8L, "PROJECT", null, "CANDIDATES"))
				.thenThrow(NoSuchEntityException.byQuery(Project.class, "id", 8L));

		ResponseEntity<AiReviewDto> found = controller.latest(7L, "PROJECT", null, "CANDIDATES");
		assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(found.getBody().runId()).isEqualTo("run-1");
		assertThat(found.getBody().summary()).isEqualTo("2 candidates");
		assertThat(controller.latest(7L, "GOAL", 3L, "CANDIDATES").getStatusCode())
				.isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(controller.latest(8L, "PROJECT", null, "CANDIDATES").getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void refusalsMapToForbiddenAndBadRequest() {
		ResponseEntity<ErrorResponse> forbidden = controller
				.forbidden(new AuthorizationException("no access"));
		assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(forbidden.getBody().error()).isEqualTo("FORBIDDEN");
		assertThat(forbidden.getBody().message()).isEqualTo("no access");

		ResponseEntity<ErrorResponse> bad = controller
				.badRequest(new IllegalArgumentException("Unknown corpus set: X"));
		assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(bad.getBody().error()).isEqualTo("BAD_REQUEST");
	}
}
