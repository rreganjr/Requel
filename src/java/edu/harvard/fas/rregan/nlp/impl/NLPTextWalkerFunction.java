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
package edu.harvard.fas.rregan.nlp.impl;

import edu.harvard.fas.rregan.nlp.NLPText;

/**
 * A visitor for NLPText nodes called at the start and end of visiting each node
 * by an TextWalker NLPTextProcessor.
 * 
 * @param <R> -
 *            the return type of the results at the end of processing a node.
 * @author ron
 */
public interface NLPTextWalkerFunction<R> {

	/**
	 * This method gets called before the first call to begin() when a text is
	 * passed in for processing.
	 */
	public void init();

	/**
	 * This method gets called on a node when it is first visited.
	 * 
	 * @param node -
	 *            the NLPText node.
	 */
	public void begin(NLPText node);

	/**
	 * This method gets called on a node after all it's children have been
	 * visited.
	 * 
	 * @param node -
	 *            the NLPText node.
	 * @return
	 */
	public R end(NLPText node);
}