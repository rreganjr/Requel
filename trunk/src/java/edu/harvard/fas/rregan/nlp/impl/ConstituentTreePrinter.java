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

import edu.harvard.fas.rregan.nlp.GrammaticalStructureLevel;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.ParseTag;

/**
 * An NLPTextWalkerFunction that takes an NLPText and prints out the tree of the
 * phrase stucture. For example:<br>
 * 
 * <pre>
 * 	(ROOT
 *  	(S
 *  		(NP (DT The) (NN system#1))
 *  		(VP (VBZ notifies#1)
 *  			(NP
 *  				(NP (DT the) (NNS stakeholders#1))
 *  				(PP (IN of)
 *  					(NP (NNS changes#4))))
 *  				(PP (TO to)
 *  					(NP (DT the) (NN project#1)))) (. .)))
 * </pre>
 * 
 * <p>
 * The tags, such as S, NP, VP, etc. are based on the Penn Treebank tag set. The
 * (#) indicates the guessed word sense from WordNet.
 * 
 * @see {@link ParseTag}
 * @author ron
 */
public class ConstituentTreePrinter implements NLPTextWalkerFunction<StringBuilder> {

	private final boolean pretty;
	private final int bufferSize;
	private StringBuilder sb;
	private int indent = 0;

	/**
	 * Create a tree printer with an initial buffer size of 1000 characters and
	 * pretty printing on that adds tabs, spaces, and new lines to make the tree
	 * easier to read.
	 */
	public ConstituentTreePrinter() {
		this(1000);
	}

	/**
	 * Create a tree printer with the specified initial buffer size and pretty
	 * printing on that adds tabs, spaces, and new lines to make the tree easier
	 * to read.
	 * 
	 * @param bufferSize -
	 *            the initial size of the buffer for building the tree.
	 */
	public ConstituentTreePrinter(int bufferSize) {
		this(bufferSize, true);
	}

	/**
	 * Create a tree printer with the specified initial buffer size and pretty
	 * printing state.
	 * 
	 * @param bufferSize -
	 *            the initial size of the buffer for building the tree.
	 * @param pretty -
	 *            when true tabs, spaces, and new lines are added to make the
	 *            tree easier to read.
	 */
	public ConstituentTreePrinter(int bufferSize, boolean pretty) {
		this.bufferSize = bufferSize;
		this.pretty = pretty;
	}

	@Override
	public void init() {
		sb = new StringBuilder(bufferSize);
	}

	@Override
	public void begin(NLPText text) {
		if (pretty) {
			if (!GrammaticalStructureLevel.WORD.equals(text.getGrammaticalStructureLevel())) {
				sb.append("\n");
				for (int i = 0; i < indent; i++) {
					sb.append("\t");
				}
			} else {
				sb.append(" ");
			}
		}
		sb.append("(");
		sb.append(text.getParseTag().getText());
		if (GrammaticalStructureLevel.WORD.equals(text.getGrammaticalStructureLevel())) {
			String word = text.getText();
			sb.append(" ");
			if ((word != null) && (word.length() > 0)) {
				sb.append(word);
			}
			if (text.getDictionaryWordSense() != null) {
				sb.append("#");
				sb.append(text.getDictionaryWordSense().getRank());
			}
		}
		if (pretty && !GrammaticalStructureLevel.WORD.equals(text.getGrammaticalStructureLevel())) {
			indent++;
		}
	}

	@Override
	public StringBuilder end(NLPText t) {
		sb.append(")");
		if (pretty && !GrammaticalStructureLevel.WORD.equals(t.getGrammaticalStructureLevel())) {
			indent--;
		}
		return sb;
	}
}
