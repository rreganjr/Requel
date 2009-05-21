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
package edu.harvard.fas.rregan.requel.annotation;

import java.util.Date;
import java.util.Set;

import edu.harvard.fas.rregan.requel.user.User;

/**
 * @author ron
 */
public interface Issue extends Annotation {
	/**
	 * @return The position options for resolving the issue.
	 */
	public Set<Position> getPositions();

	/**
	 * @return if the issue is resolved, the position used to resolve it.
	 */
	public Position getResolvedByPosition();

	/**
	 * @return if the issue is resolved, the user that resolve it.
	 */
	public User getResolvedByUser();

	/**
	 * @return if the issue is resolved, the date it was resolve.
	 */
	public Date getResolvedDate();

	/**
	 * Resolve this issue with the given position and user. The resolved date is
	 * set to the current time.
	 * 
	 * @param resolvedByPosition
	 * @param resolvedByUser
	 */
	public void resolve(Position resolvedByPosition, User resolvedByUser);

	/**
	 * Clear the resolution of the issue position, user and date.<br>
	 * TODO: undo the action of the resolving position.
	 */
	public void unresolve();
}
