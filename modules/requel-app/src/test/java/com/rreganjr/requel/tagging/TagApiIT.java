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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.impl.GoalImpl;
import com.rreganjr.requel.project.impl.ProjectImpl;
import com.rreganjr.requel.service.api.dto.TagCategoryDto;
import com.rreganjr.requel.service.api.dto.TagDto;
import com.rreganjr.requel.service.command.CommandController;
import com.rreganjr.requel.service.query.TagQueryController;
import com.rreganjr.requel.tagging.impl.TagCategoryImpl;
import com.rreganjr.requel.tagging.impl.TagImpl;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.impl.OrganizationImpl;
import com.rreganjr.requel.user.impl.UserImpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

/**
 * End-to-end coverage for the tag CQRS surface: dispatches tag/category commands through the generic
 * {@link CommandController} (exercising the TagCommandRegistrar input applicators, result extractors,
 * and input/result DTOs) and reads them back through {@link TagQueryController}.
 */
@SpringBootTest
@ActiveProfiles("test")
public class TagApiIT {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CommandController commandController;

	@Autowired
	private TagQueryController tagQueryController;

	@Autowired
	private TagRepository tagRepository;

	private User admin() {
		User admin = userRepository.findUserByUsername("admin");
		Long adminId = ((UserImpl) Hibernate.unproxy(admin)).getId();
		return entityManager.find(UserImpl.class, adminId);
	}

	private void dispatchOk(String commandType, Map<String, Object> input) {
		ResponseEntity<?> response = commandController.dispatchJson(commandType, null, input);
		assertTrue(response.getStatusCode().is2xxSuccessful(),
				commandType + " should dispatch successfully but was " + response.getStatusCode());
	}

	@Test
	@Transactional
	public void dispatchesTagAndCategoryCommandsAndReadsThemBack() {
		User admin = admin();
		String projectName = "TagApi-" + UUID.randomUUID();
		Project project = new ProjectImpl(projectName, admin, new OrganizationImpl("TagApiOrg-" + UUID.randomUUID()));
		entityManager.persist(project);
		GoalImpl goal = new GoalImpl(project, admin, "Goal-" + UUID.randomUUID(), "goal text");
		entityManager.persist(goal);
		entityManager.flush();
		Long projectId = ((ProjectImpl) project).getId();
		Long goalId = goal.getId();

		// EditTagCategory — typed category with a controlled value list.
		dispatchOk("EditTagCategory", Map.of(
				"projectName", projectName, "name", "type", "exclusive", true,
				"allowedEntityTypes", List.of("Goal"),
				"values", List.of("business-rule", "performance")));

		// EditTag — create a tag in that category.
		dispatchOk("EditTag", Map.of(
				"projectName", projectName, "category", "type", "value", "business-rule"));
		entityManager.flush();
		Tag tag = tagRepository.findTag(projectId, "type", "business-rule");
		assertNotNull(tag, "EditTag should have created the tag");

		// AssignTag — attach it to the goal.
		dispatchOk("AssignTag", Map.of("tagId", tag.getId(), "entityType", "Goal", "entityId", goalId));
		entityManager.flush();

		// Reads through the query controller.
		List<TagDto> onEntity = tagQueryController.getTagsOnEntity("Goal", goalId).getBody();
		assertNotNull(onEntity);
		assertTrue(onEntity.stream().anyMatch(t -> "business-rule".equals(t.value())),
				"goal should carry the assigned tag");

		List<TagDto> forProject = tagQueryController.getTagsForProject(projectName).getBody();
		assertNotNull(forProject);
		assertTrue(forProject.stream().anyMatch(t -> "type".equals(t.category())));

		List<String> categories = tagQueryController.getCategories(projectName).getBody();
		assertTrue(categories != null && categories.contains("type"));

		List<TagCategoryDto> typed = tagQueryController.getTypedCategories(projectName).getBody();
		assertTrue(typed != null && typed.stream().anyMatch(c -> "type".equals(c.name()) && c.exclusive()),
				"typed categories should include the exclusive 'type'");

		List<Map<String, Object>> entities = tagQueryController.getEntitiesWithTag(tag.getId()).getBody();
		assertNotNull(entities);
		assertTrue(entities.stream().anyMatch(e -> "Goal".equals(e.get("entityType"))),
				"entities-with-tag should list the goal");

		// UnassignTag — detach it.
		dispatchOk("UnassignTag", Map.of("tagId", tag.getId(), "entityType", "Goal", "entityId", goalId));
		entityManager.flush();
		assertTrue(tagQueryController.getTagsOnEntity("Goal", goalId).getBody().isEmpty(),
				"tag assignment should be removed");

		// DeleteTag + DeleteTagCategory.
		dispatchOk("DeleteTag", Map.of("tagId", tag.getId()));
		TagCategory category = tagRepository.findCategory(projectId, "type");
		assertNotNull(category);
		dispatchOk("DeleteTagCategory", Map.of("categoryId", category.getId()));
		entityManager.flush();
		entityManager.clear();
		assertEquals(null, tagRepository.findCategoryById(category.getId()),
				"category should be deleted");
	}

	/**
	 * #343: an EditTag with a tagId takes its scope from the tag, not from the caller's
	 * projectName. Naming any other project (or naming one at all for a global tag) is refused and
	 * leaves the tag alone; omitting projectName edits the tag in its own project; an unknown
	 * tagId is refused instead of creating a tag.
	 */
	@Test
	@Transactional
	public void editTagByIdKeepsTheTagsOwnScope() {
		User admin = admin();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		Project projectA = newProject(admin, "TagScopeA-" + suffix);
		Project projectB = newProject(admin, "TagScopeB-" + suffix);
		Long projectAId = ((ProjectImpl) projectA).getId();

		TagImpl tagA = new TagImpl("type", "a-" + suffix, projectAId, admin);
		TagImpl global = new TagImpl("type", "g-" + suffix, null, admin);
		entityManager.persist(tagA);
		entityManager.persist(global);
		entityManager.flush();

		assertEquals(400, dispatchStatus("EditTag", Map.of("tagId", tagA.getId(),
				"projectName", projectB.getName(), "category", "type", "value", "hijacked-" + suffix)),
				"project A's tag must not be editable as project B");
		assertEquals(400, dispatchStatus("EditTag", Map.of("tagId", global.getId(),
				"projectName", projectB.getName(), "category", "type", "value", "hijacked-" + suffix)),
				"a global tag must not be editable as project B");
		// #345: an unknown id is not found (404), no longer invalid input (400).
		assertEquals(404, dispatchStatus("EditTag", Map.of("tagId", Long.MAX_VALUE,
				"projectName", projectA.getName(), "category", "type", "value", "ghost-" + suffix)),
				"an unknown tagId is refused");
		entityManager.flush();
		entityManager.clear();
		assertEquals("a-" + suffix, tagRepository.findTagById(tagA.getId()).getValue());
		assertEquals("g-" + suffix, tagRepository.findTagById(global.getId()).getValue());
		assertNull(tagRepository.findTag(projectAId, "type", "ghost-" + suffix),
				"an unknown tagId must not create a tag");

		dispatchOk("EditTag", Map.of("tagId", tagA.getId(), "category", "type",
				"value", "renamed-" + suffix));
		dispatchOk("EditTag", Map.of("tagId", tagA.getId(), "projectName", projectA.getName(),
				"category", "type", "value", "renamed-again-" + suffix));
		entityManager.flush();
		entityManager.clear();
		Tag renamed = tagRepository.findTagById(tagA.getId());
		assertEquals("renamed-again-" + suffix, renamed.getValue());
		assertEquals(projectAId, renamed.getProjectId(), "the tag stays in project A");
	}

	/** #343: the same rule for EditTagCategory with a categoryId. */
	@Test
	@Transactional
	public void editTagCategoryByIdKeepsTheCategorysOwnScope() {
		User admin = admin();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		Project projectA = newProject(admin, "CatScopeA-" + suffix);
		Project projectB = newProject(admin, "CatScopeB-" + suffix);
		Long projectAId = ((ProjectImpl) projectA).getId();

		TagCategoryImpl catA = new TagCategoryImpl("a-" + suffix, projectAId, false, admin);
		TagCategoryImpl global = new TagCategoryImpl("g-" + suffix, null, false, admin);
		entityManager.persist(catA);
		entityManager.persist(global);
		entityManager.flush();

		assertEquals(400, dispatchStatus("EditTagCategory", categoryInput(catA.getId(),
				projectB.getName(), "hijacked-" + suffix)),
				"project A's category must not be editable as project B");
		assertEquals(400, dispatchStatus("EditTagCategory", categoryInput(global.getId(),
				projectB.getName(), "hijacked-" + suffix)),
				"a global category must not be editable as project B");
		// #345: an unknown id is not found (404), no longer invalid input (400).
		assertEquals(404, dispatchStatus("EditTagCategory", categoryInput(Long.MAX_VALUE,
				projectA.getName(), "ghost-" + suffix)),
				"an unknown categoryId is refused");
		entityManager.flush();
		entityManager.clear();
		assertEquals("a-" + suffix, tagRepository.findCategoryById(catA.getId()).getName());
		assertEquals("g-" + suffix, tagRepository.findCategoryById(global.getId()).getName());
		assertFalse(tagRepository.findCategoryById(catA.getId()).isExclusive());

		dispatchOk("EditTagCategory", categoryInput(catA.getId(), null, "renamed-" + suffix));
		entityManager.flush();
		entityManager.clear();
		TagCategory renamed = tagRepository.findCategoryById(catA.getId());
		assertEquals("renamed-" + suffix, renamed.getName());
		assertEquals(projectAId, renamed.getProjectId(), "the category stays in project A");
	}

	private Project newProject(User owner, String name) {
		Project project = new ProjectImpl(name, owner, new OrganizationImpl("TagScopeOrg-" + UUID.randomUUID()));
		entityManager.persist(project);
		entityManager.flush();
		return project;
	}

	private int dispatchStatus(String commandType, Map<String, Object> input) {
		return commandController.dispatchJson(commandType, null, input).getStatusCode().value();
	}

	/** An exclusive-category edit; a null projectName is left out of the input. */
	private static Map<String, Object> categoryInput(Long categoryId, String projectName, String name) {
		Map<String, Object> input = new HashMap<>();
		input.put("categoryId", categoryId);
		if (projectName != null) {
			input.put("projectName", projectName);
		}
		input.put("name", name);
		input.put("exclusive", true);
		return input;
	}
}
