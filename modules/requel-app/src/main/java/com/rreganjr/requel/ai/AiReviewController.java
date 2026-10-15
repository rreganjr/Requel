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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.requel.service.api.dto.ErrorResponse;

/**
 * Manual trigger endpoint for the AI requirements review (issue #43, Phase 5):
 * {@code POST /api/ai/reviews?entityType=&entityId=}, mounted under {@code /api/**} so the JWT
 * chain authenticates the caller. Dispatches a {@code REQUIREMENTS_REVIEW} run for the entity,
 * and a {@code POLICY_REVIEW} run when a policy applies (#265), and returns
 * {@code 202 Accepted}; the run executes asynchronously. Authorization (project access) and
 * bad-request / forbidden mapping are handled here: the global {@code ApiExceptionHandler} is
 * scoped to {@code com.rreganjr.requel.service}, so it never saw this controller's exceptions and
 * both turned into 500s (#355). A missing entity is mapped to 404 here.
 */
@RestController
@RequestMapping("/api/ai/reviews")
public class AiReviewController {

	private final AiReviewService aiReviewService;

	@Autowired
	public AiReviewController(AiReviewService aiReviewService) {
		this.aiReviewService = aiReviewService;
	}

	@PostMapping
	public ResponseEntity<Void> requestReview(@RequestParam String entityType,
			@RequestParam Long entityId) {
		try {
			aiReviewService.requestReview(entityType, entityId);
			return ResponseEntity.accepted().build();
		} catch (NoSuchEntityException e) {
			return ResponseEntity.notFound().build();
		}
	}

	/**
	 * Issue #355: {@code GET /api/ai/reviews?entityType=&entityId=} - the entity's latest review
	 * run: status, failure reason, the model's summary and the findings that run reported. 204
	 * when the entity has never been reviewed, 404 for an unknown entity, 400 for a type that
	 * is not reviewable, 403 without access to its project. {@code taskType=POLICY_REVIEW} reads
	 * the policy pass instead (#265).
	 */
	@GetMapping
	public ResponseEntity<AiReviewDto> latestReview(@RequestParam String entityType,
			@RequestParam Long entityId,
			@RequestParam(defaultValue = AiReviewService.TASK_TYPE) String taskType) {
		try {
			return aiReviewService.latestReview(entityType, entityId, taskType)
					.map(AiReviewDto::of)
					.map(ResponseEntity::ok)
					.orElseGet(() -> ResponseEntity.noContent().build());
		} catch (NoSuchEntityException e) {
			return ResponseEntity.notFound().build();
		}
	}

	/** #355: no access to the entity's project. */
	@ExceptionHandler(AuthorizationException.class)
	public ResponseEntity<ErrorResponse> forbidden(AuthorizationException e) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ErrorResponse.of("FORBIDDEN", e.getMessage()));
	}

	/** #355: an entity type that is not reviewable. */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException e) {
		return ResponseEntity.badRequest().body(ErrorResponse.of("BAD_REQUEST", e.getMessage()));
	}
}
