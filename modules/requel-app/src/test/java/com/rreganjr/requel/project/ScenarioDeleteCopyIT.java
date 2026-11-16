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
package com.rreganjr.requel.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.service.api.dto.ScenarioDto;
import com.rreganjr.requel.service.api.dto.StepDto;
import com.rreganjr.requel.service.api.dto.UseCaseDto;
import com.rreganjr.requel.user.User;

/**
 * Issue #341, through the in-process gateway as the project's creator (admin):
 * <ul>
 * <li>DeleteScenario on a use case's primary scenario is refused as invalid input naming the use
 * case (it used to fail on the {@code usecases.scenario_id} foreign key).</li>
 * <li>DeleteScenario deletes its own plain steps, keeping one another scenario still uses (they
 * used to stay behind holding their names).</li>
 * <li>CopyScenario links a nested scenario instead of flattening it to a plain step.</li>
 * <li>CopyScenario's free-name search sees plain steps too.</li>
 * <li>DeleteActor on a use case's primary actor is invalid input, not an execution error.</li>
 * </ul>
 * {@code ScenarioDeleteCopyMySqlIT} runs the same tests on MySQL, where the foreign keys and the
 * shared step/scenario unique key are real.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ScenarioDeleteCopyIT extends AbstractIntegrationTestCase {

	@Autowired
	private CommandGateway gateway;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private String projectName;

	@BeforeAll
	void setUpFixture() throws Exception {
		initializeBaselineData();
		User admin = getUserRepository().findUserByUsername("admin");
		long ts = System.currentTimeMillis();
		projectName = "scenario-delete-copy-" + ts;
		EditProjectCommand projectCmd = getProjectCommandFactory().newEditProjectCommand();
		projectCmd.setEditedBy(admin);
		projectCmd.setName(projectName);
		projectCmd.setText("#341 scenario delete and copy");
		projectCmd.setOrganizationName("ScenarioOrg-" + ts);
		getCommandHandler().execute(projectCmd);
	}

	@BeforeEach
	void signIn() {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("admin", "x", List.of()));
	}

	@AfterEach
	void signOut() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void deletingAUseCasesPrimaryScenarioIsRefusedNamingTheUseCase() throws Exception {
		long ts = System.nanoTime();
		UseCaseDto useCase = (UseCaseDto) execute("EditUseCase", Map.of("projectName", projectName,
				"name", "uc-" + ts, "primaryActorName", "actor-" + ts));

		GatewayException ex = assertThrows(GatewayException.class, () -> execute("DeleteScenario",
				Map.of("projectName", projectName, "scenarioId", useCase.scenarioId())));

		assertEquals(GatewayException.Kind.INVALID_INPUT, ex.getKind(), ex.getMessage());
		assertTrue(ex.getMessage().contains("uc-" + ts), ex.getMessage());
		assertEquals(1, rows("SELECT COUNT(*) FROM scenarios WHERE id = ?", useCase.scenarioId()));
	}

	@Test
	void deletingAScenarioDeletesItsOwnStepsButKeepsASharedOne() throws Exception {
		long ts = System.nanoTime();
		ScenarioDto scenario = editScenario("doomed-" + ts, List.of(
				Map.of("name", "own-" + ts, "text", "only here"),
				Map.of("name", "shared-" + ts, "text", "also elsewhere")));
		Long sharedId = scenario.steps().get(1).id();
		editScenario("keeper-" + ts, List.of(Map.of("stepId", sharedId)));

		execute("DeleteScenario", Map.of("projectName", projectName, "scenarioId", scenario.id()));

		assertEquals(0, rows("SELECT COUNT(*) FROM scenarios WHERE name = ?", "doomed-" + ts));
		assertEquals(0, rows("SELECT COUNT(*) FROM scenarios WHERE name = ?", "own-" + ts),
				"the scenario's own plain step is deleted with it");
		assertEquals(1, rows("SELECT COUNT(*) FROM scenarios WHERE id = ?", sharedId),
				"a step another scenario uses is kept");
	}

	@Test
	void copyingAScenarioLinksANestedScenario() throws Exception {
		long ts = System.nanoTime();
		ScenarioDto inner = editScenario("inner-" + ts, List.of(
				Map.of("name", "inner-step-" + ts, "text", "inside")));
		ScenarioDto outer = editScenario("outer-" + ts, List.of(
				Map.of("name", "outer-step-" + ts, "text", "first"),
				Map.of("stepId", inner.id(), "isScenario", true)));

		ScenarioDto copy = (ScenarioDto) execute("CopyScenario",
				Map.of("projectName", projectName, "scenarioId", outer.id()));

		assertEquals("outer-" + ts + " 1", copy.name());
		assertEquals(2, copy.steps().size());
		StepDto copiedStep = copy.steps().get(0);
		StepDto nested = copy.steps().get(1);
		assertEquals("outer-step-" + ts + " 1", copiedStep.name(), "a plain step is copied");
		assertTrue(nested.isScenario(), "the nested scenario stays a scenario");
		assertEquals(inner.id(), nested.id(), "and is the same scenario, linked");
		assertEquals(1, rows("SELECT COUNT(*) FROM scenario_steps WHERE scenario_id = ?", inner.id()),
				"the nested scenario keeps its own step");
	}

	@Test
	void copyingSkipsANameAPlainStepAlreadyHas() throws Exception {
		long ts = System.nanoTime();
		ScenarioDto original = editScenario("checkout-" + ts, List.of());
		// A plain step named like the first copy would be.
		editScenario("holder-" + ts, List.of(Map.of("name", "checkout-" + ts + " 1", "text", "x")));

		ScenarioDto copy = (ScenarioDto) execute("CopyScenario",
				Map.of("projectName", projectName, "scenarioId", original.id()));

		assertEquals("checkout-" + ts + " 2", copy.name());
	}

	@Test
	void deletingAUseCasesPrimaryActorIsInvalidInput() throws Exception {
		long ts = System.nanoTime();
		execute("EditUseCase", Map.of("projectName", projectName, "name", "actor-uc-" + ts,
				"primaryActorName", "primary-" + ts));
		Long actorId = getProjectRepository().findActorByProjectOrDomainAndName(
				getProjectRepository().findProjectByName(projectName), "primary-" + ts).getId();

		GatewayException ex = assertThrows(GatewayException.class, () -> execute("DeleteActor",
				Map.of("projectName", projectName, "actorId", actorId)));

		assertEquals(GatewayException.Kind.INVALID_INPUT, ex.getKind(), ex.getMessage());
		assertTrue(ex.getMessage().contains("primary actor"), ex.getMessage());
	}

	// ---- helpers -------------------------------------------------------------------------------

	private ScenarioDto editScenario(String name, List<Map<String, Object>> steps) throws Exception {
		return (ScenarioDto) execute("EditScenario", Map.of("projectName", projectName,
				"name", name, "scenarioTypeName", "Primary", "steps", steps));
	}

	private Object execute(String commandType, Map<String, Object> input) throws Exception {
		return gateway.execute(new GatewayRequest(commandType, input)).result();
	}

	private int rows(String sql, Object... args) {
		Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
		return count == null ? 0 : count;
	}
}
