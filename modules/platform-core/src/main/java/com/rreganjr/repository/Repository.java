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
package com.rreganjr.repository;

import com.rreganjr.platform.exception.EntityException;

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
	 * take an exclusive database lock on the row backing the supplied entity,
	 * blocking until it is available, and return the attached entity.
	 * <p>
	 * Used to serialize a cascading delete against concurrent writers that reach
	 * the same rows by a different path. The lock is held until the surrounding
	 * transaction ends. Both sides of such a race must take this lock on the same
	 * entity <em>first</em>, before touching anything the other side also touches,
	 * or the two orderings deadlock. See issue #279.
	 *
	 * @param <T>
	 * @param entity
	 * @return the attached, locked entity.
	 * @throws EntityException
	 */
	public <T> T lockForUpdate(T entity) throws EntityException;

	/**
	 * force the repository to sync up any pending work, without actually ending
	 * a transaction.
	 * 
	 * @throws EntityException
	 */
	public void flush() throws EntityException;

}
