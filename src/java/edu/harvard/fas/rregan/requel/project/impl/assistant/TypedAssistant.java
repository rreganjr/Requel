/*
 * $Id: TypedAssistant.java,v 1.1 2009/01/20 10:26:01 rregan Exp $
 * Copyright (c) 2009 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.impl.assistant;


/**
 * @param <T> -
 *            the type of entity the assistant analyzes.
 * @author ron
 */
public interface TypedAssistant<T> {

	/**
	 * @return The entity being analyzed.
	 */
	public T getEntity();

	/**
	 * @param entity -
	 *            the entity to analyze
	 */
	public void setEntity(T entity);

	/**
	 * Start the analysis of the supplied entity.
	 */
	public void analyze();

}
