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
package com.rreganjr.requel.annotation.command;

import com.rreganjr.platform.command.EditCommand;
import com.rreganjr.requel.annotation.Annotatable;

/**
 * Unlink an annotatable entity from <em>every</em> annotation, reading the
 * database's current state rather than the session's, and delete any annotation
 * left with no annotatables (issue #247).
 * <p>
 * The last step of an entity delete cascade, run immediately before the entity
 * row itself is deleted. The per-annotation
 * {@link RemoveAnnotationFromAnnotatableCommand} loop that precedes it only sees
 * the annotations loaded with the entity; an annotation a background assistant
 * links to the entity after that load - but before the delete - would otherwise
 * survive in the {@code <table>_annotations} join table and fail the delete with
 * a foreign-key violation (the e2e {@code goals_annotations} /
 * {@code stories_annotations} 409s).
 * <p>
 * Internal to delete cascades; not exposed through the command API or gateway.
 *
 * @author ron
 */
public interface RemoveAllAnnotationsFromAnnotatableCommand extends EditCommand {

	/**
	 * @param annotatable
	 *            the entity about to be deleted.
	 */
	public void setAnnotatable(Annotatable annotatable);

	/**
	 * @return the entity about to be deleted.
	 */
	public Annotatable getAnnotatable();
}
