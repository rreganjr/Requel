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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.impl.GoalImpl;
import com.rreganjr.requel.project.impl.ProjectImpl;
import com.rreganjr.requel.tagging.command.AssignTagCommand;
import com.rreganjr.requel.tagging.command.EditTagCommand;
import com.rreganjr.requel.tagging.command.TagCommandFactory;
import com.rreganjr.requel.tagging.impl.TagCategoryImpl;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.impl.OrganizationImpl;
import com.rreganjr.requel.user.impl.UserImpl;
import com.rreganjr.validator.EntityValidationException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

/**
 * Integration tests for Phase 6 typed-category rule enforcement: exclusivity (replace),
 * allowed entity types, and controlled value lists.
 */
@SpringBootTest
@ActiveProfiles("test")
public class TagCategoryIT {

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
		Project project = new ProjectImpl("TagCat-" + UUID.randomUUID(), owner,
				new OrganizationImpl("TagCatOrg-" + UUID.randomUUID()));
		entityManager.persist(project);
		entityManager.flush();
		return project;
	}

	private GoalImpl newGoal(Project project, User owner) {
		GoalImpl goal = new GoalImpl(project, owner, "Goal-" + UUID.randomUUID(), "goal text");
		entityManager.persist(goal);
		entityManager.flush();
		return goal;
	}

	private TagCategoryImpl category(User owner, Project project, String name, boolean exclusive) {
		TagCategoryImpl category = new TagCategoryImpl(name, ((ProjectImpl) project).getId(), exclusive, owner);
		return category;
	}

	private void createAndAssign(User user, Project project, Taggable target, String cat, String value)
			throws Exception {
		EditTagCommand edit = tagCommandFactory.newEditTagCommand();
		edit.setEditedBy(user);
		edit.setProjectScope(project);
		edit.setCategory(cat);
		edit.setValue(value);
		edit.execute();

		AssignTagCommand assign = tagCommandFactory.newAssignTagCommand();
		assign.setEditedBy(user);
		assign.setProjectScope(project);
		assign.setTag(edit.getTag());
		assign.setTaggable(target);
		assign.execute();
	}

	@Test
	@Transactional
	public void exclusiveCategoryReplacesPreviousValue() throws Exception {
		User admin = admin();
		Project project = newProject(admin);
		GoalImpl goal = newGoal(project, admin);

		tagRepository.persist(category(admin, project, "type", true));
		entityManager.flush();

		createAndAssign(admin, project, goal, "type", "business-rule");
		entityManager.flush();
		createAndAssign(admin, project, goal, "type", "performance");
		entityManager.flush();

		Set<String> typeValues = tagRepository.findTagsOnEntity("Goal", goal.getId()).stream()
				.filter(t -> "type".equals(t.getCategory()))
				.map(Tag::getValue)
				.collect(Collectors.toSet());
		assertEquals(Set.of("performance"), typeValues,
				"exclusive category should keep only the latest value");
	}

	@Test
	@Transactional
	public void categoryRejectsDisallowedEntityType() throws Exception {
		User admin = admin();
		Project project = newProject(admin);
		GoalImpl goal = newGoal(project, admin);

		TagCategoryImpl projectkind = category(admin, project, "projectkind", false);
		projectkind.getAllowedEntityTypes().add("Project");
		tagRepository.persist(projectkind);
		entityManager.flush();

		EditTagCommand edit = tagCommandFactory.newEditTagCommand();
		edit.setEditedBy(admin);
		edit.setProjectScope(project);
		edit.setCategory("projectkind");
		edit.setValue("product");
		edit.execute();

		AssignTagCommand assign = tagCommandFactory.newAssignTagCommand();
		assign.setEditedBy(admin);
		assign.setProjectScope(project);
		assign.setTag(edit.getTag());
		assign.setTaggable(goal);
		assertThrows(EntityValidationException.class, assign::execute,
				"projectkind may not attach to a Goal");

		// The same tag attaches fine to the Project, which is allowed.
		AssignTagCommand onProject = tagCommandFactory.newAssignTagCommand();
		onProject.setEditedBy(admin);
		onProject.setProjectScope(project);
		onProject.setTag(edit.getTag());
		onProject.setTaggable((Taggable) project);
		onProject.execute();
		entityManager.flush();
		assertTrue(tagRepository.findTagsOnEntity("Project", ((ProjectImpl) project).getId()).stream()
						.anyMatch(t -> "projectkind".equals(t.getCategory())),
				"projectkind should attach to the Project");
	}

	@Test
	@Transactional
	public void controlledValueListRejectsOffListValues() throws Exception {
		User admin = admin();
		Project project = newProject(admin);

		TagCategoryImpl kind = category(admin, project, "type", false);
		kind.getValues().add("business-rule");
		kind.getValues().add("performance");
		tagRepository.persist(kind);
		entityManager.flush();

		EditTagCommand bad = tagCommandFactory.newEditTagCommand();
		bad.setEditedBy(admin);
		bad.setProjectScope(project);
		bad.setCategory("type");
		bad.setValue("not-allowed");
		assertThrows(EntityValidationException.class, bad::execute,
				"value not in the category's controlled list must be rejected");

		EditTagCommand good = tagCommandFactory.newEditTagCommand();
		good.setEditedBy(admin);
		good.setProjectScope(project);
		good.setCategory("type");
		good.setValue("performance");
		good.execute();
		assertEquals("performance", good.getTag().getValue(), "on-list value is accepted");
	}
}
