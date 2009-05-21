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

import edu.harvard.fas.rregan.nlp.GrammaticalRelation;
import edu.harvard.fas.rregan.nlp.GrammaticalRelationType;
import edu.harvard.fas.rregan.nlp.NLPText;

/**
 * @author ron
 */
public class GrammaticalRelationImpl implements GrammaticalRelation {

	private final GrammaticalRelationType type;
	private final NLPText governor;
	private final NLPText dependent;

	/**
	 * @param type
	 * @param governor
	 * @param dependent
	 */
	public GrammaticalRelationImpl(GrammaticalRelationType type, NLPText governor, NLPText dependent) {
		this.type = type;
		this.governor = governor;
		this.dependent = dependent;
		((NLPTextImpl) governor).addGovernorOf(this);
		((NLPTextImpl) dependent).addDependentOf(this);
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.GrammaticalRelation#getType()
	 */
	public GrammaticalRelationType getType() {
		return type;
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.GrammaticalRelation#getGovernor()
	 */
	public NLPText getGovernor() {
		return governor;
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.GrammaticalRelation#getDependent()
	 */
	public NLPText getDependent() {
		return dependent;
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.GrammaticalRelation#compareTo(edu.harvard.fas.rregan.nlp.impl.GrammaticalRelation)
	 */
	public int compareTo(GrammaticalRelation o) {
		if (getDependent().getWordIndex().equals(o.getDependent().getWordIndex())) {
			if (getGovernor().getWordIndex().equals(o.getGovernor().getWordIndex())) {
				return getType().getShortName().compareTo(o.getType().getShortName());
			}
			return (getGovernor().getWordIndex().compareTo(o.getGovernor().getWordIndex()));
		}
		return (getDependent().getWordIndex().compareTo(o.getDependent().getWordIndex()));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getDependent() == null) ? 0 : getDependent().hashCode());
		result = prime * result + ((getGovernor() == null) ? 0 : getGovernor().hashCode());
		result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final GrammaticalRelationImpl other = (GrammaticalRelationImpl) obj;
		if (dependent == null) {
			if (other.dependent != null) {
				return false;
			}
		} else if (!dependent.equals(other.dependent)) {
			return false;
		}
		if (governor == null) {
			if (other.governor != null) {
				return false;
			}
		} else if (!governor.equals(other.governor)) {
			return false;
		}
		if (type == null) {
			if (other.type != null) {
				return false;
			}
		} else if (!type.equals(other.type)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return getType().getShortName() + "(" + getGovernor().getText() + "-"
				+ getGovernor().getWordIndex() + ", " + getDependent().getText() + "-"
				+ getDependent().getWordIndex() + ")";
	}
}
