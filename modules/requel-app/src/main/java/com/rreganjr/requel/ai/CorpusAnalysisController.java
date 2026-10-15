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
 * Issue #266: explicit corpus runs. {@code POST /api/ai/corpus?projectId=&set=&rootId=&mode=}
 * dispatches one and returns 202; {@code GET} with the same parameters reads the latest run (204
 * when there is none). {@code set} is PROJECT (default), GOAL or USE_CASE; {@code mode} is
 * CANDIDATES ("Find overlaps", no AI) or ANALYSIS (the AI corpus analysis).
 */
@RestController
@RequestMapping("/api/ai/corpus")
public class CorpusAnalysisController {

	private final CorpusAnalysisService service;

	@Autowired
	public CorpusAnalysisController(CorpusAnalysisService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<Void> request(@RequestParam Long projectId,
			@RequestParam(defaultValue = "PROJECT") String set,
			@RequestParam(required = false) Long rootId,
			@RequestParam(defaultValue = "CANDIDATES") String mode) {
		try {
			service.request(projectId, set, rootId, mode);
			return ResponseEntity.accepted().build();
		} catch (NoSuchEntityException e) {
			return ResponseEntity.notFound().build();
		}
	}

	@GetMapping
	public ResponseEntity<AiReviewDto> latest(@RequestParam Long projectId,
			@RequestParam(defaultValue = "PROJECT") String set,
			@RequestParam(required = false) Long rootId,
			@RequestParam(defaultValue = "CANDIDATES") String mode) {
		try {
			return service.latest(projectId, set, rootId, mode).map(AiReviewDto::of)
					.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
		} catch (NoSuchEntityException e) {
			return ResponseEntity.notFound().build();
		}
	}

	@ExceptionHandler(AuthorizationException.class)
	public ResponseEntity<ErrorResponse> forbidden(AuthorizationException e) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ErrorResponse.of("FORBIDDEN", e.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException e) {
		return ResponseEntity.badRequest().body(ErrorResponse.of("BAD_REQUEST", e.getMessage()));
	}
}
