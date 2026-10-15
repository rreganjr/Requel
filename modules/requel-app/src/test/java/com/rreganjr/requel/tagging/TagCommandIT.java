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
package com.rreganjr.requel.tagging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.impl.GoalImpl;
import com.rreganjr.requel.project.impl.ProjectImpl;
import com.rreganjr.requel.tagging.command.AssignTagCommand;
import com.rreganjr.requel.tagging.command.DeleteTagCommand;
import com.rreganjr.requel.tagging.command.EditTagCommand;
import com.rreganjr.requel.tagging.command.TagCommandFactory;
import com.rreganjr.requel.tagging.command.UnassignTagCommand;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.impl.OrganizationImpl;
import com.rreganjr.requel.user.impl.UserImpl;
import com.rreganjr.validator.EntityValidationException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

/**
 * Service-layer integration test for the Phase 2 tag commands. Drives the real command
 * beans (created via {@link TagCommandFactory}) and the {@link TagRepository} against the
 * H2 test schema, covering create + normalization, duplicate handling, assign/unassign
 * polymorphically, delete, and the query methods. HTTP status mapping is covered generically
 * by {@code CommandControllerTest}; commands are executed directly here so the focus stays on
 * tag behavior rather than the security chain.
 */
@SpringBootTest
@ActiveProfiles("test")
public class TagCommandIT {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TagCommandFactory tagCommandFactory;

	@Autowired
	private TagRepository tagRepository;

	private User admin() {
		User admin = userRepository.findUserByUsername("admin");
		Long adminId = ((UserImpl) Hibernate.unproxy(admin)).getId();
		return entityManager.find(UserImpl.class, adminId);
	}

	private Project newProject(User owner) {
		Project project = new ProjectImpl("TagCmd-" + UUID.randomUUID(), owner,
				new OrganizationImpl("TagCmdOrg-" + UUID.randomUUID()));
		entityManager.persist(project);
		entityManager.flush();
		return project;
	}

	@Test
	@Transactional
	public void createsNormalizesAndReusesTags() throws Exception {
		User admin = admin();
		Project project = newProject(admin);

		EditTagCommand create = tagCommandFactory.newEditTagCommand();
		create.setEditedBy(admin);
		create.setProjectScope(project);
		create.setCategory("Type");
		create.setValue("Business Rule");
		create.execute();

		Tag tag = create.getTag();
		assertNotNull(tag.getId(), "created tag should have an id");
		assertEquals("type", tag.getCategory(), "category normalized to a slug");
		assertEquals("business-rule", tag.getValue(), "value normalized to a slug");
		assertEquals(((ProjectImpl) project).getId(), tag.getProjectId(), "tag scoped to project");

		// Re-creating the same normalized identity in scope reuses the existing tag.
		EditTagCommand again = tagCommandFactory.newEditTagCommand();
		again.setEditedBy(admin);
		again.setProjectScope(project);
		again.setCategory("type");
		again.setValue("business-rule");
		again.execute();
		assertEquals(tag.getId(), again.getTag().getId(), "duplicate create reuses the existing tag");
	}

	@Test
	@Transactional
	public void blankValueIsRejected() throws Exception {
		User admin = admin();
		Project project = newProject(admin);

		EditTagCommand create = tagCommandFactory.newEditTagCommand();
		create.setEditedBy(admin);
		create.setProjectScope(project);
		create.setCategory("type");
		create.setValue("   ");
		assertThrows(EntityValidationException.class, create::execute,
				"blank value must be rejected as a validation error");
	}

	@Test
	@Transactional
	public void updateToDuplicateIdentityIsRejected() throws Exception {
		User admin = admin();
		Project project = newProject(admin);

		EditTagCommand a = tagCommandFactory.newEditTagCommand();
		a.setEditedBy(admin);
		a.setProjectScope(project);
		a.setCategory("type");
		a.setValue("business-rule");
		a.execute();

		EditTagCommand b = tagCommandFactory.newEditTagCommand();
		b.setEditedBy(admin);
		b.setProjectScope(project);
		b.setCategory("type");
		b.setValue("performance");
		b.execute();

		// Rename b onto a's identity -> conflict.
		EditTagCommand rename = tagCommandFactory.newEditTagCommand();
		rename.setEditedBy(admin);
		rename.setProjectScope(project);
		rename.setTag(b.getTag());
		rename.setCategory("type");
		rename.setValue("business-rule");
		assertThrows(EntityValidationException.class, rename::execute,
				"renaming onto an existing (scope,category,value) must be rejected");
	}

	@Test
	@Transactional
	public void assignsAndUnassignsTagPolymorphically() throws Exception {
		User admin = admin();
		Project project = newProject(admin);
		GoalImpl goal = new GoalImpl(project, admin, "Goal-" + UUID.randomUUID(), "goal text");
		entityManager.persist(goal);
		entityManager.flush();

		EditTagCommand create = tagCommandFactory.newEditTagCommand();
		create.setEditedBy(admin);
		create.setProjectScope(project);
		create.setCategory("type");
		create.setValue("performance");
		create.execute();
		Tag tag = create.getTag();

		AssignTagCommand assign = tagCommandFactory.newAssignTagCommand();
		assign.setEditedBy(admin);
		assign.setProjectScope(project);
		assign.setTag(tag);
		assign.setTaggable(goal);
		assign.execute();
		entityManager.flush();

		assertTrue(tagRepository.findTagsOnEntity("Goal", goal.getId()).stream()
						.anyMatch(t -> t.getId().equals(tag.getId())),
				"goal should carry the assigned tag");

		UnassignTagCommand unassign = tagCommandFactory.newUnassignTagCommand();
		unassign.setEditedBy(admin);
		unassign.setProjectScope(project);
		unassign.setTag(tag);
		unassign.setTaggable(goal);
		unassign.execute();
		entityManager.flush();

		assertTrue(tagRepository.findTagsOnEntity("Goal", goal.getId()).isEmpty(),
				"tag assignment should be removed");
	}

	@Test
	@Transactional
	public void deletesTagAndListsCategories() throws Exception {
		User admin = admin();
		Project project = newProject(admin);

		EditTagCommand create = tagCommandFactory.newEditTagCommand();
		create.setEditedBy(admin);
		create.setProjectScope(project);
		create.setCategory("type");
		create.setValue("technical-guideline");
		create.execute();
		Tag tag = create.getTag();
		Long tagId = tag.getId();

		assertTrue(tagRepository.findDistinctCategories(((ProjectImpl) project).getId()).contains("type"),
				"distinct categories should include 'type'");

		DeleteTagCommand delete = tagCommandFactory.newDeleteTagCommand();
		delete.setEditedBy(admin);
		delete.setProjectScope(project);
		delete.setTag(tag);
		delete.execute();
		entityManager.flush();
		entityManager.clear();

		assertNull(tagRepository.findTagById(tagId), "deleted tag should no longer be found");
	}
}
