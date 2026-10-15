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
package com.rreganjr.requel.project.command;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.project.ProjectOrDomainEntity;

/**
 * Implemented by edit commands that opt into assistant analysis via the
 * assistant SPI (issue #43). Rather than calling the assistant layer themselves,
 * such commands simply expose the edited entity and the triggering user; the
 * command-handler layer ({@code AnalysisInvokingCommandHandler}) reads these
 * after a successful commit and dispatches an analysis request through the SPI.
 *
 * <p>
 * This keeps the project/command modules free of any dependency on the assistant
 * SPI — the translation to an {@code AnalysisRequest} (and the choice of
 * dispatcher) lives entirely in the application layer. Commands that have not
 * been migrated continue to use the legacy
 * {@code AnalyzableEditCommand.invokeAnalysis()} path instead.
 */
public interface AnalysisRequestSource {

	/**
	 * @return the edited entity to analyze, or {@code null} to skip analysis for
	 *         this execution.
	 */
	ProjectOrDomainEntity getAnalysisTarget();

	/**
	 * @return the user whose edit triggered analysis; authorization for any
	 *         resulting annotation writes mirrors this user.
	 */
	User getAnalysisTriggeredBy();
}
