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
package com.rreganjr.repository.jpa;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.repository.AbstractRepository;
import com.rreganjr.repository.EntityException;
import com.rreganjr.repository.EntityExceptionActionType;

/**
 * @author ron
 */
@Transactional(propagation = Propagation.REQUIRED)
public class AbstractJpaRepository extends AbstractRepository {

	@PersistenceContext
	private EntityManager entityManager;

	protected AbstractJpaRepository(ExceptionMapper exceptionMapper) {
		super(exceptionMapper);
	}

	protected EntityManager getEntityManager() {
		return entityManager;
	}

	@Override
	public <T> T persist(T entity) throws EntityException {
		try {
			entityManager.persist(entity);
			return entity;
		} catch (Exception e) {
			log.warn(e, e);
			throw convertException(e, entity.getClass(), entity, EntityExceptionActionType.Creating);
		}
	}

	/**
	 * Get the latest version of the supplied entity.
	 */
	@Override
	public <T> T get(T entity) throws EntityException {
		if (entity != null) {
			try {
				return attach(entityManager, entity);
			} catch (Exception e) {
				log.warn(e, e);
				throw convertException(e, entity.getClass(), entity,
						EntityExceptionActionType.Reading);
			}
		}
		return null;
	}

	/**
	 * This is a hibernate specific implementation that loads proxied properties
	 * and collections of the supplied entity. NOTE: only a single level is
	 * loaded, for example the elements of a collection but not the lazy
	 * properties of those elements. <br>
	 * TODO: this is very expensive
	 */
	@Override
	public <T> T initialize(T entity) throws EntityException {
		if (entity != null) {
			try {
				T attached = attach(entityManager, entity);
				// walk up the class hierarchy
				Class<?> entityType = attached.getClass();
				while (!entityType.getName().equals(Object.class.getName())) {
					for (Field field : entityType.getDeclaredFields()) {
						if (log.isDebugEnabled()) {
							log.debug("initializing: " + attached + " field " + field.getName()
									+ " " + field.getType());
						}
						field.setAccessible(true);
						Hibernate.initialize(field.get(attached));
					}
					entityType = entityType.getSuperclass();
				}
				Hibernate.initialize(attached);
				return attached;
			} catch (Exception e) {
				log.warn(e, e);
				throw convertException(e, entity.getClass(), entity,
						EntityExceptionActionType.Reading);
			}
		}
		return null;
	}

	/**
	 * Save the state of the supplied entity to the database.
	 */
	@Override
	public <T> T merge(T entity) throws EntityException {
		if (entity != null) {
			try {
				return entityManager.merge(entity);
			} catch (Exception e) {
				log.warn(e, e);
				throw convertException(e, entity.getClass(), entity,
						EntityExceptionActionType.Updating);
			}
		}
		return null;
	}

	@Override
	public void delete(Object entity) {
		try {
			entityManager.remove(attach(entityManager, entity));
		} catch (Exception e) {
			log.warn(e, e);
			throw convertException(e, entity.getClass(), entity, EntityExceptionActionType.Deleting);
		}
	}

	@Override
	public void flush() throws EntityException {
		try {
			entityManager.flush();
		} catch (Exception e) {
			log.warn(e, e);
			throw convertException(e);
		}
	}

	protected static <T> T attach(EntityManager entityManager, T entity) {
		if (!entityManager.contains(entity) && (entity != null)) {
			T original = entity;
			// TODO: this is a hibernate specific hack for proxy related
			// problems
			if (HibernateProxy.class.isAssignableFrom(entity.getClass())) {
				LazyInitializer lazyInitializer = ((HibernateProxy) entity)
						.getHibernateLazyInitializer();
				entity = entityManager.find((Class<T>) lazyInitializer.getPersistentClass(),
						lazyInitializer.getIdentifier());
				if (entity == null) {
					entity = original;
					log.warn("reloading " + entity.getClass() + " "
							+ lazyInitializer.getIdentifier() + " returned null;");
				}
			} else {
				// the object may not be persisted, if it doesn't have an id,
				// don't load it from the db
				Object id = getId(entity);
				if (id != null) {
					entity = entityManager.find((Class<T>) entity.getClass(), id);
					if (entity == null) {
						entity = original;
						log.warn("reloading " + entity.getClass() + " " + id + " returned null;");
						// TODO: Something fishy is happening. in the
						// hibernate AbstractBatcher getResultSet()
						// ResultSet rs = ps.executeQuery();
						// returns an empty result set, even though running
						// the query in SQL ui returns the expected rows.

						// maybe the entity hasn't been committed yet, return
						// the original it may be that the entity was deleted,
						// what should happen in that case?
					}
				}
			}
		}
		if (EntityProxyInterceptor.isEntityProxy(entity)) {
			log.warn("Entity proxy " + entity + " was expected to be a raw entity.",
					new Throwable());
			entity = EntityProxyInterceptor.unwrap(entity);
		}
		return entity;
	}

	protected static Object getId(Object entity) {
		if (entity != null) {
			Class<?> entityType = entity.getClass();
			do {
				try {
					Method getId = entityType.getDeclaredMethod("getId");
					getId.setAccessible(true);
					return getId.invoke(entity);
				} catch (NoSuchMethodException e) {
					entityType = entityType.getSuperclass();
				} catch (Exception e) {
					throw new RuntimeException("could not get the id of the entity " + entity, e);
				}
			} while (entityType != null);
			throw new RuntimeException("No way to get the id of the entity " + entity);
		}
		return null;
	}
}
