/*
 * $Id: AbstractSenseRelationInfo.java,v 1.1 2008/12/14 11:36:17 rregan Exp $
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
public class AbstractSenseRelationInfo implements SenseRelationInfo {

	private final Sense sense1;
	private final Sense sense2;
	private final double rank;
	private final String reason;

	protected AbstractSenseRelationInfo(Sense sense1, Sense sense2, double rank, String reason) {
		this.sense1 = sense1;
		this.sense2 = sense2;
		this.rank = rank;
		this.reason = reason;
	}

	public Sense getSense1() {
		return sense1;
	}

	public Sense getSense2() {
		return sense2;
	}

	public double getRank() {
		return rank;
	}

	public String getReason() {
		return reason;
	}

	@Override
	public int hashCode() {
		return getSense1().hashCode() + getSense2().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if ((o instanceof AbstractSenseRelationInfo) && getClass().equals(o.getClass())) {
			AbstractSenseRelationInfo other = (AbstractSenseRelationInfo) o;
			return (getSense1().equals(other.getSense1()) && (getSense2().equals(other.getSense2())))
					|| (getSense1().equals(other.getSense2()) && (getSense2().equals(other
							.getSense1())));
		}
		return false;
	}
}