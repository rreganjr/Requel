/*
 * $Id: AbstractNLPTextWalker.java,v 1.2 2009/01/26 10:19:00 rregan Exp $
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
