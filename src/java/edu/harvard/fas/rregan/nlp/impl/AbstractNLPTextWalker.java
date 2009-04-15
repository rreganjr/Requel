/*
 * $Id: AbstractNLPTextWalker.java,v 1.2 2009/01/26 10:19:00 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.impl;

import edu.harvard.fas.rregan.nlp.NLPProcessor;
import edu.harvard.fas.rregan.nlp.NLPText;

/**
 * An NLPProcessor that takes an NLPText and applies an NLPTextWalkerFunction to
 * every node in the text and returns the results from the
 * NLPTextWalkerFunction's call to the end() method on the root node,
 * transforming them to type T from type F.
 * 
 * @param <T> -
 *            the return type of the process method
 * @param <F> -
 *            the return type of the NLPTextWalkerFunction
 * @author ron
 */
public abstract class AbstractNLPTextWalker<T, F> implements NLPProcessor<T> {

	private final NLPTextWalkerFunction<F> visitorFunction;

	/**
	 * @param visitorFunction
	 */
	public AbstractNLPTextWalker(NLPTextWalkerFunction<F> visitorFunction) {
		this.visitorFunction = visitorFunction;
	}

	@Override
	public T process(NLPText text) {
		visitorFunction.init();
		return recursiveProcess(text);
	}

	private T recursiveProcess(NLPText text) {
		visitorFunction.begin(text);
		for (NLPText child : text.getChildren()) {
			recursiveProcess(child);
		}
		return transformResults(visitorFunction.end(text));
	}

	/**
	 * This method must be overridden to transform the results of the
	 * NLPTextWalkerFunction to the result type of the NLPTextWalker process()
	 * method.
	 * 
	 * @param result
	 * @return
	 */
	public abstract T transformResults(F result);
}
