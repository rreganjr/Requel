/*
 * $Id: SemanticRoleCollector.java,v 1.1 2009/02/08 13:25:11 rregan Exp $
 * Copyright (c) 2009 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl.srl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.SemanticRole;
import edu.harvard.fas.rregan.nlp.impl.AbstractNLPTextWalker;
import edu.harvard.fas.rregan.nlp.impl.NLPTextWalkerFunction;

/**
 * @author ron
 */
public class SemanticRoleCollector extends
		AbstractNLPTextWalker<Map<SemanticRole, NLPText>, Collection<NLPText>> {

	private NLPText root;

	/**
	 * @param function
	 */
	public SemanticRoleCollector(NLPTextWalkerFunction<Collection<NLPText>> function) {
		super(function);
	}

	@Override
	public Map<SemanticRole, NLPText> process(NLPText text) {
		root = text;
		return super.process(text);
	}

	@Override
	public Map<SemanticRole, NLPText> transformResults(Collection<NLPText> result) {
		Map<SemanticRole, NLPText> map = new HashMap<SemanticRole, NLPText>();
		for (NLPText roleFiller : result) {
			map.put(roleFiller.getSemanticRole(root.getPrimaryVerb()), roleFiller);
		}
		return map;
	}

}
