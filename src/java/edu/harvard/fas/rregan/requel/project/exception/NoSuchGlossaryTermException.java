/*
 * $Id: NoSuchGlossaryTermException.java,v 1.3 2008/12/13 00:40:02 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.exception;

import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.NoSuchEntityException;
import edu.harvard.fas.rregan.requel.project.GlossaryTerm;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;

/**
 * @author ron
 */
public class NoSuchGlossaryTermException extends NoSuchEntityException {
	static final long serialVersionUID = 0;

	protected static String MSG_FOR_NAME = "No glossary term for %s with name '%s'";

	/**
	 * @param projectOrDomain
	 * @param name -
	 *            the unknown word text.
	 * @return
	 */
	public static NoSuchGlossaryTermException forProjectOrDomainWithName(
			ProjectOrDomain projectOrDomain, String name) {
		return new NoSuchGlossaryTermException(GlossaryTerm.class, null, "name", name,
				EntityExceptionActionType.Reading, MSG_FOR_NAME, projectOrDomain, name);
	}

	/**
	 * @param format
	 * @param args
	 */
	protected NoSuchGlossaryTermException(Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(entityType, entity, entityPropertyName, entityValue, actionType, format, messageArgs);
	}

	/**
	 * @param cause
	 * @param format
	 * @param args
	 */
	protected NoSuchGlossaryTermException(Throwable cause, Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyName, entityValue, actionType, format,
				messageArgs);
	}
}
