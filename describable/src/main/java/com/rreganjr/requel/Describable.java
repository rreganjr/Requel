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
package com.rreganjr.requel;

import java.util.Comparator;

/**
 * @author ron
 */
public interface Describable {

	/**
	 * Get the des
	 * 
	 * @return The description of the describable object.
	 */
	public String getDescription();

	/**
	 * Compare the objects that contain goals by the description.
	 */
	public static final Comparator<Describable> COMPARATOR = new DescribableComparator();

	/**
	 * A Comparator for collections of goal containers.
	 */
	public static class DescribableComparator implements Comparator<Describable> {
		@Override
		public int compare(Describable o1, Describable o2) {
			return o1.getDescription().compareTo(o2.getDescription());
		}
	}
}
