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
package com.rreganjr.requel.project.impl.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.project.impl.ProjectAssistantSettingImpl;

import jakarta.persistence.EntityManager;

/** Issue #268: the store's own logic; ProjectAssistantSettingsIT runs its queries. */
class JpaProjectAssistantSettingsStoreTest {

	private final EntityManager entityManager = mock(EntityManager.class);
	private final JpaProjectAssistantSettingsStore store = new JpaProjectAssistantSettingsStore();

	JpaProjectAssistantSettingsStoreTest() throws Exception {
		Field field = JpaProjectAssistantSettingsStore.class.getDeclaredField("entityManager");
		field.setAccessible(true);
		field.set(store, entityManager);
	}

	@Test
	void aProjectWithoutAnIdHasNoSettings() {
		assertThat(store.disabledAssistants(null)).isEmpty();
		assertThat(store.deleteForProject(null)).isZero();
		verifyNoInteractions(entityManager);
	}

	@Test
	void aNewSettingIsPersistedWithoutAUser() {
		store.setEnabled(7L, "legacy-lexical", false, null);

		ArgumentCaptor<ProjectAssistantSettingImpl> persisted = ArgumentCaptor
				.forClass(ProjectAssistantSettingImpl.class);
		verify(entityManager).persist(persisted.capture());
		assertThat(persisted.getValue().isEnabled()).isFalse();
		assertThat(persisted.getValue().getUpdatedById()).isNull();
	}

	@Test
	void anExistingSettingIsUpdatedInPlace() {
		ProjectAssistantSettingImpl existing = new ProjectAssistantSettingImpl(7L, "legacy-lexical");
		when(entityManager.find(ProjectAssistantSettingImpl.class,
				new ProjectAssistantSettingImpl.Key(7L, "legacy-lexical"))).thenReturn(existing);
		User ron = mock(User.class);
		when(ron.getId()).thenReturn(3L);

		store.setEnabled(7L, "legacy-lexical", false, ron);
		assertThat(existing.isEnabled()).isFalse();
		assertThat(existing.getUpdatedById()).isEqualTo(3L);

		store.setEnabled(7L, "legacy-lexical", true, null);
		assertThat(existing.isEnabled()).isTrue();
		assertThat(existing.getUpdatedById()).isNull();
		verify(entityManager, never()).persist(any());
	}
}
