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
package com.rreganjr.requel.project.command;

import java.util.List;

import com.rreganjr.platform.command.EditCommand;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectScopedCommand;

/**
 * Issue #348: replace a synonym of a glossary term with the term's name in the term's referers.
 * After a rename the old name is kept as a synonym; this rewrites the stories, goals and other
 * referers that still say it. Each change is the referer's own {@code Edit*} command, so it is
 * authorized like any edit: a referer the caller may not edit is refused and the rest are
 * applied.
 */
public interface ReplaceTermTextCommand extends EditCommand, ProjectScopedCommand {

	/** A referer to change, by entity type (e.g. {@code Story}) and id. */
	record Target(String entityType, Long entityId) {
	}

	/** What happened to one referer. The name is null for one that isn't a referer. */
	record Outcome(String entityType, Long entityId, String name) {
	}

	void setProject(Project project);

	/** The canonical term, whose name replaces the synonym. */
	void setGlossaryTerm(GlossaryTerm term);

	/** The synonym to replace; must be one of the term's alternate terms. */
	void setSynonym(GlossaryTerm synonym);

	/**
	 * The referers to change. Text that says the old name is linked to the synonym, so the
	 * term's and the synonym's referers both count; anything else is left unchanged.
	 */
	void setTargets(List<Target> targets);

	/** Referers changed, each through its own edit command. Read after execution. */
	List<Outcome> getApplied();

	/** Referers the caller may not edit; nothing was written to them. */
	List<Outcome> getRefused();

	/** Targets that aren't referers, or whose name and text no longer contain the synonym. */
	List<Outcome> getUnchanged();
}
