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
package com.rreganjr.requel.service.gateway;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.rreganjr.requel.gateway.ProjectContentTooLargeException;
import com.rreganjr.requel.gateway.ProjectContentTooLargeException.SectionSize;
import com.rreganjr.requel.gateway.QueryGateway;
import com.rreganjr.requel.service.api.dto.EntityReferenceDto;
import com.rreganjr.requel.service.api.dto.AnnotationsDto;
import com.rreganjr.requel.service.api.dto.GlossaryTermDto;
import com.rreganjr.requel.service.api.dto.OpenIssueDto;
import com.rreganjr.requel.service.api.dto.ProjectContentDto;
import com.rreganjr.requel.service.api.dto.ProjectDto;
import com.rreganjr.requel.service.api.dto.ProjectTreeNodeDto;
import com.rreganjr.requel.service.config.ApiExceptionHandler;

/**
 * Issue #274: the REST face of the project content read — the annotation mode reaches the gateway,
 * and a project over the cap is a 422 {@code CONTENT_TOO_LARGE} whose message names the overflow
 * (a {@code ResponseStatusException} reason would not reach the caller).
 */
class GatewayQueryControllerContentTest {

	private String modeSeen;

	private final QueryGateway gateway = new QueryGateway() {
		@Override
		public ProjectContentDto getProjectContent(String projectName, String annotations) {
			modeSeen = annotations;
			if ("Huge".equals(projectName)) {
				Map<String, SectionSize> sections = new LinkedHashMap<>();
				sections.put("goals", new SectionSize(3, 40));
				sections.put("annotations", new SectionSize(9, 900));
				throw new ProjectContentTooLargeException(projectName, 940, 100, sections);
			}
			return new ProjectContentDto(null, "OPEN", 0, 100, List.of(), List.of(), List.of(),
					List.of(), List.of(), List.of(), List.of(), List.of());
		}

		@Override public List<ProjectDto> listProjects() { return List.of(); }
		@Override public ProjectDto getProject(String n) { return null; }
		@Override public List<ProjectTreeNodeDto> getProjectTree(String n) { return List.of(); }
		@Override public List<GlossaryTermDto> getGlossaryTerms(String n) { return List.of(); }
		@Override public List<OpenIssueDto> getOpenIssues(String n) { return List.of(); }
		@Override public AnnotationsDto getAnnotations(String n, String t, long id) { return null; }
		@Override public Object getEntity(String n, String t, long id) { return null; }
		@Override public Map<String, List<EntityReferenceDto>> getEntityNeighbors(String n,
				String t, long id) { return Map.of(); }
		@Override public List<EntityReferenceDto> searchProjectEntities(String n, String q) {
			return List.of(); }
		@Override public Map<String, Object> getProjectContext(String n) { return Map.of(); }
	};

	private final MockMvc mvc = MockMvcBuilders
			.standaloneSetup(new GatewayQueryController(gateway))
			.setControllerAdvice(new ApiExceptionHandler())
			.build();

	@Test
	void theAnnotationModeReachesTheGateway() throws Exception {
		mvc.perform(get("/api/gateway/query/projects/Demo/content").param("annotations", "open"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.annotations").value("OPEN"));
		org.assertj.core.api.Assertions.assertThat(modeSeen).isEqualTo("open");
	}

	@Test
	void anAbsentModeIsNull() throws Exception {
		mvc.perform(get("/api/gateway/query/projects/Demo/content")).andExpect(status().isOk());
		org.assertj.core.api.Assertions.assertThat(modeSeen).isNull();
	}

	@Test
	void overTheCapIsA422NamingTheOverflow() throws Exception {
		mvc.perform(get("/api/gateway/query/projects/Huge/content"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.error").value(ProjectContentTooLargeException.CODE))
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.allOf(
						org.hamcrest.Matchers.containsString("940 characters"),
						org.hamcrest.Matchers.containsString("the cap is 100"),
						org.hamcrest.Matchers.containsString("goals 3 / 40"),
						org.hamcrest.Matchers.containsString("annotations 9 / 900"),
						org.hamcrest.Matchers.containsString("annotations=none"))));
	}
}
