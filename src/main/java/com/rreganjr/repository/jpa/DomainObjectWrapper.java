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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import jakarta.annotation.Resource;
import javax.persistence.Entity;

import org.apache.log4j.Logger;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;

import com.rreganjr.requel.user.UserSet;
import com.rreganjr.requel.user.impl.UserSetImpl;

/**
 * A component that takes an object and if it is a persistent entity or
 * collection of persistent entities, it wraps all the entities in a proxy using
 * the EntityProxyInterceptor to dispatch method calls.
 * 
 * @author ron
 */
@Component("domainObjectWrapper")
@Scope("singleton")
public class DomainObjectWrapper {
	protected static final Logger log = Logger.getLogger(DomainObjectWrapper.class);

	private final Map<Class<?>, Factory> factoryMap = new HashMap<Class<?>, Factory>();
	private final PersistenceContextHelper persistenceContextHelper;

	private Map<Class<?>, Integer> staleTimeoutMap;

	/**
	 * @param persistenceContextHelper
	 */
	@Autowired
	public DomainObjectWrapper(PersistenceContextHelper persistenceContextHelper) {
		this.persistenceContextHelper = persistenceContextHelper;
	}

	public Map<Class<?>, Integer> getStaleTimeoutMap() {
		return staleTimeoutMap;
	}

	// This is done as a resource instead of autowired through
	// the constructor because map key-type isn't supported with
	// autowire see http://jira.springframework.org/browse/SPR-4492
	@Resource(name = "staleTimeoutMap")
	public void setStaleTimeoutMap(Map<Class<?>, Integer> staleTimeoutMap) {
		this.staleTimeoutMap = staleTimeoutMap;
	}

	/**
	 * Given an object, if it is a persistent entity or a Collection of
	 * persistent entities, wrap it/them in a proxy.
	 * 
	 * @param object -
	 *            any thing that may need to be wrapped with an
	 *            EntityProxyInterceptor.
	 * @param timeStamp -
	 *            The time to use as the starting timestamp for freshness
	 *            checking.
	 * @return
	 */
	public Object wrapPersistentEntities(Object object, long timeStamp) {
		if (object instanceof Collection<?>) {
			Collection<?> collection = (Collection<?>) object;
			if (collection instanceof UserSet) {
				object = wrapUserSetEntries((UserSet) collection, timeStamp);
			} else if (collection instanceof SortedSet<?>) {
				object = wrapCollectionEntries(new TreeSet<Object>(((SortedSet) collection)
						.comparator()), collection, timeStamp);
			} else if (collection instanceof Set<?>) {
				object = wrapCollectionEntries(new HashSet<Object>(collection.size()), collection,
						timeStamp);
			} else if (collection instanceof List<?>) {
				object = wrapCollectionEntries(new ArrayList<Object>(collection.size()),
						collection, timeStamp);
			} else {
				throw new RuntimeException("unexpected collection type: " + object);
			}

		} else {
			object = wrapEntity(object, timeStamp);
		}
		return object;
	}

	// TODO: the UserSet may not be needed
	protected UserSet wrapUserSetEntries(UserSet collection, long timeStamp) {
		Set<Object> set = new HashSet<Object>(collection.size());
		for (Object entity : collection) {
			set.add(wrapEntity(entity, timeStamp));
		}
		return new UserSetImpl(set);
	}

	protected Object wrapCollectionEntries(Collection<Object> newCollection,
			Collection<?> origCollection, long timeStamp) {
		for (Object entity : origCollection) {
			newCollection.add(wrapEntity(entity, timeStamp));
		}
		return newCollection;
	}

	protected Object wrapEntity(Object entity, long timeStamp) {
		if (entity != null) {
			// if an entity is already wrapped, don't add another wrapper
			if (EntityProxyInterceptor.isEntityProxy(entity)) {
				return entity;
			}
			// maybe a hibernate proxy
			if (entity instanceof HibernateProxy) {
				entity = ((HibernateProxy) entity).getHibernateLazyInitializer()
						.getImplementation();
			}
			// only wrap entity objects
			if (entity.getClass().getAnnotation(Entity.class) == null) {
				return entity;
			}

			final long timeOut = getTypeSpecificStaleTimeout(entity.getClass());
			log.debug("wrapping entity: " + entity + " timeOut = " + timeOut + " timeStamp = "
					+ timeStamp);
			return getEntityFactory(entity).newInstance(
					new EntityProxyInterceptor(persistenceContextHelper, this, entity, timeStamp,
							timeOut));
		}
		return null;
	}

	private Factory getEntityFactory(Object entity) {
		Class<?> entityType = entity.getClass();
		Factory f = factoryMap.get(entityType);
		if (f == null) {
			Enhancer e = new Enhancer();
			e.setSuperclass(entityType);
			e.setCallback(new EntityProxyInterceptor(null, null, null, 0, 0));
			f = (Factory) e.create();
			factoryMap.put(entityType, f);
		}
		return f;
	}

	/**
	 * Entity types may be configured with a timeout period to determine if an
	 * entity needs to be reloaded from the db. This allows objects that are
	 * mostly read-only to have a long period so that the object isn't read from
	 * the database frequently. Entities that are likely to change frequently
	 * should have a second or less. a value of zero means the entity is always
	 * read from the database on each method access.
	 * 
	 * @param targetType
	 * @return
	 */
	protected long getTypeSpecificStaleTimeout(Class<?> targetType) {
		while (!Object.class.equals(targetType)) {
			if (staleTimeoutMap.containsKey(targetType)) {
				return staleTimeoutMap.get(targetType).longValue();
			}
			if (targetType.getInterfaces() != null) {
				for (Class<?> face : targetType.getInterfaces()) {
					if (staleTimeoutMap.containsKey(face)) {
						return staleTimeoutMap.get(face).longValue();
					}
				}
			}
			targetType = targetType.getSuperclass();
		}
		if (staleTimeoutMap.containsKey(Object.class)) {
			return staleTimeoutMap.get(Object.class).longValue();
		}
		return 1000;
	}
}
