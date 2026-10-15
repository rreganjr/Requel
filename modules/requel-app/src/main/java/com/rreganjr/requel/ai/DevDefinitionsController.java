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

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rreganjr.requel.assistant.core.definition.DevDefinitionOverrides;
import com.rreganjr.requel.assistant.core.definition.InvalidAssistantDefinitionException;

/**
 * Issue #263, dev only: {@code POST /api/dev/ai/definitions/reload} re-reads
 * {@code requel.ai.definitions.dir} so a tuning round needs no restart. It exists only when that
 * property is set (as the other {@code /api/dev} endpoints exist only when enabled).
 */
@ConditionalOnProperty(name = "requel.ai.definitions.dir")
@RestController
@RequestMapping("/api/dev/ai")
public class DevDefinitionsController {

	private final DevDefinitionOverrides overrides;

	@Autowired
	public DevDefinitionsController(DevDefinitionOverrides overrides) {
		this.overrides = overrides;
	}

	@PostMapping("/definitions/reload")
	public ResponseEntity<Map<String, Object>> reload() {
		try {
			List<String> keys = overrides.reload();
			return ResponseEntity.ok(Map.of("reloaded", keys));
		} catch (InvalidAssistantDefinitionException e) {
			return ResponseEntity.badRequest().body(Map.of("error", String.join("; ", e.problems())));
		} catch (IllegalStateException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}
}
