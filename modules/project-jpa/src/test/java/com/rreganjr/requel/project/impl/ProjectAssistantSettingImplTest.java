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
package com.rreganjr.requel.project.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProjectAssistantSettingImplTest {

	@Test
	void aNewSettingIsEnabledUntilUpdated() {
		ProjectAssistantSettingImpl setting = new ProjectAssistantSettingImpl(7L, "legacy-lexical");

		assertThat(setting.getKey().getProjectId()).isEqualTo(7L);
		assertThat(setting.getKey().getAssistantId()).isEqualTo("legacy-lexical");
		assertThat(setting.isEnabled()).isTrue();
		assertThat(setting.getUpdatedById()).isNull();
		assertThat(setting.getDateUpdated()).isNull();

		setting.update(false, 3L);

		assertThat(setting.isEnabled()).isFalse();
		assertThat(setting.getUpdatedById()).isEqualTo(3L);
		assertThat(setting.getDateUpdated()).isNotNull();
	}

	@Test
	void keysAreEqualByProjectAndAssistant() {
		ProjectAssistantSettingImpl.Key key = new ProjectAssistantSettingImpl.Key(7L, "a");

		assertThat(key).isEqualTo(new ProjectAssistantSettingImpl.Key(7L, "a"))
				.hasSameHashCodeAs(new ProjectAssistantSettingImpl.Key(7L, "a"))
				.isNotEqualTo(new ProjectAssistantSettingImpl.Key(8L, "a"))
				.isNotEqualTo(new ProjectAssistantSettingImpl.Key(7L, "b"))
				.isNotEqualTo("7:a");
		assertThat(new ProjectAssistantSettingImpl.Key())
				.isEqualTo(new ProjectAssistantSettingImpl.Key(null, null));
		assertThat(new ProjectAssistantSettingImpl().getKey()).isNull();
	}
}
