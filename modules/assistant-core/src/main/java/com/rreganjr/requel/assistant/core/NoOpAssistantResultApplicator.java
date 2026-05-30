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
package com.rreganjr.requel.assistant.core;

import java.util.List;

import org.springframework.stereotype.Component;

import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;

/**
 * Fallback applicator that records nothing. The command-backed
 * {@link CommandBackedAssistantResultApplicator} is annotated {@code @Primary},
 * so it is preferred for injection wherever the annotation command stack is
 * available; this no-op remains as a safe default (and is what the worker uses
 * in unit tests that construct it directly).
 */
@Component
public class NoOpAssistantResultApplicator implements AssistantResultApplicator {

	@Override
	public AppliedAssistantResult apply(AssistantContext context, AssistantResult result) {
		return new AppliedAssistantResult(0, List.of());
	}
}
