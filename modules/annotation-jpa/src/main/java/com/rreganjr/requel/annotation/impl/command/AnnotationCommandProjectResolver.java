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
package com.rreganjr.requel.annotation.impl.command;

import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Argument;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;

/**
 * Resolves the owning {@link Project} for an annotation-layer entity, so annotation commands can
 * implement {@code ProjectScopedCommand} for per-stakeholder authorization (issue #69 Slice 2).
 * An annotation has no direct project reference; the project is reached through its grouping
 * object or its annotatables (or, for positions/arguments, through the issue(s) they belong to).
 * Returns {@code null} when no {@link Project} can be reached (e.g. a domain-scoped annotation),
 * which the authorizing handler treats as "not a project stakeholder".
 */
final class AnnotationCommandProjectResolver {

	private AnnotationCommandProjectResolver() {
	}

	static Project of(Annotation annotation) {
		if (annotation == null) {
			return null;
		}
		if (annotation.getGroupingObject() instanceof Project project) {
			return project;
		}
		for (Annotatable annotatable : annotation.getAnnotatables()) {
			Project project = ofAnnotatable(annotatable);
			if (project != null) {
				return project;
			}
		}
		return null;
	}

	static Project of(Position position) {
		if (position == null) {
			return null;
		}
		for (Issue issue : position.getIssues()) {
			Project project = of(issue);
			if (project != null) {
				return project;
			}
		}
		return null;
	}

	static Project of(Argument argument) {
		return argument == null ? null : of(argument.getPosition());
	}

	static Project ofAnnotatable(Annotatable annotatable) {
		if (annotatable instanceof ProjectOrDomainEntity entity
				&& entity.getProjectOrDomain() instanceof Project project) {
			return project;
		}
		return null;
	}
}
