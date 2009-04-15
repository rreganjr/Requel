/*
 * $Id: AbstractSenseRelationInfo.java,v 1.1 2008/12/14 11:36:17 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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