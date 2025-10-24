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

import com.rreganjr.requel.user.UserSet;
import com.rreganjr.requel.user.impl.UserSetImpl;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * A component that takes an object and if it is a persistent entity or
 * collection of persistent entities, it wraps all the entities in a proxy using
 * the EntityProxyInterceptor to dispatch method calls.
 * 
 * @author ron
 */
@Component("domainObjectWrapper")
@Scope("singleton")
public class UserAwareDomainObjectWrapper extends DomainObjectWrapper {
	protected static final Logger log = Logger.getLogger(UserAwareDomainObjectWrapper.class);

	/**
	 * @param persistenceContextHelper
	 */
	@Autowired
	public UserAwareDomainObjectWrapper(PersistenceContextHelper persistenceContextHelper) {
		super(persistenceContextHelper);
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
	@Override
	public Object wrapPersistentEntities(Object object, long timeStamp) {
		if (object instanceof Collection<?>) {
			Collection<?> collection = (Collection<?>) object;
			if (collection instanceof UserSet) {
				object = wrapUserSetEntries((UserSet) collection, timeStamp);
			} else {
				object = super.wrapPersistentEntities(object, timeStamp);
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

}
