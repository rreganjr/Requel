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
package com.rreganjr;

import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.user.impl.UserImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Transactional test helper that grants a role to a user within a single
 * JPA session. This avoids the detached-entity cascade issues that arise
 * when using EditUserCommand with CGLIB-proxied role entities.
 */
@Component
public class TestRoleGrantHelper {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Grants {@link ProjectUserRole} to the user identified by {@code username}
     * if they do not already have it. Runs in a single transaction so that
     * the user entity and new role are both managed throughout.
     */
    @Transactional
    public void grantProjectRoleIfMissing(String username) {
        List<UserImpl> results = entityManager
                .createQuery("SELECT u FROM UserImpl u WHERE u.username = :username", UserImpl.class)
                .setParameter("username", username)
                .getResultList();
        if (results.isEmpty()) return;
        UserImpl user = results.get(0);
        boolean hasRole = user.getUserRoles().stream()
                .anyMatch(r -> r instanceof ProjectUserRole);
        if (!hasRole) {
            user.grantRole(ProjectUserRole.class);
        }
    }
}
