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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rreganjr.requel.gateway.AiReviewGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.service.gateway.GatewayCommandController.GatewayErrorBody;

/**
 * Issue #387: the gateway's AI review over REST, for {@code requel review}. Under
 * {@code /api/gateway/**}, so the MCP / gateway security chain authenticates the caller (a PAT or
 * an OAuth token). Errors carry the gateway {@code kind} as the command endpoint's do.
 * <ul>
 * <li>{@code POST /api/gateway/ai/reviews/{projectName}?entityType=&entityId=} - request a review;
 * 202 with the runs to follow. Its findings are written to the project, so it is refused like a
 * write command when {@code requel.gateway.write.enabled} is false.</li>
 * <li>{@code GET /api/gateway/ai/reviews/{projectName}?entityType=&entityId=&taskType=} - the
 * latest run, 204 when the entity has never been reviewed.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/gateway/ai/reviews")
public class GatewayAiReviewController {

	static final String WRITES_DISABLED_MESSAGE = "Requesting an AI review writes its findings to"
			+ " the project, and write commands are disabled; set"
			+ " requel.gateway.write.enabled=true to enable them";

	private final AiReviewGateway gateway;
	private final boolean writeEnabled;

	@Autowired
	public GatewayAiReviewController(AiReviewGateway gateway,
			@Value("${requel.gateway.write.enabled:false}") boolean writeEnabled) {
		this.gateway = gateway;
		this.writeEnabled = writeEnabled;
	}

	@PostMapping("/{projectName}")
	public ResponseEntity<?> requestReview(@PathVariable String projectName,
			@RequestParam String entityType, @RequestParam long entityId) {
		if (!writeEnabled) {
			return error(new GatewayException(GatewayException.Kind.NOT_ALLOWED,
					WRITES_DISABLED_MESSAGE));
		}
		try {
			return ResponseEntity.accepted()
					.body(gateway.requestReview(projectName, entityType, entityId));
		} catch (GatewayException e) {
			return error(e);
		}
	}

	@GetMapping("/{projectName}")
	public ResponseEntity<?> latestReview(@PathVariable String projectName,
			@RequestParam String entityType, @RequestParam long entityId,
			@RequestParam(required = false) String taskType) {
		try {
			return gateway.latestReview(projectName, entityType, entityId, taskType)
					.<ResponseEntity<?>>map(ResponseEntity::ok)
					.orElseGet(() -> ResponseEntity.noContent().build());
		} catch (GatewayException e) {
			return error(e);
		}
	}

	private static ResponseEntity<?> error(GatewayException e) {
		HttpStatus status = switch (e.getKind()) {
			case NOT_ALLOWED, UNAUTHORIZED -> HttpStatus.FORBIDDEN;
			case NOT_FOUND -> HttpStatus.NOT_FOUND;
			case CONFLICT -> HttpStatus.CONFLICT;
			case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
			case EXECUTION_ERROR -> HttpStatus.UNPROCESSABLE_ENTITY;
		};
		return ResponseEntity.status(status).body(new GatewayErrorBody(e.getKind().name(),
				e.getMessage()));
	}
}
