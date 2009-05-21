/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
 * Elicitation System.
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
package edu.harvard.fas.rregan.requel.project;

import java.util.Set;

/**
 * A GlossaryTerm is an entry in a glossary that defines a specific term. The
 * "name" of the entry must be unique in the glossary.<br>
 * Multiple terms may refer to the same concept and are linked to a canonical
 * term, which represents the primary term.
 * 
 * @author ron
 */
public interface GlossaryTerm extends TextEntity {

	/**
	 * @return The term that is considered the primary/prefered way of
	 *         referencing this concept.
	 */
	public GlossaryTerm getCanonicalTerm();

	/**
	 * @return The set of terms that refer to this term as the canonical term.
	 */
	public Set<GlossaryTerm> getAlternateTerms();

	/**
	 * @return project or domain entities that refer to this term, such as goals
	 */
	public Set<ProjectOrDomainEntity> getReferers();
}
