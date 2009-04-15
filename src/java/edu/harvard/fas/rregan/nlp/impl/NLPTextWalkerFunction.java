/*
 * $Id: NLPTextWalkerFunction.java,v 1.1 2009/01/26 10:19:01 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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