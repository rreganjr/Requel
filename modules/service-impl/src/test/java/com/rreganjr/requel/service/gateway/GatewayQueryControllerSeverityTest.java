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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.rreganjr.requel.annotation.IssueSeverity;
import com.rreganjr.requel.gateway.QueryGateway;
import com.rreganjr.requel.service.api.dto.AnnotationsDto;
import com.rreganjr.requel.service.api.dto.EntityReferenceDto;
import com.rreganjr.requel.service.api.dto.GlossaryTermDto;
import com.rreganjr.requel.service.api.dto.OpenIssueDto;
import com.rreganjr.requel.service.api.dto.ProjectContentDto;
import com.rreganjr.requel.service.api.dto.ProjectDto;
import com.rreganjr.requel.service.api.dto.ProjectTreeNodeDto;
import com.rreganjr.requel.service.config.ApiExceptionHandler;

/**
 * Issue #338: the gateway's open-issues and context endpoints parse {@code minSeverity} (any case,
 * absent or blank for none) and hand the gateway the typed value; anything else is a 400 naming
 * the parameter, and the gateway is never called.
 */
class GatewayQueryControllerSeverityTest {

	private String called;
	private IssueSeverity minSeveritySeen;

	private final QueryGateway gateway = new QueryGateway() {
		@Override
		public List<OpenIssueDto> getOpenIssues(String n, IssueSeverity min) {
			called = "openIssues";
			minSeveritySeen = min;
			return List.of();
		}

		@Override
		public Map<String, Object> getProjectContext(String n, IssueSeverity min) {
			called = "context";
			minSeveritySeen = min;
			return Map.of("openIssues", List.of());
		}

		@Override public List<ProjectDto> listProjects() { return List.of(); }
		@Override public ProjectDto getProject(String n) { return null; }
		@Override public List<ProjectTreeNodeDto> getProjectTree(String n) { return List.of(); }
		@Override public List<GlossaryTermDto> getGlossaryTerms(String n) { return List.of(); }
		@Override public AnnotationsDto getAnnotations(String n, String t, long id) { return null; }
		@Override public Object getEntity(String n, String t, long id) { return null; }
		@Override public Map<String, List<EntityReferenceDto>> getEntityNeighbors(String n,
				String t, long id) { return Map.of(); }
		@Override public List<EntityReferenceDto> searchProjectEntities(String n, String q) {
			return List.of(); }
		@Override public ProjectContentDto getProjectContent(String n, String a) { return null; }
	};

	private final MockMvc mvc = MockMvcBuilders
			.standaloneSetup(new GatewayQueryController(gateway))
			.setControllerAdvice(new ApiExceptionHandler())
			.build();

	@Test
	void openIssuesPassesTheParsedMinimum() throws Exception {
		mvc.perform(get("/api/gateway/query/projects/Demo/open-issues").param("minSeverity", "medium"))
				.andExpect(status().isOk());
		assertThat(called).isEqualTo("openIssues");
		assertThat(minSeveritySeen).isEqualTo(IssueSeverity.MEDIUM);
	}

	@Test
	void openIssuesWithoutAMinimumPassesNull() throws Exception {
		mvc.perform(get("/api/gateway/query/projects/Demo/open-issues")).andExpect(status().isOk());
		assertThat(called).isEqualTo("openIssues");
		assertThat(minSeveritySeen).isNull();
	}

	@Test
	void contextPassesTheParsedMinimum() throws Exception {
		mvc.perform(get("/api/gateway/query/projects/Demo/context").param("minSeverity", "HIGH"))
				.andExpect(status().isOk());
		assertThat(called).isEqualTo("context");
		assertThat(minSeveritySeen).isEqualTo(IssueSeverity.HIGH);
	}

	@Test
	void anUnknownMinimumIsA400NamingItOnBothEndpoints() throws Exception {
		for (String path : List.of("open-issues", "context")) {
			mvc.perform(get("/api/gateway/query/projects/Demo/" + path)
					.param("minSeverity", "urgent"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.error").value("BAD_REQUEST"))
					.andExpect(jsonPath("$.message").value(
							"Unsupported minSeverity value: urgent; expected LOW, MEDIUM or HIGH"));
		}
		assertThat(called).isNull();
	}
}
