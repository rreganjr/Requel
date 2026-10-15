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
package com.rreganjr.requel.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.command.ImportProjectCommand;

/**
 * #355: the AI review evaluation fixture ({@code scripts/ai-eval/}) imports, and every entity its
 * expectations file names exists in it with that type - so renaming a fixture entity without
 * updating the expectations fails the build instead of quietly scoring nothing. Needs no AI
 * provider.
 */
public class FixtureExpectationsIT extends AbstractIntegrationTestCase {

	private static final List<String> TYPES = List.of("Goal", "Story", "UseCase", "Scenario",
			"Step", "Actor", "GlossaryTerm");

	private TransactionTemplate transactionTemplate;

	@Autowired
	protected void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@Test
	public void everyExpectedEntityIsInTheImportedFixture() throws Exception {
		Path dir = evalDir();
		JsonNode expectations = new ObjectMapper().readTree(dir.resolve("expectations.json").toFile());
		Project project = importFixture(dir.resolve(expectations.path("fixture").asText()));

		Map<String, Set<String>> namesByType = namesByType(project);
		List<String> missing = new ArrayList<String>();
		Map<String, int[]> flawedAndSilent = new HashMap<String, int[]>();
		for (JsonNode entity : expectations.path("entities")) {
			String type = entity.path("type").asText();
			String name = entity.path("name").asText();
			if (!namesByType.getOrDefault(type, Set.of()).contains(name)) {
				missing.add(type + " \"" + name + "\"");
			}
			int[] counts = flawedAndSilent.computeIfAbsent(type, t -> new int[2]);
			if (entity.path("silent").asBoolean(false)) {
				counts[1]++;
			} else {
				assertTrue(entity.path("expect").size() > 0,
						type + " \"" + name + "\" is neither silent nor expects a finding");
				counts[0]++;
			}
		}
		assertTrue(missing.isEmpty(), "expected entities not in the fixture: " + missing);

		for (String type : TYPES) {
			int[] counts = flawedAndSilent.getOrDefault(type, new int[2]);
			assertTrue(counts[0] > 0, type + " has no flawed case");
			assertTrue(counts[1] > 0, type + " has no silent case");
		}
		long traps = 0;
		for (JsonNode entity : expectations.path("entities")) {
			if (entity.path("trap").asBoolean(false)) {
				traps++;
				assertTrue(entity.path("confirmPatterns").size() > 0, "the trap has no confirmPatterns");
			}
		}
		assertEquals(1, traps, "exactly one shared-wrong-premise trap");
	}

	/**
	 * #263: the import kept a scenario's step references in a set, so every imported scenario's
	 * steps came out in hash order. The fixture's correct and deliberately wrong orders both have
	 * to survive the import, or the review scores a flaw the fixture does not have.
	 */
	@Test
	public void importKeepsEachScenariosStepOrder() throws Exception {
		Path dir = evalDir();
		Project project = importFixture(dir.resolve("fixture-project.xml"));
		assertEquals(List.of("The member opens the loan on the account page", "The member chooses Renew",
				"The system adds 7 days to the loan's due date and shows the new date"),
				stepNames(project, "Renew from the account page"));
		assertEquals(List.of("The supplier ships the tool", "The treasurer approves the request",
				"A member submits a purchase request"), stepNames(project, "Approve a purchase request"));
	}

	private List<String> stepNames(Project project, String scenarioName) {
		return transactionTemplate.execute(status -> {
			Project loaded = getProjectRepository().get(project);
			for (ProjectOrDomainEntity entity : loaded.getProjectEntities()) {
				if (entity instanceof Scenario scenario && scenarioName.equals(scenario.getName())) {
					List<String> names = new ArrayList<String>();
					for (Step step : scenario.getSteps()) {
						names.add(step.getName());
					}
					return names;
				}
			}
			throw new AssertionError("no scenario named " + scenarioName);
		});
	}

	private Project importFixture(Path xml) throws Exception {
		ImportProjectCommand command = getProjectCommandFactory().newImportProjectCommand();
		command.setEditedBy(getUserRepository().findUserByUsername("project"));
		command.setName("AI Eval Fixture " + System.nanoTime());
		try (InputStream in = Files.newInputStream(xml)) {
			command.setInputStream(in);
			return getCommandHandler().execute(command).getProject();
		}
	}

	private Map<String, Set<String>> namesByType(Project project) {
		return transactionTemplate.execute(status -> {
			Project loaded = getProjectRepository().get(project);
			Map<String, Set<String>> names = new HashMap<String, Set<String>>();
			for (ProjectOrDomainEntity entity : loaded.getProjectEntities()) {
				if (entity.getName() == null) {
					continue;
				}
				names.computeIfAbsent(entity.getProjectOrDomainEntityInterface().getSimpleName(),
						t -> new TreeSet<String>()).add(entity.getName());
			}
			return names;
		});
	}

	/** {@code scripts/ai-eval}, found by walking up from the module directory. */
	private static Path evalDir() {
		Set<Path> tried = new HashSet<Path>();
		for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
			Path candidate = dir.resolve("scripts").resolve("ai-eval");
			tried.add(candidate);
			if (Files.isRegularFile(candidate.resolve("expectations.json"))) {
				return candidate;
			}
		}
		throw new AssertionError("scripts/ai-eval/expectations.json not found; tried " + tried);
	}
}
