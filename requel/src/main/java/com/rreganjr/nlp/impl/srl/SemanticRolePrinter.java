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
package com.rreganjr.nlp.impl.srl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rreganjr.nlp.NLPText;
import com.rreganjr.nlp.SemanticRole;
import com.rreganjr.nlp.impl.NLPTextWalkerFunction;

/**
 * An NLPTextWalkerFunction that takes an NLPText and prints out the semantic
 * roles.
 * 
 * <pre>
 * </pre>
 * 
 * <p>
 * 
 * @see {@link SemanticRole}
 * @author ron
 */
public class SemanticRolePrinter implements NLPTextWalkerFunction<StringBuilder> {
	private static final Log log = LogFactory.getLog(SemanticRolePrinter.class);

	private final NLPText verb;
	private final int bufferSize;
	private StringBuilder sb;

	public SemanticRolePrinter() {
		this(null);
	}

	/**
	 * @param verb -
	 *            the verb to get the semantic role for.
	 */
	public SemanticRolePrinter(NLPText verb) {
		this(verb, 1000);
	}

	/**
	 * Create a tree printer with the specified initial buffer size and pretty
	 * printing on that adds tabs, spaces, and new lines to make the tree easier
	 * to read.
	 * 
	 * @param verb -
	 *            the verb to get the semantic role for.
	 * @param bufferSize -
	 *            the initial size of the buffer for building the tree.
	 */
	public SemanticRolePrinter(NLPText verb, int bufferSize) {
		this.verb = verb;
		this.bufferSize = bufferSize;
	}

	@Override
	public void init() {
		sb = new StringBuilder(bufferSize);
	}

	@Override
	public void begin(NLPText text) {
		if (verb != null) {
			appendSemanticRoleForVerb(verb, text);
		} else {
			for (NLPText verb : text.getSupportedVerbs()) {
				appendSemanticRoleForVerb(verb, text);
			}
		}
	}

	@Override
	public StringBuilder end(NLPText t) {
		return sb;
	}

	private void appendSemanticRoleForVerb(NLPText verb, NLPText text) {
		SemanticRole thisSemanticRole = text.getSemanticRole(verb);
		if (thisSemanticRole != null) {
			sb.append(verb);
			sb.append("[");
			sb.append(thisSemanticRole);
			sb.append("]");
			sb.append(text);
			sb.append("\n");
		}
	}
}
