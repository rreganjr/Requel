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
import com.rreganjr.requel.project.Project;

/**
 * Implemented by commands whose result should be analyzed as a whole project (#268): an import,
 * or an explicit re-run of analysis. After a successful commit
 * {@code AnalysisInvokingCommandHandler} reads these and dispatches one analysis request per
 * text entity in the project through the assistant SPI. Checked before
 * {@link AnalysisRequestSource}.
 */
public interface ProjectAnalysisRequestSource {

	/**
	 * @return the project to analyze, or {@code null} to skip analysis for this execution.
	 */
	Project getAnalysisProject();

	/**
	 * @return the user who triggered analysis; authorization for any resulting annotation
	 *         writes mirrors this user.
	 */
	User getAnalysisTriggeredBy();
}
