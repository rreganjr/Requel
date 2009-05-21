/*
 * $Id$
 * Copyright (c) 2009 Ron Regan 
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
