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

package edu.harvard.fas.rregan.nlp;

/**
 * @author ron
 */
public enum SemanticRole {
	/**
	 * 
	 */
	PARTICIPANT(),

	/**
	 * 
	 */
	AGENT(PARTICIPANT),

	/**
	 * 
	 */
	ACTOR(AGENT),

	/**
	 * 
	 */
	ACTOR1(ACTOR),

	/**
	 * 
	 */
	ACTOR2(ACTOR),

	/**
	 * 
	 */
	EXPERIENCER(AGENT),

	/**
	 * 
	 */
	CAUSE(AGENT),

	/**
	 * 
	 */
	PATIENT(PARTICIPANT),

	/**
	 * 
	 */
	PATIENT1(PATIENT),

	/**
	 * 
	 */
	PATIENT2(PATIENT),

	/**
	 * 
	 */
	BENEFICIARY(PATIENT),

	/**
	 * 
	 */
	PRODUCT(PATIENT),

	/**
	 * 
	 */
	RECIPIENT(PATIENT),

	/**
	 * 
	 */
	THEME(PARTICIPANT),

	/**
	 * 
	 */
	THEME1(THEME),

	/**
	 * 
	 */
	THEME2(THEME),

	/**
	 * 
	 */
	INSTRUMENT(THEME),

	/**
	 * 
	 */
	MATERIAL(THEME),

	/**
	 * 
	 */
	STIMULUS(THEME),

	/**
	 * 
	 */
	LOCATION(THEME),

	/**
	 * 
	 */
	DESTINATION(LOCATION),

	/**
	 * 
	 */
	SOURCE(LOCATION),

	/**
	 * 
	 */
	ASSET(THEME),

	/**
	 * 
	 */
	ATTRIBUTE(),

	/**
	 * 
	 */
	PREDICATE(ATTRIBUTE),

	/**
	 * 
	 */
	VALUE(),

	/**
	 * 
	 */
	EXTENT(VALUE),

	/**
	 * 
	 */
	TIME(VALUE),

	/**
	 * TODO: should this be a theme?
	 */
	TOPIC(),
	/**
	 * 
	 */
	PROPOSITION(TOPIC),

	/**
	 * I don't know what this represents in VerbNet
	 */
	OBLIQUE(),

	/**
	 * The primary verb or a sentence or clause.
	 */
	VERB();

	private final SemanticRole parent;

	private SemanticRole(SemanticRole parent) {
		this.parent = parent;
	}

	private SemanticRole() {
		this.parent = null;
	}

	/**
	 * @return the super type of semantic role or null if this is a base role.
	 */
	public SemanticRole getParent() {
		return parent;
	}

	/**
	 * @param role
	 * @return true if this role is a descendant of, or is the supplied role.
	 */
	public boolean isA(SemanticRole role) {
		SemanticRole thisOrAncestor = this;
		do {
			if (thisOrAncestor.equals(role)) {
				return true;
			}
			thisOrAncestor = thisOrAncestor.getParent();
		} while (thisOrAncestor != null);
		return false;
	}
}
