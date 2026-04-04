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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.annotation.impl.NoteImpl;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.impl.ActorImpl;
import com.rreganjr.requel.project.impl.ProjectImpl;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.impl.OrganizationImpl;

/**
 * Verifies that the Hibernate @Any/@ManyToAny mapping supplied by
 * ProjectAnnotatableMetadataContributor can resolve the registered project
 * annotatable types (e.g., Actor) when loading an annotation.
 */
@RunWith(SpringRunner.class)
public class AnnotationAnyMappingTest extends AbstractIntegrationTestCase {

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	@Transactional
	public void loadsAnnotatableViaAnyMapping() throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
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
		Assert.assertEquals("Annotation should retain grouping project", project.getName(),
				((Project) reloaded.getGroupingObject()).getName());

		Annotatable target = reloaded.getAnnotatables().iterator().next();
		Assert.assertTrue("Annotatable must resolve to ActorImpl", target instanceof ActorImpl);
		Assert.assertEquals(actor.getName(), ((ActorImpl) target).getName());
	}
}
