/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
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
package com.rreganjr.nlp.impl;

import com.rreganjr.nlp.NLPText;

/**
 * An NLPTextWalkerFunction that takes an NLPText and returns the maximum depth
 * of the syntax structure.
 * 
 * @author ron
 */
public class ConstituentTreeDepthFinder implements NLPTextWalkerFunction<Integer> {

	private int currentLevel = 0;
	private int maxLevel = 0;

	/**
	 * Create a tree printer with an initial buffer size of 1000 characters and
	 * pretty printing on that adds tabs, spaces, and new lines to make the tree
	 * easier to read.
	 */
	public ConstituentTreeDepthFinder() {
	}

	@Override
	public void init() {
		currentLevel = 0;
		maxLevel = 0;
	}

	@Override
	public void begin(NLPText text) {
		currentLevel++;
		if (currentLevel > maxLevel) {
			maxLevel++;
		}
	}

	@Override
	public Integer end(NLPText t) {
		currentLevel--;
		return maxLevel;
	}
}
