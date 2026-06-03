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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rreganjr.platform.exception.NoSuchEntityException;

/**
 * Manual trigger endpoint for the AI requirements review (issue #43, Phase 5):
 * {@code POST /api/ai/reviews?entityType=&entityId=}, mounted under {@code /api/**} so the JWT
 * chain authenticates the caller. Dispatches a {@code REQUIREMENTS_REVIEW} run for the entity
 * and returns
 * {@code 202 Accepted}; the run executes asynchronously. Authorization (project access) and
 * bad-request / forbidden mapping are handled by {@link AiReviewService} + the global
 * {@code ApiExceptionHandler}; a missing entity is mapped to 404 here.
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
}
