/*
 * $Id: GrammaticalStructureLevel.java,v 1.2 2008/10/31 06:09:55 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp;

/**
 * TODO: role this into GrammaticalRelationType
 * 
 * @author ron
 */
public enum GrammaticalStructureLevel {
	/**
	 * Default if the level of the text is unknown.
	 */
	UNKNOWN(),

	/**
	 * 
	 */
	PARAGRAPH(),

	/**
	 * A collection of words without any particular grammatical relation between
	 * them. This may be used by NLPProcessors as a utility for doing its work.
	 */
	BAGOFWORDS(),

	/**
	 * 
	 */
	SENTENCE(),

	/**
	 * A clause is a collection of grammatically-related words including a
	 * predicate and a subject
	 */
	CLAUSE(),

	/**
	 * A phrase is a group of two or more grammatically linked words without a
	 * subject and predicate
	 */
	PHRASE(),

	/**
	 * 
	 */
	WORD();
}
