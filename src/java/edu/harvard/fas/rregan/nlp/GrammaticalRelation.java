/*
 * $Id: GrammaticalRelation.java,v 1.3 2008/07/23 10:05:21 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp;

/**
 * @author ron
 */
public interface GrammaticalRelation extends Comparable<GrammaticalRelation> {

	/**
	 * @return The type of grammatical relationship, such as subject, modifier,
	 *         etc.
	 */
	public GrammaticalRelationType getType();

	/**
	 * @return The governor or "from" element of the relationship
	 */
	public NLPText getGovernor();

	/**
	 * @return The dependent or "to" element of the relationship
	 */
	public NLPText getDependent();
}