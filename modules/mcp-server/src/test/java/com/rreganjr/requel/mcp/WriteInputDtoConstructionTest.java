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
package com.rreganjr.requel.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Constructs every allowlisted write input DTO (the records annotated with @NotNull/@NotBlank in
 * issue #104) through its canonical constructor and exercises its generated members. These records
 * are otherwise only instantiated on the live command path; this pins their generated code and the
 * fact that the added validation annotations do not interfere with construction.
 */
class WriteInputDtoConstructionTest {

	private static final String PKG = "com.rreganjr.requel.service.api.dto.";

	private static final List<String> WRITE_INPUT_DTOS = List.of(
			"AddActorToActorContainerInput",
			"AddGoalToGoalContainerInput",
			"AddScenarioToUseCaseInput",
			"AddStoryToStoryContainerInput",
			"CopyActorInput",
			"CopyGoalInput",
			"CopyScenarioInput",
			"CopyStoryInput",
			"CopyUseCaseInput",
			"DeleteActorInput",
			"DeleteArgumentInput",
			"DeleteGlossaryTermInput",
			"DeleteGoalInput",
			"DeleteGoalRelationInput",
			"DeleteIssueInput",
			"DeleteNoteInput",
			"DeletePositionInput",
			"DeleteReportGeneratorInput",
			"DeleteScenarioInput",
			"DeleteStakeholderInput",
			"DeleteStoryInput",
			"DeleteUseCaseInput",
			"EditActorInput",
			"EditArgumentInput",
			"EditGlossaryTermInput",
			"EditGoalInput",
			"EditGoalRelationInput",
			"EditIssueInput",
			"EditNonUserStakeholderInput",
			"EditNoteInput",
			"EditPositionInput",
			"EditReportGeneratorInput",
			"EditScenarioInput",
			"EditStoryInput",
			"EditUseCaseInput",
			"RemoveActorFromActorContainerInput",
			"RemoveGoalFromGoalContainerInput",
			"RemoveScenarioFromUseCaseInput",
			"RemoveStoryFromStoryContainerInput",
			"SetPrimaryScenarioInput");

	@Test
	void everyWriteInputDtoCanBeConstructedAndAccessed() throws Exception {
		for (String simpleName : WRITE_INPUT_DTOS) {
			Class<?> type = Class.forName(PKG + simpleName);
			assertThat(type.isRecord()).as("%s should be a record", simpleName).isTrue();

			RecordComponent[] components = type.getRecordComponents();
			Class<?>[] paramTypes = new Class<?>[components.length];
			Object[] args = new Object[components.length];
			for (int i = 0; i < components.length; i++) {
				paramTypes[i] = components[i].getType();
				args[i] = defaultValue(components[i].getType());
			}

			Constructor<?> canonical = type.getDeclaredConstructor(paramTypes);
			Object instance = canonical.newInstance(args);

			// Exercise the generated accessors, equals/hashCode/toString.
			for (RecordComponent component : components) {
				component.getAccessor().invoke(instance);
			}
			assertThat(instance).isEqualTo(instance);
			assertThat(instance.hashCode()).isEqualTo(instance.hashCode());
			assertThat(instance.toString()).contains(simpleName);
		}
	}

	/** Non-null defaults for primitives (which cannot accept null); null for reference types. */
	private static Object defaultValue(Class<?> type) {
		if (type == int.class || type == long.class || type == short.class || type == byte.class) {
			return type == long.class ? 0L : type == short.class ? (short) 0
					: type == byte.class ? (byte) 0 : 0;
		}
		if (type == boolean.class) {
			return false;
		}
		if (type == double.class) {
			return 0d;
		}
		if (type == float.class) {
			return 0f;
		}
		if (type == char.class) {
			return '\0';
		}
		return null;
	}
}
