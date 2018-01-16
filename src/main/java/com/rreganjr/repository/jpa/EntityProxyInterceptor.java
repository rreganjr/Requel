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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.apache.log4j.Logger;
import org.hibernate.LazyInitializationException;

/**
 * A MethodInterceptor that gets assigned to a proxy for entity objects by the
 * DomainObjectWrappingAdvice. The proxy is an empty dummy object which acts as
 * a reference to the entity. The "real" entity is stored in this interceptor
 * and refreshed as needed.<br>
 * TODO: as data is loaded from the db it should be cached in the local copy for
 * faster access. Ideally only lazy loaded objects should require accessing the
 * database. Also, how will updates in other transactions get propogated to this
 * object?
 * 
 * @author ron
 */
public class EntityProxyInterceptor implements MethodInterceptor {
	protected static final Logger log = Logger.getLogger(EntityProxyInterceptor.class);
	private long staleTimeOut = 0;
	private long lastRefreshTime = 0;
	private Object entity;
	final private PersistenceContextHelper persistenceContextHelper;
	final private DomainObjectWrapper domainObjectWrapper;

	/**
	 * @param persistenceContextHelper -
	 *            a helper that attaches/gets the latest version of the entity.
	 * @param domainObjectWrapper -
	 *            a helper that wraps entities in a proxy
	 * @param entity -
	 *            the real object being proxied, the proxy is really a dummy
	 *            object.
	 */
	public EntityProxyInterceptor(PersistenceContextHelper persistenceContextHelper,
			DomainObjectWrapper domainObjectWrapper, Object entity, long lastRefreshTime,
			long staleTimeOut) {
		this.lastRefreshTime = lastRefreshTime;
		this.persistenceContextHelper = persistenceContextHelper;
		this.domainObjectWrapper = domainObjectWrapper;
		this.entity = entity;
		this.staleTimeOut = staleTimeOut;
	}

	public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy)
			throws Throwable {
		method.setAccessible(true);
		if ("finalize".equals(method.getName())) {
			return null;
		}
		if (isSetterMethod(method)) {
			log.debug("possibly setting a persistent value on a proxy,"
					+ " which may be bad outside of a command.");
		}
		Object retVal;
		if ((staleTimeOut < 1) || ((System.currentTimeMillis() - lastRefreshTime) > staleTimeOut)) {
			// TODO: what about using the entity version if available instead of
			// reloading the whole object from the database?
			retVal = persistenceContextHelper.invokeInTransaction(this, domainObjectWrapper,
					method, args);
		} else {
			// try to invoke the method on the cached version, if it fails
			// because of lazy loading, then invoke the method
			// transactionally.
			try {
				try {
					retVal = domainObjectWrapper.wrapPersistentEntities(
							method.invoke(entity, args), this.lastRefreshTime);
				} catch (InvocationTargetException e) {
					throw e.getCause();
				}
			} catch (LazyInitializationException e) {
				retVal = persistenceContextHelper.invokeInTransaction(this, domainObjectWrapper,
						method, args);
			}
		}
		return retVal;
	}

	/**
	 * @return the current cached version of the entity
	 */
	public Object getEntity() {
		return entity;
	}

	/**
	 * set the cached version of the entity.
	 * 
	 * @param entity
	 */
	public void setEntity(Object entity) {
		lastRefreshTime = System.currentTimeMillis();
		this.entity = entity;
	}

	/**
	 * If the supplied object is a proxy with "advice" from the EntityProxy,
	 * return the entity attached to the callback, otherwise return the supplied
	 * object.
	 * 
	 * @param possibleProxy
	 * @return
	 */
	public static <T> T unwrap(T possibleProxy) {
		if (possibleProxy instanceof Factory) {
			Callback callback = ((Factory) possibleProxy).getCallback(0);
			if (callback instanceof EntityProxyInterceptor) {
				return (T) ((EntityProxyInterceptor) callback).getEntity();
			}
		}
		return possibleProxy;
	}

	/**
	 * @param possibleProxy
	 * @return true if possibleProxy is an EntityProxy
	 */
	public static boolean isEntityProxy(Object possibleProxy) {
		if (possibleProxy instanceof Factory) {
			Callback callback = ((Factory) possibleProxy).getCallback(0);
			if (callback instanceof EntityProxyInterceptor) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param method
	 * @return
	 */
	public static boolean isSetterMethod(Method method) {
		if (method.getName().startsWith("set")) {
			return true;
		}
		return false;
	}
}