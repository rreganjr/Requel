/*
 * $Id: GlossaryTerm.java,v 1.7 2008/11/20 09:55:12 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
