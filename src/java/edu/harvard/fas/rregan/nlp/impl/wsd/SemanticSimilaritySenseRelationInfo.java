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
package edu.harvard.fas.rregan.nlp.impl.wsd;

import edu.harvard.fas.rregan.nlp.dictionary.Sense;

/**
 * @author ron
 */
public class SemanticSimilaritySenseRelationInfo extends AbstractSenseRelationInfo {

	protected SemanticSimilaritySenseRelationInfo(Sense sense1, Sense sense2, double similarity,
			String reason) {
		super(sense1, sense2, similarity, reason);
	}

	@Override
	public String toString() {
		return "similarity(" + getSense1() + ", " + getSense2() + ") -> " + getRank() + " {"
				+ getReason() + "}";
	}
}