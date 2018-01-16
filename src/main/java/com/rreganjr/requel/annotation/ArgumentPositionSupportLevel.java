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
package com.rreganjr.requel.annotation;

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
