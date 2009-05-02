/*
 * $Id: DependencyPrinter.java,v 1.5 2009/01/28 04:58:59 rregan Exp $
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

import java.util.HashSet;
import java.util.Set;

import edu.harvard.fas.rregan.nlp.GrammaticalRelation;
import edu.harvard.fas.rregan.nlp.GrammaticalStructureLevel;
import edu.harvard.fas.rregan.nlp.NLPText;

/**
 * An NLPTextWalkerFunction that takes an NLPText and prints out the list of
 * dependency relations. For example:<br>
 * 
 * <pre>
 * </pre>
 * 
 * @author ron
 */
public class DependencyPrinter implements NLPTextWalkerFunction<StringBuilder> {

	private final Set<GrammaticalRelation> relations = new HashSet<GrammaticalRelation>();
	private final int bufferSize;
	private StringBuilder sb;

	/**
	 * 
	 */
	public DependencyPrinter() {
		this(1000);
	}

	/**
	 * @param bufferSize
	 */
	public DependencyPrinter(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	@Override
	public void init() {
		sb = new StringBuilder(bufferSize);
		relations.clear();
	}

	@Override
	public void begin(NLPText text) {
		if (GrammaticalStructureLevel.WORD.equals(text.getGrammaticalStructureLevel())) {
			if (text.getDependentOf().size() > 0) {
				for (GrammaticalRelation rel : text.getDependentOf()) {
					if (!relations.contains(rel)) {
						relations.add(rel);
						sb.append("[dep] ");
						sb.append(rel.getDependent().getText());
						sb.append("-");
						sb.append(rel.getDependent().getWordIndex());
						sb.append(" is the ");
						sb.append(rel.getType().getLongName());
						sb.append(" of ");
						sb.append(rel.getGovernor().getText());
						sb.append("-");
						sb.append(rel.getGovernor().getWordIndex());
						sb.append("\n");
					}
				}
			}
		}
	}

	@Override
	public StringBuilder end(NLPText t) {
		return sb;
	}
}
