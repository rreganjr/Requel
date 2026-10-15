/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;

/** Issue #75: what each stakeholder permission brings with it. */
class StakeholderPermissionRulesTest {

	private static String key(Class<?> type, StakeholderPermissionType permission) {
		return StakeholderPermissionRules.key(type, permission);
	}

	@Test
	void keysMatchThePersistedPermissionKeys() {
		assertThat(key(UseCase.class, StakeholderPermissionType.Edit)).isEqualTo(
				StakeholderPermissionImpl.generatePermissionKey(UseCase.class,
						StakeholderPermissionType.Edit));
		assertThat(StakeholderPermissionRules.label(key(Goal.class, StakeholderPermissionType.Delete)))
				.isEqualTo("Goal[Delete]");
	}

	@Test
	void useCaseEditImpliesScenarioAndActorEditAndNothingElseImpliesAnything() {
		assertThat(StakeholderPermissionRules.closure(
				List.of(key(UseCase.class, StakeholderPermissionType.Edit))))
				.containsExactlyInAnyOrder(key(UseCase.class, StakeholderPermissionType.Edit),
						key(Scenario.class, StakeholderPermissionType.Edit),
						key(Actor.class, StakeholderPermissionType.Edit));
		Set<String> deletes = Set.of(key(Goal.class, StakeholderPermissionType.Delete),
				key(UseCase.class, StakeholderPermissionType.Delete),
				key(Project.class, StakeholderPermissionType.Delete));
		assertThat(StakeholderPermissionRules.closure(deletes)).isEqualTo(deletes);
		assertThat(StakeholderPermissionRules.closure(List.of())).isEmpty();
	}

	@Test
	void grantingNeedsTheTypesGrantOrProjectGrantWhenTheTypeHasNone() {
		List<String> catalog = List.of(key(Goal.class, StakeholderPermissionType.Edit),
				key(Goal.class, StakeholderPermissionType.Grant),
				key(Project.class, StakeholderPermissionType.Grant),
				key(AssistantDefinition.class, StakeholderPermissionType.Edit));

		assertThat(StakeholderPermissionRules.grantKeyFor(
				key(Goal.class, StakeholderPermissionType.Edit), catalog))
				.isEqualTo(key(Goal.class, StakeholderPermissionType.Grant));
		assertThat(StakeholderPermissionRules.grantKeyFor(
				key(AssistantDefinition.class, StakeholderPermissionType.Edit), catalog))
				.isEqualTo(key(Project.class, StakeholderPermissionType.Grant));
	}

	@Test
	void ownedDeletesFlagScenarioAndAnnotationDeletesWithANote() {
		String scenarioDelete = key(Scenario.class, StakeholderPermissionType.Delete);
		String annotationDelete = key(Annotation.class, StakeholderPermissionType.Delete);

		assertThat(StakeholderPermissionRules.OWNED_DELETES)
				.anySatisfy(rule -> {
					assertThat(rule.granted()).isEqualTo(key(UseCase.class, StakeholderPermissionType.Delete));
					assertThat(rule.flagged()).isEqualTo(scenarioDelete);
				})
				.anySatisfy(rule -> {
					assertThat(rule.granted()).isEqualTo(key(Scenario.class, StakeholderPermissionType.Edit));
					assertThat(rule.flagged()).isEqualTo(scenarioDelete);
				})
				.anySatisfy(rule -> {
					assertThat(rule.granted()).isEqualTo(key(Goal.class, StakeholderPermissionType.Delete));
					assertThat(rule.flagged()).isEqualTo(annotationDelete);
					assertThat(rule.note()).isEqualTo(
							"Deleting a goal deletes the notes and issues that are only on it.");
				})
				.anySatisfy(rule -> assertThat(rule.note()).startsWith("Deleting an actor"))
				.allSatisfy(rule -> assertThat(rule.note()).isNotBlank());
		assertThat(StakeholderPermissionRules.OWNED_DELETES).filteredOn(
				rule -> rule.granted().equals(key(Project.class, StakeholderPermissionType.Delete)))
				.extracting(StakeholderPermissionRules.OwnedDelete::flagged)
				.contains(scenarioDelete, annotationDelete,
						key(Goal.class, StakeholderPermissionType.Delete));
		// an implied permission is never also only "owned"
		assertThat(StakeholderPermissionRules.OWNED_DELETES).noneSatisfy(rule -> assertThat(
				StakeholderPermissionRules.closure(List.of(rule.granted()))).contains(rule.flagged()));
	}
}
