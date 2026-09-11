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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;

/**
 * Unit coverage for {@link ProxyTypes#userClassOf(Object)} — the entity-agnostic half of the
 * proxy-safe type resolution behind issue #253.
 * <p>
 * Each proxy family an entity can be wearing by the time it reaches a caller is exercised
 * separately: the CGLIB wrapper {@link DomainObjectWrappingAdvice} adds, a Hibernate proxy, and the
 * plain case.
 *
 * @author ron
 */
class ProxyTypesTest {

	/** Stands in for an entity; any class with a no-arg constructor will do. */
	public static class Entity {
	}

	@Test
	void nullResolvesToNullRatherThanThrowing() {
		assertNull(ProxyTypes.userClassOf(null));
	}

	@Test
	void aPlainObjectIsItsOwnUserClass() {
		assertEquals(Entity.class, ProxyTypes.userClassOf(new Entity()));
	}

	@Test
	void aCglibSubclassResolvesToTheClassItExtends() {
		Entity proxy = cglibProxy(Entity.class);
		assertTrue(proxy.getClass().getName().contains("$$"),
				"fixture is not actually a proxy: " + proxy.getClass().getName());

		assertEquals(Entity.class, ProxyTypes.userClassOf(proxy));
	}

	/**
	 * The shape {@link DomainObjectWrapper} produces: a CGLIB subclass whose callback is an
	 * {@link EntityProxyInterceptor} holding the real entity.
	 */
	@Test
	void anEntityProxyResolvesThroughItsInterceptorToTheWrappedEntity() {
		Entity entity = new Entity();
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(Object.class);
		enhancer.setCallback(new EntityProxyInterceptor(null, null, entity, 0, 0));
		Object proxy = enhancer.create();

		assertEquals(Entity.class, ProxyTypes.userClassOf(proxy),
				"the wrapped entity's class should win over the proxy's own");
	}

	/** An interceptor with no entity behind it must fall back rather than resolve to null. */
	@Test
	void anEntityProxyHoldingNothingFallsBackToTheProxyItself() {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(Entity.class);
		enhancer.setCallback(new EntityProxyInterceptor(null, null, null, 0, 0));
		Object proxy = enhancer.create();

		assertEquals(Entity.class, ProxyTypes.userClassOf(proxy));
	}

	@Test
	void aHibernateProxyIsAnsweredFromItsPersistentClassWithoutBeingInitialized() {
		LazyInitializer initializer = mock(LazyInitializer.class);
		when(initializer.getPersistentClass()).thenAnswer(invocation -> Entity.class);
		HibernateProxy proxy = mock(HibernateProxy.class);
		when(proxy.getHibernateLazyInitializer()).thenReturn(initializer);

		assertEquals(Entity.class, ProxyTypes.userClassOf(proxy));
		verify(initializer, never()).getImplementation();
	}

	/** No persistent class to read: fall through to the implementation rather than give up. */
	@Test
	void aHibernateProxyWithoutAPersistentClassFallsBackToItsImplementation() {
		Entity entity = new Entity();
		LazyInitializer initializer = mock(LazyInitializer.class);
		when(initializer.getPersistentClass()).thenReturn(null);
		when(initializer.getImplementation()).thenReturn(entity);
		HibernateProxy proxy = mock(HibernateProxy.class);
		when(proxy.getHibernateLazyInitializer()).thenReturn(initializer);

		assertEquals(Entity.class, ProxyTypes.userClassOf(proxy));
	}

	/** Neither a persistent class nor an implementation: answer something, never null. */
	@Test
	void aHibernateProxyWithNothingBehindItStillResolvesToAClass() {
		LazyInitializer initializer = mock(LazyInitializer.class);
		when(initializer.getPersistentClass()).thenReturn(null);
		when(initializer.getImplementation()).thenReturn(null);
		HibernateProxy proxy = mock(HibernateProxy.class);
		when(proxy.getHibernateLazyInitializer()).thenReturn(initializer);

		Class<?> resolved = ProxyTypes.userClassOf(proxy);
		assertFalse(resolved == null, "userClassOf must never answer null for a non-null argument");
	}

	@SuppressWarnings("unchecked")
	private static <T> T cglibProxy(Class<T> type) {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(type);
		enhancer.setCallback((MethodInterceptor) (obj, method, args, methodProxy) -> methodProxy
				.invokeSuper(obj, args));
		return (T) enhancer.create();
	}
}
