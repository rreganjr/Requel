/*
 * $Id: Position.java,v 1.9 2008/08/08 08:01:11 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.annotation;

import java.util.Set;

import edu.harvard.fas.rregan.requel.CreatedEntity;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * A position on how to resolve an issue.
 * 
 * @author ron
 */
public interface Position extends Comparable<Position>, CreatedEntity {

	/**
	 * @return All the issues that have this position as a resolution option.
	 */
	public Set<Issue> getIssues();

	/**
	 * @return the text describing how to resolve an issue.
	 */
	public String getText();

	/**
	 * @return The arguments that are for and against this position.
	 */
	public Set<Argument> getArguments();

	/**
	 * Resolve the supplied issue with this position.
	 * 
	 * @param issue -
	 *            the issue to resolve.
	 * @param resolvedByUser -
	 *            the user that initiated the resolution.
	 * @throws Exception -
	 *             TODO: what can be thrown?
	 */
	public void resolveIssue(Issue issue, User resolvedByUser) throws Exception;

	/**
	 * Resolve all issues with this position.
	 * 
	 * @param resolvedByUser
	 * @throws Exception
	 */
	public void resolveAllIssue(User resolvedByUser) throws Exception;
}
