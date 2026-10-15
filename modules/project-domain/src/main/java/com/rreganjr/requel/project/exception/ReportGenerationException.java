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

package com.rreganjr.requel.project.exception;

/**
 * Issue #275: a report generator could not produce its document — a transform error, a reference
 * the template could not resolve, or an entity it asked for by name that is not in the project.
 * The message names the cause (the template's own {@code xsl:message} text when it stopped
 * itself), and no partial output was written.
 */
public class ReportGenerationException extends RuntimeException {
	static final long serialVersionUID = 0;

	/** Error code returned over REST (422). */
	public static final String CODE = "REPORT_FAILED";

	public ReportGenerationException(String message) {
		super(message);
	}

	public ReportGenerationException(String message, Throwable cause) {
		super(message, cause);
	}
}
