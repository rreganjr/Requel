/*
 * $Id: ConstituentTreeDepthFinder.java,v 1.1 2009/03/22 11:08:21 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.impl;

import edu.harvard.fas.rregan.nlp.NLPText;

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
