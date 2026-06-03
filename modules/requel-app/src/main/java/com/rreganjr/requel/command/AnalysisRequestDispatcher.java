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
package com.rreganjr.requel.command;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.assistant.api.AnalysisRequest;
import com.rreganjr.requel.assistant.api.AssistantDispatcher;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.user.UserRepository;

/**
 * Application-layer bridge that turns a freshly-edited domain entity into an
 * {@link AnalysisRequest} and hands it to the {@link AssistantDispatcher}. This
 * lives in {@code requel-app} (not in the project/command modules) so the
 * assistant SPI dependency stays in the application layer; the command modules
 * only expose domain objects via
 * {@link com.rreganjr.requel.project.command.AnalysisRequestSource}.
 */
@Component("analysisRequestDispatcher")
public class AnalysisRequestDispatcher {

	private final AssistantDispatcher assistantDispatcher;
	private final UserRepository userRepository;

	@Autowired
	public AnalysisRequestDispatcher(AssistantDispatcher assistantDispatcher,
			UserRepository userRepository) {
		this.assistantDispatcher = Objects.requireNonNull(assistantDispatcher,
				"assistantDispatcher");
		this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
	}

	/**
	 * Dispatch analysis of {@code target} on behalf of {@code triggeringUser}.
	 * No-op when either is {@code null}.
	 */
	public void dispatch(ProjectOrDomainEntity target, User triggeringUser) {
		dispatch(target, triggeringUser, null);
	}

	/**
	 * Dispatch analysis of {@code target} for a specific {@code taskType} (e.g.
	 * {@code "REQUIREMENTS_REVIEW"} for a manual AI review). A {@code null} task type is the
	 * ordinary post-edit analysis. No-op when target or user is {@code null}.
	 */
	public void dispatch(ProjectOrDomainEntity target, User triggeringUser, String taskType) {
		if (target == null || triggeringUser == null) {
			return;
		}
		EntityRef targetRef = EntityRef.of(target.getProjectOrDomainEntityInterface().getSimpleName(),
				target.getId());
		ProjectOrDomain projectOrDomain = target.getProjectOrDomain();
		EntityRef projectRef = projectOrDomain != null
				? EntityRef.of("Project", projectOrDomain.getId())
				: null;
		UserRef triggeringUserRef = new UserRef(triggeringUser.getId(), triggeringUser.getUsername());
		AnalysisRequest request = new AnalysisRequest(targetRef, projectRef, triggeringUserRef,
				assistantUserRef(), taskType, Locale.getDefault(), Map.of());
		assistantDispatcher.dispatch(request);
	}

	private UserRef assistantUserRef() {
		User assistant = userRepository.findUserByUsername("assistant");
		return new UserRef(assistant.getId(), assistant.getUsername());
	}
}
