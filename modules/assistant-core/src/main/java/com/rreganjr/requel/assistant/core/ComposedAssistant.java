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

import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantException;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.RequelAssistant;

/**
 * Issue #265: one assistant that does the work of several in a single pass (policies composed
 * into one provider call) and reports one result per member. Each result's
 * {@link AssistantResult#assistantId()} names the member it belongs to, so the applicator
 * attributes, deduplicates and cleans up each member's findings as if it had run alone.
 */
public interface ComposedAssistant {

	/** The assistants this one stands for, one per composed unit of work. */
	List<RequelAssistant<?>> members();

	/** One result per member that ran; a member left out of the pass has no result. */
	List<AssistantResult> analyzeAll(AssistantContext context, Object target)
			throws AssistantException;

	/** The member a result belongs to, or null when none has that id. */
	default RequelAssistant<?> memberFor(String assistantId) {
		for (RequelAssistant<?> member : members()) {
			if (member.assistantId().equals(assistantId)) {
				return member;
			}
		}
		return null;
	}
}
