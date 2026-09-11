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

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.repository.jpa.ProxyTypes;
import com.rreganjr.requel.annotation.command.ResolveIssueCommand;
import com.rreganjr.requel.annotation.impl.command.ResolveIssueCommandImpl;
import com.rreganjr.requel.annotation.impl.command.ResolveIssueWithAddWordToDictionaryPositionCommandImpl;
import com.rreganjr.requel.annotation.impl.command.ResolveIssueWithChangeSpellingPositionCommandImpl;
import com.rreganjr.requel.project.impl.AddActorPosition;
import com.rreganjr.requel.project.impl.command.ResolveIssueWithAddActorPositionCommandImpl;
import org.junit.jupiter.api.Test;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Resolver selection, pinned across the extraction of the three-tier type resolution into
 * {@link PositionTypes} (issue #253).
 * <p>
 * {@code newResolveIssueCommand} had no test before that refactor, and picking the wrong resolver
 * is silent — a spelling position would simply resolve as a generic one. These cases cover the
 * tiers by the shape of the position handed in, including a position whose subtype is only
 * knowable from its discriminator.
 * <p>
 * Lives in the {@code annotation.impl} package because the {@code PositionImpl} constructors and
 * {@code setType} are protected.
 *
 * @author ron
 */
public class ResolveIssueCommandSelectionIT extends AbstractIntegrationTestCase {

	@Test
	void aPlainPositionSelectsTheGenericResolver() {
		assertResolver(ResolveIssueCommandImpl.class, new PositionImpl());
	}

	@Test
	void aSubclassPositionSelectsItsOwnResolver() {
		assertResolver(ResolveIssueWithAddWordToDictionaryPositionCommandImpl.class,
				new AddWordToDictionaryPosition());
		assertResolver(ResolveIssueWithChangeSpellingPositionCommandImpl.class,
				new ChangeSpellingPosition());
	}

	@Test
	void aCglibWrappedSubclassStillSelectsItsOwnResolver() {
		AddWordToDictionaryPosition proxy = cglibProxy(AddWordToDictionaryPosition.class);
		assertTrue(proxy.getClass().getSimpleName().contains("$$"),
				"fixture is not actually a proxy: " + proxy.getClass().getName());

		assertResolver(ResolveIssueWithAddWordToDictionaryPositionCommandImpl.class, proxy);
	}

	/**
	 * The tier that only the discriminator can answer: a position loaded as the base type but
	 * carrying a subclass discriminator, with the subclass registered from another module.
	 */
	@Test
	void theDiscriminatorAloneSelectsTheSubclassResolver() {
		PositionImpl position = new PositionImpl();
		position.setType(AddActorPosition.class.getName());

		assertResolver(ResolveIssueWithAddActorPositionCommandImpl.class, position);
	}

	private void assertResolver(Class<?> expected, com.rreganjr.requel.annotation.Position position) {
		ResolveIssueCommand command = getAnnotationCommandFactory()
				.newResolveIssueCommand(position);
		// The factory hands back a Spring-managed instance, which may itself be proxied.
		assertEquals(expected, ProxyTypes.userClassOf(command));
	}

	@SuppressWarnings("unchecked")
	private static <T> T cglibProxy(Class<T> entityType) {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(entityType);
		enhancer.setCallback((MethodInterceptor) (obj, method, args, methodProxy) -> methodProxy
				.invokeSuper(obj, args));
		return (T) enhancer.create();
	}
}
