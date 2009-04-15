/*
 * $Id: SemanticRoleCollectorFunction.java,v 1.2 2009/02/09 10:12:29 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.impl.srl;

import java.util.Collection;
import java.util.HashSet;

import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.SemanticRole;
import edu.harvard.fas.rregan.nlp.impl.NLPTextWalkerFunction;

/**
 * An NLPTextWalkerFunction that takes an NLPText and returns a Collection of
 * the NLPTexts that fill SemanticRoles of the primary verb.
 * 
 * @see {@link SemanticRole}
 * @author ron
 */
public class SemanticRoleCollectorFunction implements NLPTextWalkerFunction<Collection<NLPText>> {

	private final Collection<NLPText> roleFillers;
	private final NLPText verb;

	/**
	 * Create a tree printer with an initial buffer size of 1000 characters and
	 * pretty printing on that adds tabs, spaces, and new lines to make the tree
	 * easier to read.
	 * 
	 * @param verb -
	 *            the verb to get the semantic role for.
	 */
	public SemanticRoleCollectorFunction(NLPText verb) {
		this(verb, new HashSet<NLPText>());
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
	public SemanticRoleCollectorFunction(NLPText verb, Collection<NLPText> roleFillers) {
		this.verb = verb;
		this.roleFillers = roleFillers;
	}

	@Override
	public void init() {
	}

	@Override
	public void begin(NLPText text) {
		if (text.getSemanticRole(verb) != null) {
			roleFillers.add(text);
		}
	}

	@Override
	public Collection<NLPText> end(NLPText t) {
		return roleFillers;
	}
}
