/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.project.impl.repository.jpa;

import java.util.Collections;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.project.Idea;
import com.rreganjr.requel.project.IdeaStore;
import com.rreganjr.requel.project.impl.IdeaImpl;

/**
 * The JPA {@link IdeaStore}.
 */
@Repository("ideaStore")
@Scope("singleton")
@Transactional(propagation = Propagation.REQUIRED)
public class JpaIdeaStore implements IdeaStore {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public Idea record(Long projectId, String text, String sourceFileName, User createdBy) {
		IdeaImpl idea = new IdeaImpl(projectId, text, sourceFileName, createdBy);
		entityManager.persist(idea);
		return idea;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Idea> list(Long projectId) {
		if (projectId == null) {
			return Collections.emptyList();
		}
		return List.copyOf(entityManager.createQuery(
				"select i from IdeaImpl i where i.projectId = :projectId order by i.id desc",
				IdeaImpl.class).setParameter("projectId", projectId).getResultList());
	}

	@Override
	public int deleteForProject(Long projectId) {
		if (projectId == null) {
			return 0;
		}
		return entityManager.createQuery("delete from IdeaImpl i where i.projectId = :projectId")
				.setParameter("projectId", projectId).executeUpdate();
	}
}
