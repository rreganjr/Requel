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
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.project.ProjectAssistantSettingsStore;
import com.rreganjr.requel.project.impl.ProjectAssistantSettingImpl;

/**
 * Issue #268: the JPA {@link ProjectAssistantSettingsStore}.
 */
@Repository("projectAssistantSettingsStore")
@Scope("singleton")
@Transactional(propagation = Propagation.REQUIRED)
public class JpaProjectAssistantSettingsStore implements ProjectAssistantSettingsStore {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Set<String> disabledAssistants(Long projectId) {
		if (projectId == null) {
			return Collections.emptySet();
		}
		return new HashSet<>(entityManager.createQuery(
				"select s.key.assistantId from ProjectAssistantSettingImpl s"
						+ " where s.key.projectId = :projectId and s.enabled = false",
				String.class).setParameter("projectId", projectId).getResultList());
	}

	@Override
	public void setEnabled(Long projectId, String assistantId, boolean enabled, User by) {
		ProjectAssistantSettingImpl.Key key = new ProjectAssistantSettingImpl.Key(projectId,
				assistantId);
		ProjectAssistantSettingImpl setting = entityManager.find(ProjectAssistantSettingImpl.class,
				key);
		if (setting == null) {
			setting = new ProjectAssistantSettingImpl(projectId, assistantId);
			setting.update(enabled, by == null ? null : by.getId());
			entityManager.persist(setting);
		} else {
			setting.update(enabled, by == null ? null : by.getId());
		}
	}

	@Override
	public int deleteForProject(Long projectId) {
		if (projectId == null) {
			return 0;
		}
		return entityManager.createQuery(
				"delete from ProjectAssistantSettingImpl s where s.key.projectId = :projectId")
				.setParameter("projectId", projectId).executeUpdate();
	}
}
