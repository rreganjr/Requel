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
package com.rreganjr.requel;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Issue #379: reports a refused database as Spring Boot's "APPLICATION FAILED TO START" box,
 * with what to do, instead of a stack trace. Registered in {@code META-INF/spring.factories}.
 */
public class RequelStartupFailureAnalyzer extends AbstractFailureAnalyzer<UnversionedDatabaseException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, UnversionedDatabaseException cause) {
		return new FailureAnalysis(cause.getMessage(),
				"To move a Requel 1.0 or 1.1 database, export each project to XML in the old version,"
						+ " then start Requel 2.0 on a new, empty database and import the files."
						+ " Point spring.datasource.url at an empty database to start fresh."
						+ " See doc/guides/INSTALL.md, \"Upgrading\".",
				cause);
	}
}
