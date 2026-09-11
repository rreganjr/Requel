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
package com.rreganjr.requel.annotation.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;

import com.rreganjr.requel.annotation.Position;

/**
 * Unit coverage for {@link PositionTypes}, the proxy-safe position type resolution behind issue
 * #253. Each of the three tiers is exercised in isolation so a future reordering fails loudly, and
 * every case asserts the result is a bare name — a generated proxy class name must never reach the
 * API, whichever tier answered.
 *
 * @author ron
 */
class PositionTypesTest {

	/**
	 * A position type this module cannot import — it lives in project-jpa, which depends on
	 * annotation-jpa and not the other way round. Resolution is by discriminator string, so the
	 * helper handles it without the class being on the classpath, which is the point.
	 */
	private static final String ADD_ACTOR = "com.rreganjr.requel.project.impl.AddActorPosition";

	@Test
	void nullPositionResolvesToNullRatherThanThrowing() {
		assertNull(PositionTypes.typeNameOf(null));
		assertNull(PositionTypes.rawTypeNameOf(null));
	}

	// ---- tier 3: a transient position with no discriminator yet --------------------------------

	@Test
	void plainPositionImplDropsTheImplSuffix() {
		assertBareName("Position", PositionTypes.typeNameOf(new PositionImpl()));
	}

	@Test
	void plainSubclassKeepsItsOwnName() {
		assertBareName("AddWordToDictionaryPosition",
				PositionTypes.typeNameOf(new AddWordToDictionaryPosition()));
		assertBareName("ChangeSpellingPosition",
				PositionTypes.typeNameOf(new ChangeSpellingPosition()));
	}

	// ---- tier 2: the persisted discriminator ---------------------------------------------------

	@Test
	void theDiscriminatorWinsOverTheInstanceClass() {
		PositionImpl position = new PositionImpl();
		position.setType(ADD_ACTOR);

		assertEquals(ADD_ACTOR, PositionTypes.rawTypeNameOf(position),
				"the resolver registries are keyed on the entity name, so it must not be prettified");
		assertBareName("AddActorPosition", PositionTypes.typeNameOf(position));
	}

	// ---- tier 1: a lazy Hibernate proxy --------------------------------------------------------

	@Test
	void aHibernateProxyIsAnsweredFromItsEntityNameWithoutBeingInitialized() {
		LazyInitializer initializer = mock(LazyInitializer.class);
		when(initializer.getEntityName()).thenReturn(ADD_ACTOR);
		Position position = mock(Position.class,
				withSettings().extraInterfaces(HibernateProxy.class));
		when(((HibernateProxy) position).getHibernateLazyInitializer()).thenReturn(initializer);

		assertBareName("AddActorPosition", PositionTypes.typeNameOf(position));
		verify(initializer, never()).getImplementation();
	}

	// ---- the reported defect -------------------------------------------------------------------

	@Test
	void aCglibProxyNeverLeaksItsGeneratedName() {
		AddWordToDictionaryPosition proxy = cglibProxy(AddWordToDictionaryPosition.class);
		assertTrue(proxy.getClass().getSimpleName().contains("$$"),
				"fixture is not actually a proxy, so this test would prove nothing: "
						+ proxy.getClass().getName());

		// tier 3 — no discriminator on a freshly constructed instance
		assertBareName("AddWordToDictionaryPosition", PositionTypes.typeNameOf(proxy));

		// tier 2 — a loaded entity carries its discriminator, read straight through the proxy
		proxy.setType(ADD_ACTOR);
		assertBareName("AddActorPosition", PositionTypes.typeNameOf(proxy));
	}

	@Test
	void aCglibProxyOverThePlainTypeIsStillCalledPosition() {
		PositionImpl proxy = cglibProxy(PositionImpl.class);
		assertTrue(proxy.getClass().getSimpleName().contains("$$"));

		assertBareName("Position", PositionTypes.typeNameOf(proxy));
	}

	// ---- falling between the tiers -------------------------------------------------------------

	/** A proxy that cannot name its entity must not stop resolution there. */
	@Test
	void aHibernateProxyWithoutAnEntityNameFallsThroughToTheDiscriminator() {
		LazyInitializer initializer = mock(LazyInitializer.class);
		when(initializer.getEntityName()).thenReturn(null);
		PositionImpl position = mock(PositionImpl.class,
				withSettings().extraInterfaces(HibernateProxy.class));
		when(((HibernateProxy) position).getHibernateLazyInitializer()).thenReturn(initializer);
		when(position.getType()).thenReturn(ADD_ACTOR);

		assertBareName("AddActorPosition", PositionTypes.typeNameOf(position));
	}

	/** A Position that is neither proxied nor a PositionImpl still has to resolve. */
	@Test
	void aPositionThatIsNotAPositionImplResolvesFromItsClass() {
		Position position = mock(Position.class);

		assertBareName("Position", PositionTypes.typeNameOf(position));
	}

	// ---- the pretty-name edges -----------------------------------------------------------------

	/**
	 * The belt-and-braces guard. No caller should reach {@code prettyName} with a generated name —
	 * the tiers above exist so that it cannot — but if one ever does, the API is not where it
	 * surfaces.
	 */
	@Test
	void aGeneratedNameIsTruncatedRatherThanPublished() {
		PositionImpl position = new PositionImpl();
		position.setType(ADD_ACTOR + "$$EnhancerByCGLIB$$1f1035a5");

		assertBareName("AddActorPosition", PositionTypes.typeNameOf(position));
	}

	/** A type named exactly "Impl" keeps its name rather than being stripped to nothing. */
	@Test
	void aNameThatIsOnlyTheImplSuffixSurvives() {
		PositionImpl position = new PositionImpl();
		position.setType("com.rreganjr.requel.annotation.impl.Impl");

		assertEquals("Impl", PositionTypes.typeNameOf(position));
	}

	/** A discriminator with no package at all. */
	@Test
	void anUnqualifiedDiscriminatorIsUsedAsIs() {
		PositionImpl position = new PositionImpl();
		position.setType("AddActorPosition");

		assertBareName("AddActorPosition", PositionTypes.typeNameOf(position));
	}

	// ---- helpers -------------------------------------------------------------------------------

	/**
	 * The same shape {@code DomainObjectWrapper.getEntityFactory} builds in production: a CGLIB
	 * subclass of the entity type. The callback passes through, so the object behaves like the
	 * entity while carrying a generated class name.
	 */
	@SuppressWarnings("unchecked")
	private static <T> T cglibProxy(Class<T> entityType) {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(entityType);
		enhancer.setCallback((MethodInterceptor) (obj, method, args, methodProxy) -> methodProxy
				.invokeSuper(obj, args));
		return (T) enhancer.create();
	}

	private static void assertBareName(String expected, String actual) {
		assertEquals(expected, actual);
		assertFalse(actual.contains("$"), "a generated class name reached the API: " + actual);
		assertFalse(actual.contains("."), "a package name reached the API: " + actual);
	}
}
