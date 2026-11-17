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

import java.util.List;

import com.rreganjr.platform.identity.User;

/**
 * The ideas submitted to each project. The interface is in the domain; the JPA implementation
 * and the project delete path are in project-jpa.
 */
public interface IdeaStore {

	/** Save a new idea in {@link Idea.Status#SUBMITTED}. */
	Idea record(Long projectId, String text, String sourceFileName, User createdBy);

	/** The project's ideas, newest first. */
	List<Idea> list(Long projectId);

	/** Delete a project's ideas, when it is deleted. */
	int deleteForProject(Long projectId);
}
