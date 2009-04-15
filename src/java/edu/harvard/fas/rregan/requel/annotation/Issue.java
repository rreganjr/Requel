/*
 * $Id: Issue.java,v 1.7 2009/02/23 09:39:55 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
