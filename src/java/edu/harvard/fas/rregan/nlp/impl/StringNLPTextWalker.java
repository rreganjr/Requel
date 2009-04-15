/*
 * $Id: StringNLPTextWalker.java,v 1.4 2009/03/22 11:08:22 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl;

/**
 * A driver for an NLPTextWalkerFunction that converts the final StringBuilder
 * returned by the function to a String.
 * 
 * @author ron
 */
public class StringNLPTextWalker extends AbstractNLPTextWalker<String, StringBuilder> {

	/**
	 * @param function -
	 *            a NLPTextWalkerFunction that returns a StringBuilder
	 */
	public StringNLPTextWalker(NLPTextWalkerFunction<StringBuilder> function) {
		super(function);
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.impl.AbstractNLPTextWalker#transformResults(java.lang.Object)
	 */
	@Override
	public String transformResults(StringBuilder stringBuilder) {
		return stringBuilder.toString();
	}
}
