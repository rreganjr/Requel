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
package com.rreganjr.requel.ai;

/**
 * Issue #387: a review was requested that cannot run for the entity - AI is off on the server, the
 * project is not allowlisted, or the review covering its type is switched off in the project. The
 * message is the reason, for a person to read. 409 on REST, CONFLICT on the gateway.
 */
public class AiReviewUnavailableException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** The error code in the 409 body. */
	public static final String CODE = "AI_REVIEW_UNAVAILABLE";

	public AiReviewUnavailableException(String reason) {
		super(reason);
	}
}
