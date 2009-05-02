/*
 * $Id: DomainObjectWrapper.java,v 1.1 2008/12/13 00:41:16 rregan Exp $
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
package edu.harvard.fas.rregan.repository.jpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.Entity;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;

import org.apache.log4j.Logger;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import edu.harvard.fas.rregan.requel.user.UserSet;
import edu.harvard.fas.rregan.requel.user.impl.UserSetImpl;

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

	/**
	 * @param persistenceContextHelper
	 */
	@Autowired
	public DomainObjectWrapper(PersistenceContextHelper persistenceContextHelper) {
		this.persistenceContextHelper = persistenceContextHelper;
	}

	/**
	 * Given an object, if it is a persistent entity or a Collection of
	 * persistent entities, wrap it/them in a proxy.
	 * 
	 * @param object
	 * @return
	 */
	public Object wrapPersistentEntities(Object object) {
		if (object instanceof Collection<?>) {
			Collection<?> collection = (Collection<?>) object;
			if (collection instanceof UserSet) {
				object = wrapUserSetEntries((UserSet) collection);
			} else if (collection instanceof SortedSet<?>) {
				object = wrapCollectionEntries(new TreeSet<Object>(((SortedSet) collection)
						.comparator()), collection);
			} else if (collection instanceof Set<?>) {
				object = wrapCollectionEntries(new HashSet<Object>(collection.size()), collection);
			} else if (collection instanceof List<?>) {
				object = wrapCollectionEntries(new ArrayList<Object>(collection.size()), collection);
			} else {
				throw new RuntimeException("unexpected collection type: " + object);
			}

		} else {
			object = wrapEntity(object);
		}
		return object;
	}

	// TODO: the UserSet may not be needed
	protected UserSet wrapUserSetEntries(UserSet collection) {
		Set<Object> set = new HashSet<Object>(collection.size());
		for (Object entity : collection) {
			set.add(wrapEntity(entity));
		}
		return new UserSetImpl(set);
	}

	protected Object wrapCollectionEntries(Collection<Object> newCollection,
			Collection<?> origCollection) {
		for (Object entity : origCollection) {
			newCollection.add(wrapEntity(entity));
		}
		return newCollection;
	}

	protected Object wrapEntity(Object entity) {
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
			log.debug("wrapping entity: " + entity);
			return getEntityFactory(entity).newInstance(
					new EntityProxyInterceptor(persistenceContextHelper, this, entity));
		}
		return null;
	}

	private Factory getEntityFactory(Object entity) {
		Class<?> entityType = entity.getClass();
		Factory f = factoryMap.get(entityType);
		if (f == null) {
			Enhancer e = new Enhancer();
			e.setSuperclass(entityType);
			e.setCallback(new EntityProxyInterceptor(null, null, null));
			f = (Factory) e.create();
			factoryMap.put(entityType, f);
		}
		return f;
	}
}
