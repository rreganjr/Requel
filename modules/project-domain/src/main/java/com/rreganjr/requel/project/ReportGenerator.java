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
package com.rreganjr.requel.project;

/**
 * @author ron
 */
public interface ReportGenerator extends TextEntity, Comparable<ReportGenerator> {

	/**
	 * Issue #275: the key of the bundled generator this one renders ("project-html",
	 * "ticket-markdown"), or null for a generator whose own text is the template. A keyed generator
	 * always renders the current bundled template; editing its text detaches it (clears the key).
	 */
	String getBuiltinKey();
}
