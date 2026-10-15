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
package com.rreganjr.requel.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.rreganjr.requel.DescribedValue;
import com.rreganjr.requel.project.GoalRelationType;
import com.rreganjr.requel.service.api.AllowedValues;
import com.rreganjr.requel.service.api.dto.EditGoalRelationInput;
import com.rreganjr.requel.service.api.dto.EditIssueInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link CommandInputSchema}: every Java-to-JSON type mapping, required-field derivation
 * from {@code @NotNull}/{@code @NotBlank}, declaration-order preservation, the empty/{@link Void}/
 * non-record cases, and the small schema-node builder helpers. This is the shared generator that
 * MCP typed tools, the CLI's typed subcommands, and the descriptor endpoint all rely on (issues
 * #103/#104), so pinning its behavior here guards every front-end at once.
 */
class CommandInputSchemaTest {

	enum Color {
		RED, GREEN
	}

	/** A record covering every jsonType branch, with a required String and a required Long. */
	record RichInput(
			@NotBlank String name,
			@NotNull Long id,
			int count,
			boolean flag,
			Double ratio,
			Color color,
			List<String> tags,
			Object blob) {
	}

	/** A vocabulary that carries its meanings (issue #257). */
	enum Kind implements DescribedValue {
		Near("Close by."), Far("A long way off.");

		private final String description;

		Kind(String description) {
			this.description = description;
		}

		@Override
		public String getDescription() {
			return description;
		}
	}

	/** String fields restricted to an enum's names; the input stays a String (issue #257). */
	record AllowedInput(
			@NotBlank @AllowedValues(Kind.class) String kind,
			@AllowedValues(Color.class) String color,
			String free) {
	}

	/** Not a record — must be treated like Void (empty schema, no field names). */
	static class NotARecord {
	}

	@Test
	@SuppressWarnings("unchecked")
	void mapsEveryJavaTypeToItsJsonType() {
		Map<String, Object> schema = CommandInputSchema.of(RichInput.class);
		var props = (Map<String, Object>) schema.get("properties");

		assertThat(props.get("name")).isEqualTo(Map.of("type", "string"));
		assertThat(props.get("id")).isEqualTo(Map.of("type", "integer"));    // Long
		assertThat(props.get("count")).isEqualTo(Map.of("type", "integer")); // primitive int
		assertThat(props.get("flag")).isEqualTo(Map.of("type", "boolean"));  // primitive boolean
		assertThat(props.get("ratio")).isEqualTo(Map.of("type", "number"));  // Double
		assertThat(props.get("color")).isEqualTo(Map.of("type", "string"));  // enum → string
		assertThat(props.get("tags")).isEqualTo(Map.of("type", "array"));    // List → array
		assertThat(props.get("blob")).isEqualTo(Map.of("type", "object"));   // fallback object
		assertThat(schema).containsEntry("type", "object");
		assertThat(schema).containsEntry("additionalProperties", false);
	}

	@Test
	@SuppressWarnings("unchecked")
	void derivesRequiredOnlyFromValidationAnnotations() {
		var required = (List<String>) CommandInputSchema.of(RichInput.class).get("required");
		// Only the @NotBlank/@NotNull components; unannotated ones (incl. primitives) are optional.
		assertThat(required).containsExactlyInAnyOrder("name", "id");
		assertThat(required).doesNotContain("count", "flag", "ratio", "color", "tags", "blob");
	}

	@Test
	@SuppressWarnings("unchecked")
	void propertiesPreserveDeclarationOrder() {
		var props = (Map<String, Object>) CommandInputSchema.of(RichInput.class).get("properties");
		assertThat(props.keySet())
				.containsExactly("name", "id", "count", "flag", "ratio", "color", "tags", "blob");
	}

	@Test
	@SuppressWarnings("unchecked")
	void voidNullAndNonRecordYieldEmptyObjectSchema() {
		for (Class<?> type : new Class<?>[] {Void.class, null, NotARecord.class}) {
			Map<String, Object> schema = CommandInputSchema.of(type);
			assertThat((Map<String, Object>) schema.get("properties")).isEmpty();
			assertThat((List<String>) schema.get("required")).isEmpty();
			assertThat(schema).containsEntry("type", "object");
			assertThat(schema).containsEntry("additionalProperties", false);
		}
	}

	@Test
	void fieldNamesReflectDeclarationOrderAndEmptyForNonRecords() {
		assertThat(CommandInputSchema.fieldNames(RichInput.class))
				.containsExactly("name", "id", "count", "flag", "ratio", "color", "tags", "blob");
		assertThat(CommandInputSchema.fieldNames(Void.class)).isEmpty();
		assertThat(CommandInputSchema.fieldNames(null)).isEmpty();
		assertThat(CommandInputSchema.fieldNames(NotARecord.class)).isEmpty();
	}

	@Test
	void builderHelpersProduceExpectedNodes() {
		assertThat(CommandInputSchema.stringType()).isEqualTo(Map.of("type", "string"));
		assertThat(CommandInputSchema.integerType()).isEqualTo(Map.of("type", "integer"));
		assertThat(CommandInputSchema.booleanType()).isEqualTo(Map.of("type", "boolean"));
		assertThat(CommandInputSchema.objectSchema(
				Map.of("x", CommandInputSchema.stringType()), List.of("x")))
				.isEqualTo(Map.of("type", "object",
						"properties", Map.of("x", Map.of("type", "string")),
						"required", List.of("x"),
						"additionalProperties", false));
	}

	@Test
	@SuppressWarnings("unchecked")
	void allowedValuesListsTheEnumNamesAndDescribesADescribedValue() {
		Map<String, Object> schema = CommandInputSchema.of(AllowedInput.class);
		var props = (Map<String, Object>) schema.get("properties");

		assertThat(props.get("kind")).isEqualTo(Map.of("type", "string", "enum", List.of("Near", "Far"),
				"description", "Near: Close by. Far: A long way off."));
		// A plain enum lists its names with no description.
		assertThat(props.get("color")).isEqualTo(Map.of("type", "string", "enum", List.of("RED", "GREEN")));
		assertThat(props.get("free")).isEqualTo(Map.of("type", "string"));
		// @AllowedValues does not change what is required.
		assertThat((List<String>) schema.get("required")).containsExactly("kind");
	}

	@Test
	@SuppressWarnings("unchecked")
	void editGoalRelationSchemaListsEveryRelationTypeWithItsMeaning() {
		var props = (Map<String, Object>) CommandInputSchema.of(EditGoalRelationInput.class)
				.get("properties");
		var relationType = (Map<String, Object>) props.get("relationType");
		assertThat((List<String>) relationType.get("enum")).containsExactly("Supports", "Conflicts",
				"Refines", "Duplicates", "DependsOn", "Obstructs", "Measures");
		assertThat((String) relationType.get("description"))
				.contains("Measures: " + GoalRelationType.Measures.getDescription());
	}

	@Test
	@SuppressWarnings("unchecked")
	void editIssueSchemaListsTheSeverities() {
		var props = (Map<String, Object>) CommandInputSchema.of(EditIssueInput.class).get("properties");
		assertThat(props.get("severity"))
				.isEqualTo(Map.of("type", "string", "enum", List.of("LOW", "MEDIUM", "HIGH")));
	}
}
