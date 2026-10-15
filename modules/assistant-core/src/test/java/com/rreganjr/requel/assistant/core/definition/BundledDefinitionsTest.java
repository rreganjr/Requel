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
package com.rreganjr.requel.assistant.core.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import com.fasterxml.jackson.databind.ObjectMapper;

/** #260: the bundled definition file format. */
class BundledDefinitionsTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void aFileReadsIntoABundledDefinition() throws Exception {
		AssistantDefinition definition = BundledDefinitions.fromJson(objectMapper.readTree("""
				{"key":"k","displayName":"K","taskType":"REQUIREMENTS_REVIEW","version":3,
				 "scope":["Goal"],"contextProviders":["entity"],
				 "outputSchemaName":"RequirementsReviewOutput","outputSchemaVersion":"1",
				 "vocabulary":[{"type":"AMBIGUOUS","description":"unclear"}],
				 "instructions":"Do it."}
				"""));
		assertThat(definition.key()).isEqualTo("k");
		assertThat(definition.kind()).isEqualTo(DefinitionKind.REVIEW);
		assertThat(definition.enabled()).isTrue(); // default
		assertThat(definition.version()).isEqualTo(3);
		assertThat(definition.source()).isEqualTo(DefinitionSource.BUNDLED);
		assertThat(definition.projectId()).isNull();
		assertThat(definition.scope()).containsExactly("Goal");
		assertThat(definition.vocabulary()).containsExactly(new VocabularyEntry("AMBIGUOUS",
				"unclear"));
	}

	@Test
	void aFileCanOverrideContextBudgets() throws Exception {
		AssistantDefinition definition = BundledDefinitions.fromJson(objectMapper.readTree("""
				{"key":"k","displayName":"K","taskType":"REQUIREMENTS_REVIEW","version":1,
				 "contextProviders":["entity","goal-siblings"],
				 "contextBudgets":{"goal-siblings":12000},
				 "outputSchemaName":"RequirementsReviewOutput","outputSchemaVersion":"1",
				 "vocabulary":[{"type":"AMBIGUOUS","description":"unclear"}],
				 "instructions":"Do it."}
				"""));
		assertThat(definition.contextBudgets()).containsExactly(java.util.Map.entry("goal-siblings",
				12000));
	}

	@Test
	void anUnreadableFileNamesItself() {
		assertThatThrownBy(() -> BundledDefinitions.parse(objectMapper,
				new ByteArrayResource("{not json".getBytes()), "broken.json"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Bundled assistant definition broken.json could not be read");
	}

	@Test
	void anInvalidFileFailsWithItsNameAndEveryProblem() {
		ByteArrayResource bad = new ByteArrayResource(("{\"key\":\"bad\",\"displayName\":\"B\","
				+ "\"taskType\":\"REQUIREMENTS_REVIEW\",\"scope\":[\"Widget\"],"
				+ "\"contextProviders\":[\"entity\"],\"outputSchemaName\":\"Nope\","
				+ "\"outputSchemaVersion\":\"1\",\"vocabulary\":[],\"instructions\":\"x\"}")
				.getBytes()) {
			@Override
			public String getFilename() {
				return "bad.json";
			}
		};
		assertThatThrownBy(() -> BundledDefinitions.load(objectMapper,
				new AssistantDefinitionValidator(16000), new org.springframework.core.io.Resource[] {
						bad }))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Bundled assistant definition bad.json is invalid")
				.hasMessageContaining("unknown entity type(s) in scope: [Widget]")
				.hasMessageContaining("vocabulary is empty")
				.hasMessageContaining("output schema Nope v1");
	}

	@Test
	void theTestClasspathHasNoBundledFiles() {
		// assistant-core ships none; assistant-ai ships the default (covered there).
		assertThat(BundledDefinitions.load(objectMapper, new AssistantDefinitionValidator(16000)))
				.isEmpty();
	}
}
