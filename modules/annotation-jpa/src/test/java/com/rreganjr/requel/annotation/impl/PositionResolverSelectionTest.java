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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;

import com.rreganjr.command.Command;
import com.rreganjr.command.CommandFactoryStrategy;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.command.ResolveIssueCommand;
import com.rreganjr.requel.annotation.impl.command.AnnotationCommandFactoryImpl;
import com.rreganjr.requel.annotation.impl.command.ResolveIssueCommandImpl;
import com.rreganjr.requel.annotation.impl.command.ResolveIssueWithAddWordToDictionaryPositionCommandImpl;
import com.rreganjr.requel.annotation.impl.command.ResolveIssueWithChangeSpellingPositionCommandImpl;

/**
 * Which resolver {@code AnnotationCommandFactoryImpl.newResolveIssueCommand} picks, across the
 * shapes a position can arrive in (issue #253).
 * <p>
 * That method had no test before its three-tier type resolution moved into {@link PositionTypes},
 * and picking the wrong resolver is silent — a spelling position would simply resolve as a generic
 * one. This covers the tiers with a stub {@link CommandFactoryStrategy}, so no Spring context is
 * needed; {@code ResolveIssueCommandSelectionIT} covers the wired case and the
 * {@code AddActorPosition} resolver that project-jpa registers at startup.
 * <p>
 * Lives in the {@code annotation.impl} package because {@link PositionImpl}'s constructors and
 * {@code setType} are protected.
 *
 * @author ron
 */
class PositionResolverSelectionTest {

	/** Records what the factory asked for, and hands back something castable. */
	private static final class RecordingStrategy implements CommandFactoryStrategy {
		private Class<? extends Command> requested;

		@Override
		public Command newInstance(Class<? extends Command> commandType) {
			requested = commandType;
			return mock(commandType);
		}
	}

	private final RecordingStrategy strategy = new RecordingStrategy();
	private final AnnotationCommandFactoryImpl factory = new AnnotationCommandFactoryImpl(strategy);

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
	 * The tier only the discriminator can answer: loaded as the base type, but carrying a subclass
	 * discriminator.
	 */
	@Test
	void theDiscriminatorAloneSelectsTheSubclassResolver() {
		PositionImpl position = new PositionImpl();
		position.setType(AddWordToDictionaryPosition.class.getName());

		assertResolver(ResolveIssueWithAddWordToDictionaryPositionCommandImpl.class, position);
	}

	/** Nothing registered for the type, by entity name or anywhere up the hierarchy. */
	@Test
	void anUnregisteredPositionTypeIsReportedRatherThanResolvedWrongly() {
		Position position = mock(Position.class);

		RuntimeException thrown = assertThrows(RuntimeException.class,
				() -> factory.newResolveIssueCommand(position));
		assertTrue(thrown.getMessage().contains("no resolver command"), thrown.getMessage());
	}

	private void assertResolver(Class<?> expected, Position position) {
		ResolveIssueCommand command = factory.newResolveIssueCommand(position);
		assertEquals(expected, strategy.requested);
		assertTrue(command != null, "the factory returned nothing for " + expected.getSimpleName());
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
