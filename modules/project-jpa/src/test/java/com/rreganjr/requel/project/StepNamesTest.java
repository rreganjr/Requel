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
package com.rreganjr.requel.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * #468: how a step's name reads with its actor role filled in.
 */
class StepNamesTest {

	private final Actor operator = actor("Operator");
	private final Actor engineer = actor("Conduit Engineer");

	@Test
	void aStepWithNoRoleReadsAsItsName() {
		Step step = step("Operator ends the room", null, null);
		assertThat(StepNames.render(step, engineer)).isEqualTo("Operator ends the room");
		assertThat(StepNames.renderFor(step, List.of())).isEqualTo("Operator ends the room");
	}

	@Test
	void thePrimaryActorRoleReadsWithEachUseCasesOwnPrimaryActor() {
		Step step = step("ends the room", ActorRole.PRIMARY_ACTOR, null);
		assertThat(StepNames.render(step, operator)).isEqualTo("Operator ends the room");
		assertThat(StepNames.render(step, engineer)).isEqualTo("Conduit Engineer ends the room");
	}

	@Test
	void awayFromOneUseCaseThePrimaryActorIsFilledInOnlyWhenTheyAllAgree() {
		Step step = step("ends the room", ActorRole.PRIMARY_ACTOR, null);
		assertThat(StepNames.renderFor(step, List.of(useCase(operator), useCase(operator))))
				.isEqualTo("Operator ends the room");
		assertThat(StepNames.renderFor(step, List.of(useCase(operator), useCase(engineer))))
				.isEqualTo("Primary actor ends the room");
		assertThat(StepNames.renderFor(step, List.of())).isEqualTo("Primary actor ends the room");
		assertThat(StepNames.renderFor(step, List.of(useCase(null))))
				.isEqualTo("Primary actor ends the room");
	}

	@Test
	void theSystemAndNamedRolesReadTheSameEverywhere() {
		assertThat(StepNames.render(step("logs the end", ActorRole.SYSTEM, null), operator))
				.isEqualTo("The system logs the end");
		assertThat(StepNames.render(step("approves it", ActorRole.NAMED, engineer), operator))
				.isEqualTo("Conduit Engineer approves it");
		assertThat(StepNames.render(step("approves it", ActorRole.NAMED, null), operator))
				.isEqualTo("An actor approves it");
	}

	private static Actor actor(String name) {
		Actor actor = mock(Actor.class);
		when(actor.getName()).thenReturn(name);
		return actor;
	}

	private static UseCase useCase(Actor primary) {
		UseCase useCase = mock(UseCase.class);
		when(useCase.getPrimaryActor()).thenReturn(primary);
		return useCase;
	}

	private static Step step(String name, ActorRole role, Actor actor) {
		Step step = mock(Step.class);
		when(step.getName()).thenReturn(name);
		when(step.getActorRole()).thenReturn(role);
		when(step.getActor()).thenReturn(actor);
		return step;
	}
}
