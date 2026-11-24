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
package com.rreganjr.requel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditReportGeneratorCommand;
import com.rreganjr.requel.user.User;

/**
 * #526: report generators are annotatable. The report editor's Annotations section reads
 * {@code GET /api/annotations?entityType=ReportGenerator}, which was rejected with "Unknown entity
 * type" because the type was never registered. Through the HTTP API, as the editor does: notes
 * and issues added to a report are listed back, and deleting the report removes their links.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ReportAnnotationsApiIT extends AbstractIntegrationTestCase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbc;

	private String projectName;
	private long reportId;
	private String token;

	@BeforeEach
	void setUp() throws Exception {
		initializeBaselineData();
		User admin = getUserRepository().findUserByUsername("admin");
		projectName = "report-annotations-" + System.nanoTime();
		EditProjectCommand projectCommand = getProjectCommandFactory().newEditProjectCommand();
		projectCommand.setEditedBy(admin);
		projectCommand.setName(projectName);
		projectCommand.setText("report annotations test");
		projectCommand.setOrganizationName("ReportAnnotationsOrg-" + projectName);
		Project project = getCommandHandler().execute(projectCommand).getProject();

		EditReportGeneratorCommand reportCommand = getProjectCommandFactory().newEditReportGeneratorCommand();
		reportCommand.setEditedBy(admin);
		reportCommand.setProjectOrDomain(project);
		reportCommand.setName("Annotated report " + System.nanoTime());
		reportCommand.setText("<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"/>");
		reportId = getCommandHandler().execute(reportCommand).getReportGenerator().getId();

		token = objectMapper.readTree(mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin\",\"password\":\"admin\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
				.get("token").asText();
	}

	@Test
	void aReportsNotesAndIssuesAreListedBack() throws Exception {
		command("EditNote", Map.of("projectName", projectName, "entityType", "ReportGenerator",
				"entityId", reportId, "text", "Check the template against the 2.2 schema."))
				.andExpect(status().isOk());
		command("EditIssue", Map.of("projectName", projectName, "entityType", "ReportGenerator",
				"entityId", reportId, "text", "The template leaves out use cases.",
				"mustBeResolved", false, "severity", "LOW"))
				.andExpect(status().isOk());

		JsonNode annotations = annotations().andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString().transform(this::json);

		assertThat(annotations.path("notes")).extracting(n -> n.path("text").asText())
				.containsExactly("Check the template against the 2.2 schema.");
		assertThat(annotations.path("issues")).extracting(i -> i.path("text").asText())
				.containsExactly("The template leaves out use cases.");
	}

	@Test
	void aReportWithNoAnnotationsListsNone() throws Exception {
		JsonNode annotations = annotations().andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString().transform(this::json);

		assertThat(annotations.path("notes")).isEmpty();
		assertThat(annotations.path("issues")).isEmpty();
	}

	@Test
	void deletingAReportRemovesItsAnnotationLinks() throws Exception {
		command("EditNote", Map.of("projectName", projectName, "entityType", "ReportGenerator",
				"entityId", reportId, "text", "A note on a report about to be deleted."))
				.andExpect(status().isOk());
		assertThat(links()).isEqualTo(1);

		command("DeleteReportGenerator", Map.of("projectName", projectName, "reportId", reportId))
				.andExpect(status().isOk());

		assertThat(links()).isZero();
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reports_annotations WHERE report_generator_impl_id = ?",
				Integer.class, reportId)).isZero();
	}

	private ResultActions command(String type, Map<String, Object> input) throws Exception {
		return mockMvc.perform(post("/api/commands/" + type)
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(input)));
	}

	private ResultActions annotations() throws Exception {
		return mockMvc.perform(get("/api/annotations")
				.header("Authorization", "Bearer " + token)
				.param("projectName", projectName)
				.param("entityType", "ReportGenerator")
				.param("entityId", String.valueOf(reportId)));
	}

	/** Rows linking an annotation to this report, as the @Any mapping stores them. */
	private int links() {
		return jdbc.queryForObject("SELECT COUNT(*) FROM annotation_annotatable"
				+ " WHERE annotatable_type = 'ReportGenerator' AND annotatable_id = ?",
				Integer.class, reportId);
	}

	private JsonNode json(String body) {
		try {
			return objectMapper.readTree(body);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
