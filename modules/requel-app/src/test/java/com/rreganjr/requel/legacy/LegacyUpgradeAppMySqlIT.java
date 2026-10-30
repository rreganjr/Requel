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
package com.rreganjr.requel.legacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.MountableFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.ImportProjectCommand;
import com.rreganjr.requel.service.query.ProjectContentQueryService;
import com.rreganjr.requel.service.query.ProjectContentQueryService.AnnotationMode;
import com.rreganjr.requel.user.User;

/**
 * Issue #380: Requel starts on an upgraded old database and has the old data. Each subclass is
 * one release: its own MySQL with that release's fixture loaded before the context starts, and
 * {@code requel.upgrade.from-1x=true}, so startup runs the in-place upgrade and then Flyway.
 *
 * <p>The parity test is the one that finds data an old release stored differently: the
 * upgraded sample project must read the same as the same XML imported into this app.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@TestPropertySource(properties = "requel.admin.force-password-change=true")
public abstract class LegacyUpgradeAppMySqlIT extends AbstractIntegrationTestCase {

	/** The fixture's project, imported by the old release from doc/samples/Requel.xml. */
	static final String PROJECT = "Requel";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ProjectContentQueryService contentQueryService;

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	/** The release the subclass's fixture comes from. */
	protected abstract String version();

	/** Starts the container, loads the release's fixture, and points the app at it. */
	static void useFixture(MySQLContainer<?> mysql, String version, DynamicPropertyRegistry registry) {
		if (!mysql.isRunning()) {
			mysql.start();
			String file = "/tmp/requel-" + version + ".sql";
			mysql.copyFileToContainer(MountableFile.forClasspathResource("upgrade/requel-" + version + ".sql"), file);
			try {
				var result = mysql.execInContainer("sh", "-c",
						"mysql -uroot -p" + mysql.getPassword() + " " + mysql.getDatabaseName() + " < " + file);
				if (result.getExitCode() != 0) {
					throw new IllegalStateException(result.getStderr());
				}
			} catch (Exception e) {
				throw new IllegalStateException("Could not load the " + version + " fixture", e);
			}
		}
		registry.add("spring.datasource.url", () -> mysql.getJdbcUrl()
				+ "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC");
		registry.add("spring.datasource.username", () -> "root");
		registry.add("spring.datasource.password", mysql::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
		registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("requel.upgrade.from-1x", () -> "true");
	}

	static MySQLContainer<?> mysql() {
		return new MySQLContainer<>("mysql:8.4")
				.withDatabaseName("requel")
				.withUsername("requel")
				.withPassword("requel")
				.withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_ai_ci");
	}

	@Test
	void flywayRecordsTheUpgradeAsItsBaseline() {
		Map<String, Object> baseline = jdbcTemplate.queryForMap(
				"SELECT version, description FROM flyway_schema_history ORDER BY installed_rank LIMIT 1");
		assertEquals("1", baseline.get("version"));
		assertThat((String) baseline.get("description")).contains("Requel " + version());
	}

	@Test
	void theOldUsersSignInWithTheirOldPasswords() throws Exception {
		signIn("project", "project").andExpect(jsonPath("$.user.mustChangePassword").value(false));
	}

	@Test
	void theBuiltInAdminStillOnTheDefaultPasswordMustChangeIt() throws Exception {
		// #381 applies to an upgraded install as to a new one.
		signIn("admin", "admin").andExpect(jsonPath("$.user.mustChangePassword").value(true));
	}

	@Test
	void theUpgradedProjectReadsTheSameAsTheSampleImportedHere() throws Exception {
		Project upgraded = getProjectRepository().findProjectByName(PROJECT);
		Project imported = importSample(PROJECT + " imported " + System.currentTimeMillis());

		JsonNode expected = normalize(read(imported));
		JsonNode actual = normalize(read(upgraded));

		assertEquals(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(expected),
				objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actual));
	}

	// ---- helpers ------------------------------------------------------------------------------

	private org.springframework.test.web.servlet.ResultActions signIn(String username, String password)
			throws Exception {
		return mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
				.andExpect(status().isOk());
	}

	/** Read as the sample's creator; the project DTO carries the reader's permissions. */
	private JsonNode read(Project project) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("admin", null, List.of()));
		try {
			return objectMapper.valueToTree(contentQueryService.readUncapped(project, AnnotationMode.ALL));
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	/**
	 * The sample as the old release could read it. Neither 1.0.x nor 1.1.0 can import its
	 * glossary, so the fixture's project was imported without it (scripts/legacy-fixtures.sh);
	 * and both skip {@code <ignorePosition>}, an element they don't know (the sample's
	 * "Ignore this word." position, which a dozen lexical issues share), so the old database
	 * never had it. Both are removed here too, along with the references to them.
	 */
	private Project importSample(String name) throws Exception {
		String xml;
		try (InputStream in = Objects.requireNonNull(getClass().getClassLoader()
				.getResourceAsStream("doc/samples/Requel.xml"), "doc/samples/Requel.xml")) {
			xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		xml = Pattern.compile("<glossary>.*?</glossary>", Pattern.DOTALL).matcher(xml).replaceAll("<glossary/>");
		var ignorePositions = Pattern.compile("<ignorePosition [^>]*id=\"([^\"]+)\".*?</ignorePosition>", Pattern.DOTALL)
				.matcher(xml);
		java.util.List<String> ids = new java.util.ArrayList<>();
		while (ignorePositions.find()) {
			ids.add(ignorePositions.group(1));
		}
		xml = ignorePositions.replaceAll("");
		for (String id : ids) {
			xml = xml.replaceAll("\\s*<positionRef>" + Pattern.quote(id) + "</positionRef>", "");
		}
		User admin = getUserRepository().findUserByUsername("admin");
		ImportProjectCommand command = applicationContext.getBean("importProjectCommand", ImportProjectCommand.class);
		command.setAnalysisEnabled(false);
		command.setEditedBy(admin);
		command.setName(name);
		command.setInputStream(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		getCommandHandler().execute(command);
		return getProjectRepository().findProjectByName(name);
	}

	/**
	 * Fields that differ between any two copies of a project (ids, versions, dates, the name given
	 * at import, sizes), and three that differ because an import and a migrated database are
	 * treated differently, not because the upgrade lost anything:
	 * <ul>
	 * <li>{@code createdBy}: an import attributes annotations to the importing user; the old
	 * database kept the file's authors (the assistant), and the upgrade keeps them;</li>
	 * <li>{@code mustBeResolved}: V24 made existing lexical issues advisory; an import keeps the
	 * file's value;</li>
	 * <li>{@code positionType}: a later migration typed the old "Ignore this phrase." positions as
	 * ignore positions; the file has a plain {@code <position>}.</li>
	 * </ul>
	 */
	private static final Set<String> IGNORED = Set.of("id", "version", "characters", "maxCharacters",
			"dateCreated", "createdAt", "updatedAt", "projectName", "createdBy", "mustBeResolved",
			"positionType");

	private static JsonNode normalize(JsonNode node) {
		if (node instanceof ObjectNode object) {
			for (Iterator<String> names = object.fieldNames(); names.hasNext();) {
				String field = names.next();
				if (IGNORED.contains(field) || field.endsWith("Id") || field.endsWith("Ids")) {
					names.remove();
				}
			}
			if (object.has("project") && object.get("project").has("name")) {
				((ObjectNode) object.get("project")).remove("name");
			}
			object.fields().forEachRemaining(e -> normalize(e.getValue()));
		} else if (node instanceof ArrayNode array) {
			// Order isn't data: sets come back in whatever order the rows were read.
			java.util.List<JsonNode> items = new java.util.ArrayList<>();
			array.forEach(item -> items.add(normalize(item)));
			items.sort(java.util.Comparator.comparing(JsonNode::toString));
			array.removeAll();
			array.addAll(items);
		}
		return node;
	}
}
