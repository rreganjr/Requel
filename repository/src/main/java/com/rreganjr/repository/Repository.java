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
package com.rreganjr.repository;

import com.rreganjr.EntityException;

/**
 * A generic set of methods common to all repositories.
 * 
 * @author ron
 */
public interface Repository {

	/**
	 * add an object to the repository.
	 * 
	 * @param <T>
	 * @param entity -
	 *            the object to persist
	 * @return an up to date copy of the entity.
	 * @throws EntityException
	 */
	public <T> T persist(T entity) throws EntityException;

	/**
	 * take a previously persisted object and update its persisted copy in the
	 * repository with the supplied copy and return the latest version.
	 * 
	 * @param <T>
	 * @param entity
	 * @return
	 * @throws EntityException
	 */
	public <T> T merge(T entity) throws EntityException;

	/**
	 * takes a previously persisted object and returns the latest version from
	 * the database.
	 * 
	 * @param <T>
	 * @param entity
	 * @return
	 * @throws EntityException
	 */
	public <T> T get(T entity) throws EntityException;

	/**
	 * takes a persistent object and initializes any lazy loaded properties.
	 * 
	 * @param <T>
	 * @param entity
	 * @return
	 * @throws EntityException
	 */
	public <T> T initialize(T entity) throws EntityException;

	/**
	 * remove the supplied object from the repository.
	 * 
	 * @param entity
	 * @throws EntityException
	 */
	public void delete(Object entity) throws EntityException;

	/**
	 * force the repository to sync up any pending work, without actually ending
	 * a transaction.
	 * 
	 * @throws EntityException
	 */
	public void flush() throws EntityException;

}
