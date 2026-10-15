/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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

import com.rreganjr.platform.CreatedEntity;

/**
 * An argument for or against a Position of an Annotation.
 * 
 * @author ron
 */
public interface Argument extends Comparable<Argument>, CreatedEntity {

	/**
	 * @return the persistent identity of this argument, or {@code null} for
	 *         transient instances.
	 */
	public Long getId();

	/**
	 * @return the optimistic-lock version; incremented on each
	 *         user-initiated modification.
	 */
	public int getVersion();

	/**
	 * The position that the argument is for or against.
	 *
	 * @return
	 */
	public Position getPosition();

	/**
	 * @return the text of the argument describing why a position should be or
	 *         should not be chosen as the resolution to an issue.
	 */
	public String getText();

	/**
	 * @return the level at which this argument is for or against the position.
	 */
	public ArgumentPositionSupportLevel getSupportLevel();
}
