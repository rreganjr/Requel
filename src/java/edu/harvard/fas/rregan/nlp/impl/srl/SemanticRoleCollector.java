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
