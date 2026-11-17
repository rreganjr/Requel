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

import java.util.Date;

import com.rreganjr.platform.identity.User;

/**
 * A rough requirement idea submitted to a project, as text or as the text of an uploaded file.
 * It starts {@link Status#SUBMITTED}; later work (decomposition, clarification) moves it on.
 */
public interface Idea {

	enum Status {
		SUBMITTED, CLARIFYING, BREAKDOWN_REVIEW, COMPLETE
	}

	java.util.Set<Requirement> getDerivedRequirements();

	Long getId();

	Long getProjectId();

	/** The idea text; when a file was uploaded, its text follows the typed text. */
	String getText();

	/** The name of the uploaded file, or null when the idea was typed only. */
	String getSourceFileName();

	Status getStatus();

	User getCreatedBy();

	Date getDateCreated();
}