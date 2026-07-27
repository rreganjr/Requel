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
import com.rreganjr.requel.tagging.impl.TagImpl;
import com.rreganjr.requel.tagging.spi.TaggableTypeRegistry;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.impl.OrganizationImpl;
import com.rreganjr.requel.user.impl.UserImpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

/**
 * Verifies that the Hibernate {@code @ManyToAny} tag-assignment mapping supplied by
 * {@code TaggableMetadataContributor} + {@code TaggableRegistryConfiguration} can
 * persist a tag against multiple project entity types and resolve each back to its
 * concrete impl on reload — the tagging counterpart of {@code AnnotationAnyMappingTest}.
 */
@SpringBootTest
@ActiveProfiles("test")
public class TagAssignmentIT {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TaggableTypeRegistry taggableTypeRegistry;

	@Test
	@Transactional
	public void assignsTagToGoalAndProjectAndReloadsViaAnyMapping() throws Exception {
		User admin = userRepository.findUserByUsername("admin");
		// Reattach using the concrete entity class to avoid CGLIB proxy type issues.
		Long adminId = ((UserImpl) Hibernate.unproxy(admin)).getId();
		User adminRef = entityManager.find(UserImpl.class, adminId);

		Project project = new ProjectImpl("Tag-" + UUID.randomUUID(), adminRef,
				new OrganizationImpl("TagOrg-" + UUID.randomUUID()));
		GoalImpl goal = new GoalImpl(project, adminRef, "Goal-" + UUID.randomUUID(), "goal text");

		entityManager.persist(project);
		entityManager.persist(goal);
		entityManager.flush();

		TagImpl tag = new TagImpl("type", "business-rule", ((ProjectImpl) project).getId(), adminRef);
		tag.getTaggables().add(goal);
		tag.getTaggables().add((ProjectImpl) project);

		entityManager.persist(tag);
		entityManager.flush();
		entityManager.clear();

		TagImpl reloaded = entityManager.find(TagImpl.class, tag.getId());
		assertEquals("type", reloaded.getCategory(), "tag category should round-trip");
		assertEquals("business-rule", reloaded.getValue(), "tag value should round-trip");
		assertEquals(2, reloaded.getTaggables().size(),
				"tag should reload both assigned taggable entities");
		assertTrue(reloaded.getTaggables().stream().anyMatch(GoalImpl.class::isInstance),
				"goal taggable should reload through @ManyToAny as GoalImpl");
		assertTrue(reloaded.getTaggables().stream().anyMatch(ProjectImpl.class::isInstance),
				"project taggable should reload through @ManyToAny as ProjectImpl");
	}

	@Test
	public void registryResolvesProjectTaggableTypesBothDirections() {
		assertEquals(GoalImpl.class, taggableTypeRegistry.resolveEntityType("Goal").orElse(null),
				"'Goal' discriminator should resolve to GoalImpl");
		assertEquals(ProjectImpl.class, taggableTypeRegistry.resolveEntityType("Project").orElse(null),
				"'Project' discriminator should resolve to ProjectImpl");
		assertEquals("Goal", taggableTypeRegistry.resolveDiscriminator(GoalImpl.class).orElse(null),
				"GoalImpl should resolve to the 'Goal' discriminator");
		assertEquals("Project", taggableTypeRegistry.resolveDiscriminator(ProjectImpl.class).orElse(null),
				"ProjectImpl should resolve to the 'Project' discriminator");
	}
}
