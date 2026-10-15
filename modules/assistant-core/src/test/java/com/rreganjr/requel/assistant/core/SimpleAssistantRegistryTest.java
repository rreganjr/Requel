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
package com.rreganjr.requel.assistant.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.project.ProjectAssistantSettingsStore;
import com.rreganjr.requel.project.SwitchableAssistantCatalog;

class SimpleAssistantRegistryTest {

	@Test
	void findsAssistantsByTargetType() {
		SimpleAssistantRegistry registry = new SimpleAssistantRegistry(
				List.of(new NumberAssistant(), new StringAssistant()));

		assertThat(registry.findAssistantsFor("goal", context())).extracting(
				RequelAssistant::assistantId).containsExactly("string-assistant");
	}

	/** #268: a switchable assistant switched off in project 1 doesn't run there. */
	@Test
	void aSwitchedOffAssistantIsLeftOutOfThatProjectOnly() {
		SimpleAssistantRegistry registry = new SimpleAssistantRegistry(
				List.of(new StringAssistant(), new SwitchableStringAssistant()));
		registry.setSettingsStore(storeDisabling(1L, "switchable-assistant"));

		assertThat(registry.findAssistantsFor("goal", context(1L)))
				.extracting(RequelAssistant::assistantId).containsExactly("string-assistant");
		assertThat(registry.findAssistantsFor("goal", context(2L)))
				.extracting(RequelAssistant::assistantId)
				.containsExactly("string-assistant", "switchable-assistant");
	}

	/** #268: a setting for an assistant that isn't switchable is ignored. */
	@Test
	void aNonSwitchableAssistantRunsWhateverTheSetting() {
		SimpleAssistantRegistry registry = new SimpleAssistantRegistry(
				List.of(new StringAssistant()));
		registry.setSettingsStore(storeDisabling(1L, "string-assistant"));

		assertThat(registry.findAssistantsFor("goal", context(1L)))
				.extracting(RequelAssistant::assistantId).containsExactly("string-assistant");
	}

	/** #268: settings are per project; without a project context everything runs. */
	@Test
	void withoutAProjectContextNoSettingApplies() {
		SimpleAssistantRegistry registry = new SimpleAssistantRegistry(
				List.of(new SwitchableStringAssistant()));
		registry.setSettingsStore(storeDisabling(1L, "switchable-assistant"));

		assertThat(registry.findAssistantsFor("goal", null)).hasSize(1);
		assertThat(registry.findAssistantsFor("goal", new AssistantContext(UUID.randomUUID(),
				new UserRef(2L, "human"), new UserRef(3L, "assistant"), null, java.util.Locale.US,
				Clock.systemUTC(), Map.of()))).hasSize(1);
		assertThat(registry.findAssistantsFor("goal", new AssistantContext(UUID.randomUUID(),
				new UserRef(2L, "human"), new UserRef(3L, "assistant"), EntityRef.of("Domain", 1L),
				java.util.Locale.US, Clock.systemUTC(), Map.of()))).hasSize(1);
	}

	@Test
	void theCatalogListsOnlySwitchableAssistants() {
		SimpleAssistantRegistry registry = new SimpleAssistantRegistry(
				List.of(new StringAssistant(), new SwitchableStringAssistant()));

		assertThat(registry.switchableAssistants()).containsExactly(
				new SwitchableAssistantCatalog.SwitchableAssistant("switchable-assistant",
						"Switchable"));
		assertThat(registry.find("string-assistant")).isEmpty();
	}

	/** #266: by default a catalog describes only what it can switch. */
	@Test
	void aCatalogDescribesItsSwitchableAssistantsByDefault() {
		SwitchableAssistantCatalog catalog = () -> List.of(
				new SwitchableAssistantCatalog.SwitchableAssistant("a", "A"));

		assertThat(catalog.describe("a")).contains(
				new SwitchableAssistantCatalog.SwitchableAssistant("a", "A",
						SwitchableAssistantCatalog.LEXICAL_CHECKS));
		assertThat(catalog.describe("b")).isEmpty();
	}

	private static ProjectAssistantSettingsStore storeDisabling(Long projectId,
			String assistantId) {
		return new ProjectAssistantSettingsStore() {
			@Override
			public Set<String> disabledAssistants(Long id) {
				return projectId.equals(id) ? Set.of(assistantId) : Set.of();
			}

			@Override
			public void setEnabled(Long id, String assistant, boolean enabled,
					com.rreganjr.platform.identity.User by) {
			}

			@Override
			public int deleteForProject(Long id) {
				return 0;
			}
		};
	}

	private AssistantContext context() {
		return context(1L);
	}

	private AssistantContext context(Long projectId) {
		return new AssistantContext(UUID.randomUUID(), new UserRef(2L, "human"),
				new UserRef(3L, "assistant"), EntityRef.of("Project", projectId),
				java.util.Locale.US, Clock.systemUTC(), Map.of());
	}

	private static final class SwitchableStringAssistant implements RequelAssistant<String> {
		@Override
		public String assistantId() {
			return "switchable-assistant";
		}

		@Override
		public Class<String> targetType() {
			return String.class;
		}

		@Override
		public boolean projectSwitchable() {
			return true;
		}

		@Override
		public String displayName() {
			return "Switchable";
		}

		@Override
		public AssistantResult analyze(AssistantContext context, String target) {
			return AssistantResult.builder().assistantId(assistantId()).build();
		}
	}

	private static final class StringAssistant implements RequelAssistant<String> {
		@Override
		public String assistantId() {
			return "string-assistant";
		}

		@Override
		public Class<String> targetType() {
			return String.class;
		}

		@Override
		public AssistantResult analyze(AssistantContext context, String target) {
			return AssistantResult.builder().assistantId(assistantId()).build();
		}
	}

	private static final class NumberAssistant implements RequelAssistant<Number> {
		@Override
		public String assistantId() {
			return "number-assistant";
		}

		@Override
		public Class<Number> targetType() {
			return Number.class;
		}

		@Override
		public AssistantResult analyze(AssistantContext context, Number target) {
			return AssistantResult.builder().assistantId(assistantId()).build();
		}
	}
}
