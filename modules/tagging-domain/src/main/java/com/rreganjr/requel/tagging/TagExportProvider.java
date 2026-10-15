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
package com.rreganjr.requel.tagging;

import java.util.List;

/**
 * SPI seam for project export: given a project (opaque {@code Object}), return every tag assignment
 * on the project and its entities as neutral {@link TagExportAssignment}s. Implemented in
 * {@code tagging-jpa}; the export command references only this contract (via {@code tagging-domain}),
 * so {@code project-jpa} never depends on the tagging JPA module. Mirrors {@link TagImportHandler}.
 *
 * @author ron
 */
public interface TagExportProvider {

	/**
	 * @param project the project being exported (a {@code ProjectOrDomain})
	 * @return the tag assignments to emit; empty if none or if {@code project} is not a project
	 */
	List<TagExportAssignment> exportAssignmentsFor(Object project);
}
