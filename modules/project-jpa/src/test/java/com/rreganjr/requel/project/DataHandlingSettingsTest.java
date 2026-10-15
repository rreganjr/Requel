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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.project.DataHandlingSettings.RedactionCategory;

/** Issue #262: the settings read from #268's store, where no row means on. */
class DataHandlingSettingsTest {

	@Test
	void noRowsMeansEverythingOn() {
		DataHandlingSettings settings = DataHandlingSettings.fromDisabledKeys(Set.of());

		assertThat(settings.externalProviderAllowed()).isTrue();
		assertThat(settings.redactionCategories()).containsExactlyInAnyOrder(
				RedactionCategory.values());
	}

	@Test
	void switchedOffKeysAreOffAndAssistantIdsAreIgnored() {
		DataHandlingSettings settings = DataHandlingSettings.fromDisabledKeys(Set.of(
				"egress.external", "redaction.phone", "legacy-lexical-spelling"));

		assertThat(settings.externalProviderAllowed()).isFalse();
		assertThat(settings.redacts(RedactionCategory.PHONE)).isFalse();
		assertThat(settings.redacts(RedactionCategory.EMAIL)).isTrue();
		assertThat(settings.redactionSwitches()).containsEntry("phone", false)
				.containsEntry("email", true).containsKeys("credentials", "ssn", "card");
	}

	@Test
	void forProjectReadsTheStoreAndDefaultsWithoutOne() {
		ProjectAssistantSettingsStore store = mock(ProjectAssistantSettingsStore.class);
		when(store.disabledAssistants(5L)).thenReturn(Set.of("redaction.card"));

		assertThat(DataHandlingSettings.forProject(5L, store).redacts(RedactionCategory.CARD))
				.isFalse();
		assertThat(DataHandlingSettings.forProject(5L, null)).isSameAs(
				DataHandlingSettings.defaults());
		assertThat(DataHandlingSettings.forProject(null, store)).isSameAs(
				DataHandlingSettings.defaults());
	}

	@Test
	void theKeyVocabularyIsEgressPlusOneKeyPerCategory() {
		assertThat(DataHandlingSettings.KEYS).containsExactly("egress.external",
				"redaction.credentials", "redaction.email", "redaction.phone", "redaction.ssn",
				"redaction.card");
		assertThat(DataHandlingSettings.isKey("redaction.email")).isTrue();
		assertThat(DataHandlingSettings.isKey("redaction.passport")).isFalse();
		assertThat(RedactionCategory.fromId("SSN")).contains(RedactionCategory.SSN);
	}
}
