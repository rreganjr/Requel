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
import com.rreganjr.requel.project.impl.AddActorPosition;
import com.rreganjr.requel.project.impl.command.ResolveIssueWithAddActorPositionCommandImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Resolver selection, pinned across the extraction of the three-tier type resolution into
 * {@link PositionTypes} (issue #253).
 * <p>
 * {@code newResolveIssueCommand} had no test before that refactor, and picking the wrong resolver
 * is silent — a spelling position would simply resolve as a generic one. The tier-by-tier cases
 * live in annotation-jpa's {@code PositionResolverSelectionTest}, which needs no Spring context;
 * what is left here is what only the wired application can show.
 * <p>
 * Lives in the {@code annotation.impl} package because the {@code PositionImpl} constructors and
 * {@code setType} are protected.
 *
 * @author ron
 */
public class ResolveIssueCommandSelectionIT extends AbstractIntegrationTestCase {

	/**
	 * The case only the wired application can prove: {@code AddActorPosition} and its resolver are
	 * registered by project-jpa's {@code PositionCommandRegistration} at startup, so a unit test
	 * against the static registry would not see them. The position is loaded as the base type and
	 * identified by its discriminator alone.
	 */
	@Test
	void aResolverRegisteredByAnotherModuleIsSelectedFromTheDiscriminator() {
		PositionImpl position = new PositionImpl();
		position.setType(AddActorPosition.class.getName());

		assertResolver(ResolveIssueWithAddActorPositionCommandImpl.class, position);
	}

	/** A smoke check that the wired factory still resolves the ordinary case. */
	@Test
	void aPlainPositionSelectsTheGenericResolver() {
		assertResolver(ResolveIssueCommandImpl.class, new PositionImpl());
	}

	private void assertResolver(Class<?> expected, com.rreganjr.requel.annotation.Position position) {
		ResolveIssueCommand command = getAnnotationCommandFactory()
				.newResolveIssueCommand(position);
		// The factory hands back a Spring-managed instance, which may itself be proxied.
		assertEquals(expected, ProxyTypes.userClassOf(command));
	}
}
