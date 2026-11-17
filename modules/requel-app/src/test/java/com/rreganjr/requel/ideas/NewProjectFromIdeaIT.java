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
package com.rreganjr.requel.ideas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.node.NullNode;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.assistant.ai.AiAnalysisClient;
import com.rreganjr.requel.assistant.ai.AiAnalysisResponse;
import com.rreganjr.requel.assistant.ai.AiFindingDraft;
import com.rreganjr.requel.assistant.ai.AiUsage;
import com.rreganjr.requel.assistant.ai.IdeaGenerationService;
import com.rreganjr.requel.assistant.ai.IdeaGenerationService.IdeaView;
import com.rreganjr.requel.assistant.ai.ReviewResultMapper;
import com.rreganjr.requel.ideas.IdeaProjectService.Accepted;
import com.rreganjr.requel.ideas.IdeaProjectService.Started;
import com.rreganjr.requel.project.Project;

/**
 * The idea-to-new-project flow end to end on H2 with a stub AI provider: the project is created,
 * the idea saved and drafts generated; the chosen drafts become the project's requirements,
 * linked as the drafts refer to each other.
 */
@TestPropertySource(properties = "requel.ai.enabled=true")
@Import(NewProjectFromIdeaIT.StubAi.class)
public class NewProjectFromIdeaIT extends AbstractIntegrationTestCase {

	@TestConfiguration
	static class StubAi {
		@Bean
		@Primary
		AiAnalysisClient stubAiClient() {
			return request -> new AiAnalysisResponse("Gym members book classes.",
					NullNode.getInstance(),
					List.of(finding("ACTOR", "Gym member", "A person who attends classes."),
							finding("GOAL", "Members book classes remotely",
									"A member can reserve a class without calling the gym."),
							finding("GLOSSARY_TERM", "Class", "A scheduled group session."),
							finding("STORY", "Booking from a phone",
									"A member on the bus books tomorrow's class.",
									"ACTOR:Gym member", "GOAL:Members book classes remotely"),
							finding("USE_CASE", "Receive class reminder",
									"A booked member is reminded before the class.",
									"ACTOR:Gym member", "GOAL:Members book classes remotely",
									"STORY:Booking from a phone")),
					List.of(), AiUsage.noop("stub", Duration.ZERO), Map.of());
		}

		private static AiFindingDraft finding(String type, String name, String text,
				String... participants) {
			return new AiFindingDraft(type, null, 0.8, List.of(), text, null, List.of(),
					Map.of(ReviewResultMapper.SUGGESTED_ENTITY_NAME, name,
							ReviewResultMapper.PARTICIPANTS, List.of(participants)));
		}
	}

	@Autowired
	private IdeaProjectService service;

	@Autowired
	private IdeaGenerationService ideas;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@AfterEach
	public void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	private IdeaView startAndAwaitDrafts(String projectName) throws Exception {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("project", null, List.of()));
		Started started = service.startNewProject(projectName,
				"Let gym members book a class from their phone and get a reminder.", null, false);
		assertEquals(projectName, started.projectName());

		// The test profile's task executor is a no-op, so the queued generation runs here.
		assertEquals("PENDING", ideas.get(started.ideaId()).get().status());
		ideas.run(started.ideaId());
		Optional<IdeaView> view = ideas.get(started.ideaId());
		IdeaView result = view.get();
		assertEquals("DRAFTED", result.status(), "error: " + result.errorSummary());
		return result;
	}

	@Test
	public void chosenDraftsBecomeTheNewProjectsRequirements() throws Exception {
		String name = "Gym app " + System.nanoTime();
		IdeaView idea = startAndAwaitDrafts(name);
		assertEquals(5, idea.drafts().size());

		Accepted accepted = service.accept(name, idea, Set.of(0, 1, 2, 3, 4));

		assertEquals(Map.of("actor", 1, "goal", 1, "glossary term", 1, "story", 1, "use case", 1),
				accepted.created());
		assertTrue(accepted.notes().isEmpty(), accepted.notes()::toString);
		new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
			Project project = getProjectRepository().findProjectByName(name);
			assertEquals(1, project.getActors().size());
			assertEquals(1, project.getGoals().size());
			assertEquals(1, project.getStories().size());
			assertEquals(1, project.getUseCases().size());
			var story = project.getStories().iterator().next();
			assertEquals("Gym member", story.getPrimaryActor().getName());
			assertEquals(1, story.getGoals().size());
			var useCase = project.getUseCases().iterator().next();
			assertEquals(1, useCase.getGoals().size());
			assertEquals(1, useCase.getStories().size());
		});
	}

	@Test
	public void aLinkToADraftThatWasNotChosenIsLeftOutWithANote() throws Exception {
		String name = "Gym app partial " + System.nanoTime();
		IdeaView idea = startAndAwaitDrafts(name);

		// only the story (position 3): its actor and goal were not chosen
		Accepted accepted = service.accept(name, idea, Set.of(3));

		assertEquals(Map.of("story", 1), accepted.created());
		assertEquals(2, accepted.notes().size(), accepted.notes()::toString);
	}

	@Test
	public void nothingChosenAndUnknownPositionsAreRefused() throws Exception {
		String name = "Gym app refuse " + System.nanoTime();
		IdeaView idea = startAndAwaitDrafts(name);

		assertThrows(IllegalArgumentException.class, () -> service.accept(name, idea, Set.of()));
		assertThrows(IllegalArgumentException.class, () -> service.accept(name, idea, Set.of(9)));
	}
}
