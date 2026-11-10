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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.user.User;

/**
 * #480, found in the 2.1.0-rc3 acceptance test. A project's glossary is a set sorted by name,
 * backed by the join table {@code pods_glossary_terms}. Renaming a term through the API (#348)
 * left the renamed term where its old name sorted in that set, which the request had already
 * loaded to find the term; adding the old name as a synonym then made Hibernate diff the set at
 * flush, and it deleted the join row of a term it no longer found there. The term stayed in the
 * project, but every lookup through the glossary missed it: editing a synonym of it was refused
 * with "GlossaryTerm not found".
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GlossaryRenameApiIT extends AbstractIntegrationTestCase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbc;

	private String projectName;
	private String token;

	@BeforeEach
	void setUp() throws Exception {
		initializeBaselineData();
		User admin = getUserRepository().findUserByUsername("admin");
		projectName = "glossary-rename-" + System.nanoTime();
		EditProjectCommand command = getProjectCommandFactory().newEditProjectCommand();
		command.setEditedBy(admin);
		command.setName(projectName);
		command.setText("glossary rename test");
		command.setOrganizationName("GlossaryRenameOrg-" + projectName);
		getCommandHandler().execute(command);
		token = objectMapper.readTree(mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin\",\"password\":\"admin\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
				.get("token").asText();
	}

	@Test
	void renamingASynonymKeepsItsCanonicalTermInTheGlossary() throws Exception {
		long yellow = id(edit(null, "Yellow Snow", "Don't eat it.", null));
		long cone = id(edit(null, "Pee Pee Snow Cone", "Yuck.", yellow));

		id(edit(cone, "Zoiniks", "Yuck.", yellow));

		assertThat(glossary()).containsExactlyInAnyOrder("Yellow Snow", "Zoiniks",
				"Pee Pee Snow Cone");
		edit(cone, "Zoiniks", "Still yuck.", yellow).andExpect(status().isOk());
	}

	@Test
	void renamingACanonicalTermKeepsEveryTermInTheGlossary() throws Exception {
		long alpha = id(edit(null, "Alpha", "First.", null));
		id(edit(null, "Middle", "Second.", null));
		id(edit(null, "Omega", "Last.", alpha));

		id(edit(alpha, "Zulu", "First.", null));

		assertThat(glossary()).containsExactlyInAnyOrder("Zulu", "Alpha", "Middle", "Omega");
	}

	private ResultActions edit(Long termId, String name, String text, Long canonicalTermId)
			throws Exception {
		Map<String, Object> input = new HashMap<>();
		input.put("projectName", projectName);
		input.put("termId", termId);
		input.put("name", name);
		input.put("text", text);
		input.put("canonicalTermId", canonicalTermId);
		return mockMvc.perform(post("/api/commands/EditGlossaryTerm")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(input)));
	}

	private long id(ResultActions actions) throws Exception {
		return objectMapper.readTree(actions.andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString()).path("entity").path("id").asLong();
	}

	/** The project's glossary as its join rows hold it, which is what the API looks terms up in. */
	private List<String> glossary() {
		return jdbc.queryForList("select t.name from pods_glossary_terms j"
				+ " join terms t on t.id = j.glossary_terms_id"
				+ " join pods p on p.id = j.abstract_project_or_domain_id where p.name = ?",
				String.class, projectName);
	}
}
