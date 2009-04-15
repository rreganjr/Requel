package edu.harvard.fas.rregan.requel.annotation;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * Arguments can be for or against a position, this enumerates the level of
 * support.
 * 
 * @author ron
 */
@XmlEnum()
@XmlType(namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public enum ArgumentPositionSupportLevel {

	/**
	 * This argument is in strong support of this position.
	 */
	StronglyFor(2),

	/**
	 * This argument is in support of the position.
	 */
	For(1),

	/**
	 * This argument is neither for or against the position.
	 */
	Neutral(0),

	/**
	 * This argument is against the position.
	 */
	Against(-1),

	/**
	 * This argument is strongly against the position.
	 */
	StronglyAgainst(-2);

	private int supportLevel;

	private ArgumentPositionSupportLevel(int supportLevel) {
		this.supportLevel = supportLevel;
	}

	public int supportLevel() {
		return supportLevel;
	}
}
