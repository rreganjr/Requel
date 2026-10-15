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
package com.rreganjr.requel.project;

import com.rreganjr.requel.user.User;

/**
 * Issue #260: each assistant writes its annotations as its own user, so lexical checks and each
 * AI definition are told apart in authorship and audit. The identity is the user
 * {@code assistant-<assistantId>}, holding {@code AssistantUserRole}; it is created the first time
 * it is needed and made a stakeholder of the project with the assistant permission set
 * ({@code ProjectRepository.findAssistantStakeholderPermissions()}), so import, repair and project
 * creation need no list of assistants.
 */
public interface AssistantIdentities {

	/**
	 * The identity {@code assistantId} writes as, ready to edit annotations in the project.
	 *
	 * @param assistantId the assistant (or definition key) writing
	 * @param projectId the project being written to, or null for none (no stakeholder is added)
	 * @return the identity, never null
	 */
	User identityFor(String assistantId, Long projectId);
}
