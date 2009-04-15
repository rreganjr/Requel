/*
 * $Id: IntegerNLPTextWalker.java,v 1.1 2009/03/22 11:08:22 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl;

/**
 * A driver for NLPTextWalkerFunction that returns an Integer value.
 * 
 * @author ron
 */
public class IntegerNLPTextWalker extends AbstractNLPTextWalker<Integer, Integer> {

	/**
	 * @param function -
	 *            a NLPTextWalkerFunction that returns a StringBuilder
	 */
	public IntegerNLPTextWalker(NLPTextWalkerFunction<Integer> function) {
		super(function);
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.impl.AbstractNLPTextWalker#transformResults(java.lang.Object)
	 */
	@Override
	public Integer transformResults(Integer result) {
		return result;
	}
}
