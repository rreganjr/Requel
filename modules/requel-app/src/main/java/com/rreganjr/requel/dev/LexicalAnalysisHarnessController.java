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
package com.rreganjr.requel.dev;

import java.net.InetAddress;
import java.net.UnknownHostException;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rreganjr.requel.project.exception.NoSuchProjectException;

/**
 * Issue #268, step 1: serves {@link LexicalAnalysisHarness} at
 * {@code GET /api/dev/lexical-analysis?project=<name>} as TSV.
 *
 * <p>
 * Dev only: registered when {@code requel.dev.lexical-harness.enabled=true} (the dev profile).
 * {@code /api/dev/**} is unauthenticated (ApiSecurityConfig), and this endpoint returns project
 * text, so it also refuses any request that does not come from the loopback interface.
 */
@ConditionalOnProperty(name = "requel.dev.lexical-harness.enabled", havingValue = "true")
@RestController
@RequestMapping("/api/dev")
public class LexicalAnalysisHarnessController {

	private static final MediaType TSV = new MediaType("text", "tab-separated-values");

	private final LexicalAnalysisHarness harness;

	@Autowired
	public LexicalAnalysisHarnessController(LexicalAnalysisHarness harness) {
		this.harness = harness;
	}

	@GetMapping("/lexical-analysis")
	public ResponseEntity<String> analyze(@RequestParam String project,
			HttpServletRequest request) {
		if (!isLoopback(request.getRemoteAddr())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		try {
			return ResponseEntity.ok().contentType(TSV).body(harness.run(project));
		} catch (NoSuchProjectException e) {
			return ResponseEntity.notFound().build();
		}
	}

	static boolean isLoopback(String remoteAddr) {
		if (remoteAddr == null || remoteAddr.isBlank()) {
			return false;
		}
		try {
			return InetAddress.getByName(remoteAddr).isLoopbackAddress();
		} catch (UnknownHostException e) {
			return false;
		}
	}
}
