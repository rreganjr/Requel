/*
 * $Id: NLPProcessor.java,v 1.5 2009/01/24 11:08:40 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp;

/**
 * @param <T> -
 *            the type of result from the process method.
 * @author ron
 */
public interface NLPProcessor<T> {

	/**
	 * Apply this processor to the supplied NLPText.
	 * 
	 * @param text
	 * @return a processor specific type
	 */
	public T process(NLPText text);
}
