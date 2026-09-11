/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.springframework.util.ClassUtils;

/**
 * Entity-agnostic helpers for seeing past the two proxy families an entity can be wearing by the
 * time it reaches a caller.
 * <p>
 * Entities handed out by {@link AbstractJpaRepository} or by a {@code Command} getter are wrapped
 * in a CGLIB proxy by {@link DomainObjectWrappingAdvice}, and a lazy association is a Hibernate
 * proxy. Either one answers {@code getClass().getSimpleName()} with a generated name carrying a
 * build-specific hash, so any code deriving a durable string from an entity's class must come
 * through here first (issue #253).
 * <p>
 * Only the type is exposed, deliberately: an unwrap-the-object helper was written alongside this
 * one and had no callers, and an uninitialized Hibernate proxy should not be loaded just to ask
 * what it is.
 *
 * @author ron
 */
public final class ProxyTypes {

	private ProxyTypes() {
		// static helpers only
	}

	/**
	 * The user class behind whatever proxying the argument is wearing.
	 * <p>
	 * An uninitialized Hibernate proxy is answered from its persistent class rather than by loading
	 * it, so this is safe to call on a lazy association.
	 *
	 * @param candidate
	 *            possibly an EntityProxy, a Hibernate proxy, a plain entity, or null
	 * @return the user class, or null when the argument is null
	 */
	public static Class<?> userClassOf(Object candidate) {
		if (candidate == null) {
			return null;
		}
		Object entity = EntityProxyInterceptor.unwrap(candidate);
		if (entity == null) {
			entity = candidate;
		}
		if (entity instanceof HibernateProxy) {
			LazyInitializer initializer = ((HibernateProxy) entity)
					.getHibernateLazyInitializer();
			Class<?> persistentClass = initializer.getPersistentClass();
			if (persistentClass != null) {
				return persistentClass;
			}
			Object implementation = initializer.getImplementation();
			if (implementation != null) {
				entity = implementation;
			}
		}
		return ClassUtils.getUserClass(entity);
	}
}
