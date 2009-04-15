/*
 * $Id: SemanticRole.java,v 1.4 2009/02/10 10:26:04 rregan Exp $
 * Copyright (c) 2009 Ron Regan Jr. All Rights Reserved.
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
