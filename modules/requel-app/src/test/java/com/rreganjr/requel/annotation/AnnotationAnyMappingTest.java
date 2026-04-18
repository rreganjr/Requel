/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.annotation;

import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import org.hibernate.Hibernate;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.rreganjr.requel.annotation.ArgumentPositionSupportLevel;
import com.rreganjr.requel.annotation.impl.ArgumentImpl;
import com.rreganjr.requel.annotation.impl.IssueImpl;
import com.rreganjr.requel.annotation.impl.NoteImpl;
import com.rreganjr.requel.annotation.impl.PositionImpl;
import com.rreganjr.requel.project.StoryType;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.impl.ActorImpl;
import com.rreganjr.requel.project.impl.GoalImpl;
import com.rreganjr.requel.project.impl.ProjectImpl;
import com.rreganjr.requel.project.impl.StoryImpl;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.impl.OrganizationImpl;

/**
 * Verifies that the Hibernate @Any/@ManyToAny mapping supplied by
 * ProjectAnnotatableMetadataContributor can resolve the registered project
 * annotatable types (e.g., Actor) when loading an annotation.
 */
@SpringBootTest
@ActiveProfiles("test")
public class AnnotationAnyMappingTest {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private UserRepository userRepository;

	@Test
	@Transactional
	public void loadsAnnotatableViaAnyMapping() throws Exception {
		User admin = userRepository.findUserByUsername("admin");
		// Reattach using the concrete entity class to avoid CGLIB proxy type issues.
		Long adminId = ((com.rreganjr.requel.user.impl.UserImpl) Hibernate.unproxy(admin)).getId();
		User adminRef = entityManager.find(com.rreganjr.requel.user.impl.UserImpl.class, adminId);

		Project project = new ProjectImpl("Anno-" + UUID.randomUUID(), adminRef,
				new OrganizationImpl("AnnoOrg"));
		ActorImpl actor = new ActorImpl(project, adminRef, "Actor-" + UUID.randomUUID(),
				"annotation mapping probe");
		project.getActors().add(actor);

		NoteImpl note = new NoteImpl(project, "hello", adminRef);
		note.getAnnotatables().add(actor);

		entityManager.persist(project);
		entityManager.persist(actor);
		entityManager.persist(note);
		entityManager.flush();
		entityManager.clear();

		NoteImpl reloaded = entityManager.find(NoteImpl.class, note.getId());
		assertEquals(project.getName(),
				((Project) reloaded.getGroupingObject()).getName(),
				"Annotation should retain grouping project");

		Annotatable target = reloaded.getAnnotatables().iterator().next();
		assertTrue(target instanceof ActorImpl, "Annotatable must resolve to ActorImpl");
		assertEquals(actor.getName(), ((ActorImpl) target).getName());
	}

	@Test
	@Transactional
	public void loadsIssuePositionArgumentChainAcrossEntityTypes() throws Exception {
		User admin = userRepository.findUserByUsername("admin");
		Long adminId = ((com.rreganjr.requel.user.impl.UserImpl) Hibernate.unproxy(admin)).getId();
		User adminRef = entityManager.find(com.rreganjr.requel.user.impl.UserImpl.class, adminId);

		Project project = new ProjectImpl("AnnoChain-" + UUID.randomUUID(), adminRef,
				new OrganizationImpl("AnnoChainOrg"));
		GoalImpl goal = new GoalImpl(project, adminRef, "Goal-" + UUID.randomUUID(), "goal text");
		StoryImpl story = new StoryImpl(project, adminRef, "Story-" + UUID.randomUUID(),
				"story text", StoryType.Success);

		IssueImpl issue = new IssueImpl(project, "Resolve this shared issue", true, adminRef);
		issue.getAnnotatables().add(goal);
		issue.getAnnotatables().add(story);

		PositionImpl position = new PositionImpl("Proposed resolution", adminRef);
		position.getIssues().add(issue);
		issue.getPositions().add(position);

		ArgumentImpl argument = new ArgumentImpl(position, "This is the strongest option",
				ArgumentPositionSupportLevel.For, adminRef);
		position.getArguments().add(argument);

		entityManager.persist(project);
		entityManager.persist(goal);
		entityManager.persist(story);
		entityManager.persist(issue);
		entityManager.flush();
		entityManager.clear();

		IssueImpl reloaded = entityManager.find(IssueImpl.class, issue.getId());
		assertNotNull(reloaded, "issue should reload");
		assertEquals(2, reloaded.getAnnotatables().size(),
				"shared issue should reload both annotatable entities");
		assertTrue(reloaded.getAnnotatables().stream().anyMatch(GoalImpl.class::isInstance),
				"goal annotatable should reload through @ManyToAny");
		assertTrue(reloaded.getAnnotatables().stream().anyMatch(StoryImpl.class::isInstance),
				"story annotatable should reload through @ManyToAny");
		assertEquals(1, reloaded.getPositions().size(), "issue should reload its positions");

		PositionImpl reloadedPosition = (PositionImpl) reloaded.getPositions().iterator().next();
		assertEquals("Proposed resolution", reloadedPosition.getText());
		assertEquals(1, reloadedPosition.getArguments().size(),
				"position should reload its argument children");

		ArgumentImpl reloadedArgument = (ArgumentImpl) reloadedPosition.getArguments().iterator().next();
		assertEquals("This is the strongest option", reloadedArgument.getText());
		assertEquals(ArgumentPositionSupportLevel.For, reloadedArgument.getSupportLevel());
	}
}
